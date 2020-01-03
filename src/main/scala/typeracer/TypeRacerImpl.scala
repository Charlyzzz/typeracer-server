package typeracer

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Source}

import scala.collection.mutable

class TypeRacerImpl(implicit val materializer: Materializer) extends TypeRacer {


  val (sink, source) = MergeHub.source[PlayerMetrics]
    .via(TypeRacerImpl.flow)
    .toMat(BroadcastHub.sink[Scoreboard])(Keep.both).run()

  override def sendPlayerMetrics(in: Source[PlayerMetrics, NotUsed]): Source[Scoreboard, NotUsed] = {
    in.runWith(sink)
    source
  }
}

object TypeRacerImpl {
  val flow = Flow[PlayerMetrics].statefulMapConcat { () =>
    var times = 0
    var timesSent = 0
    val players: mutable.Map[String, PlayerMetrics] = mutable.Map()
    var currentTopTen: List[PlayerMetrics] = List()

    playerMetric =>
      times += 1
      val previousMetric = players.put(playerMetric.username, playerMetric)
      val playerJoined = previousMetric.exists(_.connectionTime != playerMetric.connectionTime)
      val topTen = players.values.toList.sortBy(_.strokesPerMinute).reverse.take(10)
      val shouldBroadcastTopTen = topTen != currentTopTen || playerJoined
      if (shouldBroadcastTopTen) {
        currentTopTen = topTen
        timesSent += 1
        List(Scoreboard(topTen))
      }
      else
        List()
  }

}
