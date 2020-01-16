package typeracer.flow

import akka.stream.scaladsl.{Flow, Source}
import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration

class GroupedWithTimeout[A](n: Int, interval: FiniteDuration) extends GraphStage[FlowShape[A, immutable.Seq[A]]] {

  val in = Inlet[A]("GroupedWithTimeout.in")
  val out = Outlet[immutable.Seq[A]]("GroupedWithTimeout.out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) with InHandler with OutHandler {

      var buffer = List[A]()

      override def preStart(): Unit = schedulePeriodically(None, interval)

      override def onPush(): Unit = {
        val elem = grab(in)
        buffer = buffer :+ elem
        if (buffer.size == n) emitGroup()
        else pull(in)
      }

      override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)

      override protected def onTimer(timerKey: Any): Unit = if (buffer.nonEmpty) emitGroup()

      def emitGroup(): Unit = {
        emit(out, buffer)
        buffer = buffer.empty
      }

      setHandlers(in, out, this)
    }
}

object GroupedWithTimeout {

  implicit class GroupedWithTimeoutFlowExtension[A, B, M](flow: Flow[A, B, M]) {
    def groupedWithTimeout(n: Int, interval: FiniteDuration): Flow[A, Seq[B], M] =
      flow.via(new GroupedWithTimeout(n, interval))
  }

  implicit class GroupedWithTimeoutSourceExtension[A, M](source: Source[A, M]) {
    def groupedWithTimeout(n: Int, interval: FiniteDuration): Source[Seq[A], M] =
      source.via(new GroupedWithTimeout(n, interval))
  }

}