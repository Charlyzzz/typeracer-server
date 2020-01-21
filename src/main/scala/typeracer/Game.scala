package typeracer


import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Behavior}
import akka.{actor => classic}

import scala.collection.mutable

class Game() {

  private val playing: mutable.Map[String, PlayerSpeed] = mutable.Map()

  private val afk: mutable.Map[String, PlayerSpeed] = mutable.Map()

  def trackPlayerSpeed(playerSpeed: PlayerSpeed): Unit = {
    playing.put(playerSpeed.username, playerSpeed)
    afk.remove(playerSpeed.username)
  }

  def scoreboard: Scoreboard = Scoreboard.from(playing.values.toSeq, afk.values.toSeq)

  def trackAfkPlayer(username: String): Unit = {
    val playerSpeed = playing.remove(username)
    playerSpeed.foreach(afk.put(username, _))
  }
}

object GameCoordinator {

  def apply(): Behavior[GameMessage] = Behaviors.receive {
    case (_, LinkStageActor(stageActor)) =>
      linked(stageActor)
    case (ctx, _: GetScoreboard) =>
      ctx.log.warning("Received GetScoreboard after beign bounded; ignoring")
      Behaviors.same
  }

  private def linked(actor: classic.ActorRef): Behavior[GameMessage] = Behaviors.receiveMessage {
    case GetScoreboard(replyTo) =>
      actor.tell(GetScoreboard, replyTo.toClassic)
      Behaviors.same
    case _ => Behaviors.ignore
  }
}

sealed trait GameMessage

case class LinkStageActor(stageActor: classic.ActorRef) extends GameMessage

case class GetScoreboard(replyTo: ActorRef[Scoreboard]) extends GameMessage
