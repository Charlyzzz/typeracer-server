package typeracer.flow

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import akka.stream.{ActorMaterializer, Materializer}
import extensions.{Extensions, ScalaTestWithActorTestKit}
import org.scalatest.freespec.AnyFreeSpecLike
import typeracer._

import scala.concurrent.Await
import scala.concurrent.duration._

class ScoreboardFlowSpec extends ScalaTestWithActorTestKit with AnyFreeSpecLike with Extensions {

  "Flow" - {

    "wont emit scoreboard if received same speed" in {
      val game = testKit.spawn(GameCoordinator())
      val (pub, sub) = testFlow(game, 200.millis)

      val erwinSpeed = PlayerSpeed("erwin", 200)

      2.times { _ => pub.sendNext(erwinSpeed) }

      sub.request(2)
        .expectNext(Scoreboard(List(erwinSpeed)))
    }

    "emits the new version when player speed changes" in {
      val game = testKit.spawn(GameCoordinator())
      val (pub, sub) = testFlow(game, 200.millis)

      val erwinAt200Kpm = PlayerSpeed("erwin", 200)
      val erwinAt220Kpm = erwinAt200Kpm.copy(strokesPerMinute = erwinAt200Kpm.strokesPerMinute + 20)

      pub.sendNext(erwinAt200Kpm)
      pub.sendNext(erwinAt220Kpm)

      sub.request(2)
        .expectNext(Scoreboard(List(erwinAt200Kpm)), Scoreboard(List(erwinAt220Kpm)))
    }

    "players moves into AFK without sending any speed within expected time and emits the new scoreboard" in {
      val game = testKit.spawn(GameCoordinator())
      val (pub, sub) = testFlow(game, 200.millis)

      val erwinAt200Kpm = PlayerSpeed("erwin", 200)

      pub.sendNext(erwinAt200Kpm)
      sub.request(3)
        .expectNext(Scoreboard(List(erwinAt200Kpm)))
        .expectNoMessage(200.millis)
        .expectNext(Scoreboard(List.empty, List(erwinAt200Kpm)))
    }

    "flow links its stage actor to GameCoordinator" in {
      val probe = testKit.createTestProbe[GameMessage]("game-probe")
      testFlow(probe.ref, 100.millis)

      probe.expectMessageType[LinkStageActor]
    }

    "game status can be observed via GameCoordinator" in {
      val game = testKit.spawn(GameCoordinator())
      val (pub, sub) = testFlow(game, 100.millis)

      val erwinAt200Kpm = PlayerSpeed("erwin", 200)
      val erwinAfk = Scoreboard(List.empty, List(erwinAt200Kpm))


      pub.sendNext(erwinAt200Kpm)
      sub.request(3)
        .expectNext(Scoreboard(List(erwinAt200Kpm)))
        .expectNoMessage(100.millis)
        .expectNext(erwinAfk)

      val scoreboard = Await.result(game.ask[Scoreboard](GetScoreboard), 100.millis)
      scoreboard shouldBe erwinAfk
    }
  }

  private def testFlow(game: ActorRef[GameMessage], afkTimeout: FiniteDuration): (TestPublisher.Probe[PlayerSpeed], TestSubscriber.Probe[Scoreboard]) = {
    implicit val untypedSystem = system.toClassic
    implicit val materializer: Materializer = ActorMaterializer.create(untypedSystem)

    TestSource.probe[PlayerSpeed]
      .via(new ScoreboardFlow(game, afkTimeout))
      .toMat(TestSink.probe[Scoreboard])(Keep.both)
      .run()
  }
}
