package typeracer.flow

import akka.actor.ActorSystem
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import typeracer.flow.GroupedWithTimeout._

import scala.concurrent.duration._

class GroupedWithTimeoutSpec extends TestKit(ActorSystem("test")) with Matchers with AnyFreeSpecLike with BeforeAndAfterAll with ScalaFutures {

  implicit class RepeatExtension(n: Int) {
    def times[A](block: Int => A): Unit = (1 to n).foreach(block)
  }

  implicit val materializer: Materializer = ActorMaterializer()

  "GroupedWithTimeout" - {

    "wont emit if timeout or size is not reached" in {
      val (pub, sub) = testStreamProbes(2, 500.millis)
      pub.sendNext(1)
      sub.request(1)
        .expectNoMessage(500.millis)
    }

    "emits when group size is reached" in {
      val (pub, sub) = testStreamProbes(1, 100.millis)
      pub.sendNext(1)
      sub.request(1)
        .expectNext(Seq(1))
    }

    "emits when timeout is reached" in {
      val (pub, sub) = testStreamProbes(2, 100.millis)
      pub.sendNext(1)
      sub.request(1)
        .expectNoMessage(100.millis)
        .expectNext(Seq(1))
    }
  }

  private def testStreamProbes(size: Int, interval: FiniteDuration): (TestPublisher.Probe[Int], TestSubscriber.Probe[Seq[Int]]) =
    TestSource.probe[Int]
      .groupedWithTimeout(size, interval)
      .toMat(TestSink.probe[Seq[Int]])(Keep.both)
      .run()
}
