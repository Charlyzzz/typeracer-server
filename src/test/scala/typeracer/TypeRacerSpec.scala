package typeracer

import akka.actor.ActorSystem
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import com.google.protobuf.timestamp.Timestamp
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike


class GreeterServiceImplSpec extends TestKit(ActorSystem("test")) with Matchers with AnyWordSpecLike with BeforeAndAfterAll with ScalaFutures {
  "Flow" must {
    val typeRacerLogic = TypeRacerImpl.flow
    implicit val materializer: Materializer = ActorMaterializer()

    "does not emit if scoreboard remains the same" in {
      val erwinSpeed = PlayerMetrics("erwin", 200, Some(Timestamp(0, 0)))
      val (pub, sub) = TestSource.probe[PlayerMetrics]
        .via(typeRacerLogic)
        .toMat(TestSink.probe[Scoreboard])(Keep.both)
        .run()

      sub.request(2)
      pub.sendNext(erwinSpeed)
      pub.sendNext(erwinSpeed)
      pub.sendComplete()
      sub.expectNext(Scoreboard(List(erwinSpeed)))
      sub.expectComplete()
    }

    "when scoreboard changes it emits the new version" in {
      val erwin = PlayerMetrics("erwin", 200, Some(Timestamp(0, 0)))
      val erwinFaster = erwin.copy(strokesPerMinute = erwin.strokesPerMinute + 20)

      val (pub, sub) = TestSource.probe[PlayerMetrics]
        .via(typeRacerLogic)
        .toMat(TestSink.probe[Scoreboard])(Keep.both)
        .run()

      sub.request(2)
      pub.sendNext(erwin)
      pub.sendNext(erwinFaster)
      sub.expectNext(Scoreboard(List(erwin)), Scoreboard(List(erwinFaster)))
    }
  }
}

