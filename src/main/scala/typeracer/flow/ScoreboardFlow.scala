package typeracer.flow

import akka.actor.typed.ActorRef
import akka.actor.{ActorSystem, Scheduler}
import akka.stream._
import akka.stream.scaladsl.Source
import akka.stream.stage._
import akka.util.Timeout
import akka.{NotUsed, actor => classic}
import typeracer._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

sealed trait PlayerState

case object AFK extends PlayerState

case object Playing extends PlayerState

object ScoreboardFlow {
  type Player = (PlayerState, PlayerSpeed)
}

case class Player(state: PlayerState, metric: PlayerSpeed)

class ScoreboardFlow(gameCoordinator: ActorRef[GameMessage], afkTimeout: FiniteDuration)(implicit actorSystem: ActorSystem) extends GraphStageWithMaterializedValue[FlowShape[PlayerSpeed, Scoreboard], Source[FlowMetric, NotUsed]] {

  implicit val materializer: Materializer = ActorMaterializer()

  val in = Inlet[PlayerSpeed]("Typeracer.in")
  val out = Outlet[Scoreboard]("Typeracer.out")

  override val shape = FlowShape.of(in, out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Source[FlowMetric, NotUsed]) = {
    val (queue, source) = Source.queue[FlowMetric](Integer.MAX_VALUE, OverflowStrategy.dropNew).preMaterialize()

    val logic = new TimerGraphStageLogic(shape) with InHandler with OutHandler {

      implicit val timeout: Timeout = 1.seconds
      implicit val executionContext: ExecutionContext = actorSystem.dispatcher
      implicit val scheduler: Scheduler = actorSystem.scheduler

      var metricsReceived = 0
      var scoreboardEmitted = 0
      val game = new Game()

      private lazy val self = getStageActor {
        case (replyTo, GetScoreboard) => replyTo ! game.scoreboard
        case _ =>
      }

      override def preStart(): Unit = {
        gameCoordinator ! LinkStageActor(self.ref)
        queue.offer(FlowMetric.initial)
      }

      def messageHandler(receive: (classic.ActorRef, Any)): Unit = receive match {
        case (replyTo, GetScoreboard) =>
          replyTo ! game.scoreboard
        case _ =>
      }

      def playerSpeedReceived(username: String): Unit = {
        metricsReceived += 1
        cancelTimer(username)
      }

      override def onPush(): Unit = {
        val playerSpeed = grab(in)
        playerSpeedReceived(playerSpeed.username)
        val previousScoreboard = game.scoreboard
        game.trackPlayerSpeed(playerSpeed)
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
        game.trackAfkPlayer(username)
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

