package typeracer

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Source}

import scala.collection.mutable

class TypeRacerImpl(implicit val materializer: Materializer) extends TypeRacer {

  val flow: Flow[PlayerMetrics, Scoreboard, NotUsed] = Flow[PlayerMetrics].statefulMapConcat { () =>
    var times = 0
    val players: mutable.Map[String, PlayerMetrics] = mutable.Map()
    var currentTopTen: List[PlayerMetrics] = Nil
    playerMetric =>
      times += 1
      val previousMetric = players.put(playerMetric.username, playerMetric)
      val playerJoined = previousMetric.exists(_.connectionTime != playerMetric.connectionTime)
      val topTen = players.values.toList.sortBy(_.strokesPerMinute).reverse.take(10)
      if (topTen != currentTopTen || playerJoined) {
        currentTopTen = topTen
        println("sent")
        List(Scoreboard(topTen))
      }
      else
        List()
  }

  val (sink, source) = MergeHub.source[PlayerMetrics]
    .via(flow)
    .toMat(BroadcastHub.sink[Scoreboard])(Keep.both).run()

  override def sendPlayerMetrics(in: Source[PlayerMetrics, NotUsed]): Source[Scoreboard, NotUsed] = {
    in.runWith(sink)
    source
  }
}
