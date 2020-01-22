package typeracer.flow

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import akka.stream.{ActorMaterializer, Materializer}
import extensions.{Extensions, ScalaTestWithActorTestKit}
import org.scalatest.freespec.AnyFreeSpecLike
import typeracer.flow.GroupedWithTimeout._

import scala.concurrent.duration._

class GroupedWithTimeoutSpec extends ScalaTestWithActorTestKit with AnyFreeSpecLike with Extensions {

  "GroupedWithTimeout" - {

    "wont emit if timeout or size is not reached" in {
      val (pub, sub) = testStreamProbes(10, 3.seconds)
      pub.sendNext(1)
      sub.request(1)
        .expectNoMessage(100.millis)
    }

    "emits when group size is reached" in {
      val (pub, sub) = testStreamProbes(5, 100.millis)
      5.times { _ => pub.sendNext(1) }
      sub.request(1)
        .expectNext(Seq(1, 1, 1, 1, 1))
    }

    "emits when timeout is reached" in {
      val (pub, sub) = testStreamProbes(5, 100.millis)
      pub.sendNext(1)
      sub.request(1)
        .expectNoMessage(100.millis)
        .expectNext(Seq(1))
    }
  }

  private def testStreamProbes(size: Int, interval: FiniteDuration): (TestPublisher.Probe[Int], TestSubscriber.Probe[Seq[Int]]) = {
    implicit val system: ActorSystem = testKit.system.toClassic
    implicit val materializer: Materializer = ActorMaterializer()

    TestSource.probe[Int]
      .groupedWithTimeout(size, interval)
      .toMat(TestSink.probe[Seq[Int]])(Keep.both)
      .run()
  }
}
