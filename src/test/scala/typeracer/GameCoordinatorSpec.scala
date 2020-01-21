package typeracer

import akka.actor.typed.scaladsl.adapter._
import extensions.ScalaTestWithActorTestKit
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers


class GameCoordinatorSpec extends ScalaTestWithActorTestKit with AnyFreeSpecLike with Matchers {

  "GameCoordinator" - {
    "ignores messages if not linked" in {
      val gameCoordinator = testKit.spawn(GameCoordinator())
      val probe = testKit.createTestProbe[Any]()
      gameCoordinator ! LinkStageActor(probe.ref.toClassic)
    }
  }
}

