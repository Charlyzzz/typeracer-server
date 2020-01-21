package typeracer

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem, Scheduler}
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Source}
import akka.util.Timeout
import com.google.protobuf.empty.Empty
import typeracer.flow.GroupedWithTimeout._
import typeracer.flow.ScoreboardFlow

import scala.concurrent.Future
import scala.concurrent.duration._

class TypeRacerImpl(implicit val system: ActorSystem) extends TypeRacer {

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val log = system.log

  val gameCoordinator = system.spawn(GameCoordinator(), "game")

  val ((sink, metrics), source) = MergeHub.source[PlayerSpeed]
    .viaMat(new ScoreboardFlow(gameCoordinator, 5.seconds))(Keep.both)
    .toMat(BroadcastHub.sink[Scoreboard])(Keep.both).run()

  metrics
    .groupedWithTimeout(20, 5.seconds)
    .map(_.last)
    .runForeach(flowMetric => log.info(s"$flowMetric"))

  override def sendPlayerMetrics(in: Source[PlayerSpeed, NotUsed]): Source[Scoreboard, NotUsed] = {
    in.runWith(sink)
    source
  }

  override def dashboard(in: Empty): Future[Scoreboard] = {
    implicit val timeout: Timeout = 1.seconds
    implicit val typedScheduler: Scheduler = system.scheduler
    gameCoordinator.ask[Scoreboard](GetScoreboard)
  }
}

case class FlowMetric(metricsReceived: Int, scoreboardEmitted: Int)

object FlowMetric {
  val initial: FlowMetric = FlowMetric(0, 0)
}

