package typeracer

import extensions.ScalaTestWithActorTestKit
import org.scalatest.freespec.AnyFreeSpecLike

import scala.concurrent.duration._

class GameCoordinatorSpec extends ScalaTestWithActorTestKit with AnyFreeSpecLike {

  "GameCoordinator" - {
    "ignores messages if not linked" in {
      val gameCoordinator = testKit.spawn(GameCoordinator())
      val probe = testKit.createTestProbe[Any]()
      gameCoordinator ! GetScoreboard(probe.ref)
      probe.expectNoMessage(100.millis)
    }
  }
}

