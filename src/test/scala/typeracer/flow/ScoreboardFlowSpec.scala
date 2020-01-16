package typeracer.flow

import akka.actor.ActorSystem
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import typeracer.{PlayerSpeed, Scoreboard}

import scala.concurrent.duration._

class ScoreboardFlowSpec extends TestKit(ActorSystem("test")) with Matchers with AnyFreeSpecLike with BeforeAndAfterAll with ScalaFutures {

  implicit class RepeatExtension(n: Int) {
    def times[A](block: Int => A): Unit = (1 to n).foreach(block)
  }

  "Flow" - {
    implicit val materializer: Materializer = ActorMaterializer()

    val typeRacerLogic = new ScoreboardFlow(200.millis)

    "wont emit scoreboard if received same speed" in {
      val erwinSpeed = PlayerSpeed("erwin", 200)

      val (pub, sub) = TestSource.probe[PlayerSpeed]
        .via(typeRacerLogic)
        .toMat(TestSink.probe[Scoreboard])(Keep.both)
        .run()

      2.times { _ => pub.sendNext(erwinSpeed) }

      sub.request(2)
        .expectNext(Scoreboard(List(erwinSpeed)))
    }

    "emits the new version when player speed changes" in {
      val erwinAt200Kpm = PlayerSpeed("erwin", 200)
      val erwinAt220Kpm = erwinAt200Kpm.copy(strokesPerMinute = erwinAt200Kpm.strokesPerMinute + 20)

      val (pub, sub) = TestSource.probe[PlayerSpeed]
        .via(typeRacerLogic)
        .toMat(TestSink.probe[Scoreboard])(Keep.both)
        .run()

      pub.sendNext(erwinAt200Kpm)
      pub.sendNext(erwinAt220Kpm)
      pub.sendComplete()

      sub.request(2)
        .expectNext(Scoreboard(List(erwinAt200Kpm)), Scoreboard(List(erwinAt220Kpm)))
    }

    "players moves into AFK without sending any speed within expected time and emits the new scoreboard" in {
      val erwinAt200Kpm = PlayerSpeed("erwin", 200)

      val (pub, sub) = TestSource.probe[PlayerSpeed]
        .via(typeRacerLogic)
        .toMat(TestSink.probe[Scoreboard])(Keep.both)
        .run()

      pub.sendNext(erwinAt200Kpm)
      sub.request(2)
        .expectNext(Scoreboard(List(erwinAt200Kpm)))
        .expectNoMessage(200.millis)
        .expectNext(Scoreboard(List.empty, List(erwinAt200Kpm)))
    }
  }
}
