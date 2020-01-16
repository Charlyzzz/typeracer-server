package typeracer.scoreboard

import typeracer.{PlayerSpeed, Scoreboard}

trait ScoreboardCompanion {

  def Desc[A](implicit ordering: Ordering[A]): Ordering[A] = ordering.reverse

  def from(competingPlayers: Seq[PlayerSpeed], afkPlayers: Seq[PlayerSpeed] = Seq.empty): Scoreboard = {
    val desc = Ordering[Int].reverse
    val topTen = competingPlayers.sortBy(_.strokesPerMinute)(Desc).take(10)
    Scoreboard(topTen, afkPlayers)
  }
}
