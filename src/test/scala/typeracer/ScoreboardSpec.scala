package typeracer

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ScoreboardSpec extends AnyFreeSpec with Matchers {

  "Scoreboard" - {

    "top ten" - {

      "sin jugadores activos nos da un scoreboard vacio" in {
        Scoreboard.from(List.empty) shouldBe Scoreboard()
      }

      "con un jugador activo da un scoreboard con solo esta" in {
        val velocidadDeMati = PlayerSpeed("mati", 20)
        Scoreboard.from(List(velocidadDeMati)) shouldBe Scoreboard(topTen = List(velocidadDeMati))
      }

      "con dos jugadores activos con velocidades distintas nos da un scoreboard con el top ten decreciente por velocidad" in {
        val velocidadDeMati = PlayerSpeed("mati", 20)
        val velocidadDeErwin = PlayerSpeed("erwin", 10)
        Scoreboard.from(List(velocidadDeErwin, velocidadDeMati)) shouldBe Scoreboard(topTen = List(velocidadDeMati, velocidadDeErwin))
      }

      "con 11 jugadores activos el top ten del scoreboard nos da las 10 mas rápidas" in {
        val velocidadJugadorLento = PlayerSpeed("jugadorLento", 1)
        val velocidadesDeJugadoresRapidos = (1 to 10).map(n => PlayerSpeed(s"jugador$n", 110 - n * 10))

        Scoreboard.from(velocidadesDeJugadoresRapidos :+ velocidadJugadorLento) shouldBe
          Scoreboard(velocidadesDeJugadoresRapidos)
      }
    }

    "afk" - {
      "sin jugadores inactivos nos da una lista vacia" in {
        Scoreboard.from(List.empty, List.empty) shouldBe Scoreboard(List.empty, List.empty)
      }

      "por defecto no tiene jugadores inactivos" in {
        Scoreboard.from(List.empty) shouldBe Scoreboard(List.empty, List.empty)
      }

      "incluye todos los jugadores inactivos" in {
        val cienJugadoresInactivos = (1 to 100).map(n => PlayerSpeed(s"jugador$n", 40))
        Scoreboard.from(List.empty, cienJugadoresInactivos) shouldBe Scoreboard(List.empty, afkPlayers = cienJugadoresInactivos)
      }
    }
  }
}

