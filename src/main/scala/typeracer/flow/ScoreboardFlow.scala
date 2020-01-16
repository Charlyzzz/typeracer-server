package typeracer.flow

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.Source
import akka.stream.stage._
import typeracer.{FlowMetric, Game, PlayerSpeed, Scoreboard}

import scala.concurrent.duration._

sealed trait PlayerState

case object AFK extends PlayerState

case object Playing extends PlayerState

object ScoreboardFlow {
  type Player = (PlayerState, PlayerSpeed)
}

case class Player(state: PlayerState, metric: PlayerSpeed)

class ScoreboardFlow(afkTimeout: FiniteDuration)(implicit materializer: Materializer) extends GraphStageWithMaterializedValue[FlowShape[PlayerSpeed, Scoreboard], Source[FlowMetric, NotUsed]] {

  val in = Inlet[PlayerSpeed]("Typeracer.in")
  val out = Outlet[Scoreboard]("Typeracer.out")

  override val shape = FlowShape.of(in, out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Source[FlowMetric, NotUsed]) = {
    val (queue, source) = Source.queue[FlowMetric](Integer.MAX_VALUE, OverflowStrategy.dropNew).preMaterialize()

    val logic = new TimerGraphStageLogic(shape) with InHandler with OutHandler {

      var metricsReceived = 0
      var scoreboardEmitted = 0
      val game = new Game()

      override def preStart(): Unit = queue.offer(FlowMetric.initial)

      def playerSpeedReceived(username: String): Unit = {
        metricsReceived += 1
        cancelTimer(username)
      }

      override def onPush(): Unit = {
        val playerSpeed = grab(in)
        playerSpeedReceived(playerSpeed.username)
        val previousScoreboard = game.scoreboard
        game.registrarVelocidadDeJugador(playerSpeed)
        val scoreboard = game.scoreboard
        if (previousScoreboard != scoreboard) {
          emitScoreboard(scoreboard)
          scheduleOnce(playerSpeed.username, afkTimeout)
        } else {
          pull(in)
        }
      }

      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)

      override protected def onTimer(timerKey: Any): Unit = timerKey match {
        case username: String => movePlayerToAfk(username)
      }

      private def movePlayerToAfk(username: String): Unit = {
        game.registrarJugadorAfk(username)
        emitScoreboard(game.scoreboard)
      }

      private def emitScoreboard(scoreboard: Scoreboard): Unit = {
        emit(out, scoreboard)
        scoreboardEmitted += 1
        queue.offer(FlowMetric(metricsReceived, scoreboardEmitted))
      }

      setHandlers(in, out, this)

    }
    (logic, source)
  }
}
