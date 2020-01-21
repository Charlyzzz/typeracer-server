package typeracer

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class GameSpec extends AnyFreeSpec with Matchers {

  "El juego" - {

    "sin registros nos da un scoreboard vacio" in {
      new Game().scoreboard shouldBe Scoreboard()
    }

    "si se registra solo una velocidad el top ten va a estar compuesto solo ella" in {
      val game = new Game()
      val velocidadDeMati = PlayerSpeed("mati", 10)
      game.trackPlayerSpeed(velocidadDeMati)
      game.scoreboard shouldBe Scoreboard(List(velocidadDeMati))
    }

    "registar una nueva velocidad actualiza la velocidad anterior" in {
      val game = new Game()
      val velocidadDeMati = PlayerSpeed("mati", 10)
      val velocidadDeMatiMasRapido = velocidadDeMati.copy(strokesPerMinute = 20)
      game.trackPlayerSpeed(velocidadDeMati)
      game.trackPlayerSpeed(velocidadDeMatiMasRapido)
      game.scoreboard shouldBe Scoreboard(List(velocidadDeMatiMasRapido))
    }

    "un jugador pasa a estar afk despues de avisarlo y desaparece del top ten" in {
      val game = new Game()
      val velocidadDeMati = PlayerSpeed("mati", 10)
      game.trackPlayerSpeed(velocidadDeMati)
      game.scoreboard shouldBe Scoreboard(List(velocidadDeMati))
      game.trackAfkPlayer("mati")
      game.scoreboard shouldBe Scoreboard(List.empty, List(velocidadDeMati))
    }
  }
}

