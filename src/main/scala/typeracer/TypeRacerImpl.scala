package typeracer

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Source}
import typeracer.flow.GroupedWithTimeout._
import typeracer.flow.ScoreboardFlow

import scala.concurrent.duration._

class TypeRacerImpl(implicit val system: ActorSystem) extends TypeRacer {

  implicit val materializer: Materializer = ActorMaterializer()
  val log = system.log

  val ((sink, metrics), source) = MergeHub.source[PlayerSpeed]
    .viaMat(new ScoreboardFlow(5.seconds))(Keep.both)
    .toMat(BroadcastHub.sink[Scoreboard])(Keep.both).run()

  metrics
    .groupedWithTimeout(20, 5.seconds)
    .map(_.last)
    .runForeach(flowMetric => log.info(s"$flowMetric"))

  override def sendPlayerMetrics(in: Source[PlayerSpeed, NotUsed]): Source[Scoreboard, NotUsed] = {
    in.runWith(sink)
    source
  }
}

case class FlowMetric(metricsReceived: Int, scoreboardEmitted: Int)

object FlowMetric {
  val initial: FlowMetric = FlowMetric(0, 0)
}

