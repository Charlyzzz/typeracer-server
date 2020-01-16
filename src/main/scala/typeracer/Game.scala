package typeracer

import scala.collection.mutable

class Game() {

  private val playing: mutable.Map[String, PlayerSpeed] = mutable.Map()

  private val afk: mutable.Map[String, PlayerSpeed] = mutable.Map()

  def registrarVelocidadDeJugador(playerSpeed: PlayerSpeed): Unit = {
    playing.put(playerSpeed.username, playerSpeed)
    afk.remove(playerSpeed.username)
  }

  def scoreboard: Scoreboard = Scoreboard.from(playing.values.toSeq, afk.values.toSeq)

  def registrarJugadorAfk(username: String): Any = {
    val playerSpeed = playing.remove(username)
    playerSpeed.foreach(afk.put(username, _))
  }
}
