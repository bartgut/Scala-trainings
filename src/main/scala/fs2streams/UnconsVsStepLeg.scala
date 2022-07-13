package fs2streams

import cats.effect.IO
import fs2.Pull

import scala.concurrent.ExecutionContext

object UnconsVsStepLeg {

  def connectUncons[F[_]]: (fs2.Stream[F, Int], fs2.Stream[F, Int]) => fs2.Stream[F, Int] = {
    def go(stream1: fs2.Stream[F, Int], stream2: fs2.Stream[F, Int]): Pull[F, Int, Unit] =
      stream1.pull.uncons1.flatMap { stream1Element =>
        stream2.pull.uncons1.flatMap { stream2Element =>
          (stream1Element, stream2Element) match {
            case (Some((stream1Head, stream1Tail)), Some((stream2Head, stream2Tail))) =>
              println("Some, Some")
              Pull.output1(stream1Head + stream2Head) >> go(stream1Tail, stream2Tail)
            case (Some((stream1Head, stream1Tail)), None) =>
              println("1 Stream still available")
              Pull.output1(stream1Head) >> go(fs2.Stream.empty, stream1Tail)
            case (None, Some((stream2Head, stream2Tail))) =>
              println("2 Stream still available")
              Pull.output1(stream2Head) >> go(fs2.Stream.empty, stream2Tail)
            case _ => Pull.output1(-1)
          }
        }
      }
    (one, two) => go(one, two).stream
  }

  def connectStepLeg[F[_]]: (fs2.Stream[F, Int], fs2.Stream[F, Int]) => fs2.Stream[F, Int] = {
    def go(stream1: fs2.Stream[F, Int], stream2: fs2.Stream[F, Int]): Pull[F, Int, Unit] =
      stream1.pull.stepLeg.flatMap { stream1Element =>
        stream2.pull.stepLeg.flatMap { stream2Element =>
          (stream1Element, stream2Element) match {
            case (Some(sl1), Some(sl2)) =>
              println("Some, Some")
              val one = sl1.head(0)
              val two = sl2.head(0)
              Pull.output1(one + two) >> go(sl1.stream, sl2.stream)
            case (Some(sl1), None) =>
              val one = sl1.head(0)
              println("1 Stream still available")
              Pull.output1(one) >> go(sl1.stream, fs2.Stream.empty)
            case (None, Some(sl2)) =>
              val two = sl2.head(0)
              println("2 Stream still available")
              Pull.output1(two) >> go(fs2.Stream.empty, sl2.stream)
            case _ => Pull.output1(-1)
          }
        }
      }
    (one, two) => {
      go(one.flatMap(fs2.Stream.emit), two.flatMap(fs2.Stream.emit)).stream
    }
  }

  def someText(s: Option[String]*) = s.flatten.mkString("-")

  def main(args: Array[String]): Unit = {
    val dbExecutionContext = ExecutionContext.global
    implicit val contextShift: ContextShift[IO] = IO.contextShift(dbExecutionContext)

    val a = Some("someText1")
    val b = Some("someText2")
    println(a.flatMap(aa => b.map(bb => aa + bb)))
    val c = None
    println(someText(a,b))
    println(someText(a,c))
    println(someText(c,b))

    val stream1 = fs2.Stream.bracket(IO { println("Acquire 1"); 2})(_ => IO { println("Release 1") })
      .flatMap(p => fs2.Stream.range(1,p))

    val stream2 = fs2.Stream.bracket(IO { println("Acquire 2"); 4})(_ => IO { println("Release 2") })
      .flatMap(p => fs2.Stream.range(1,p))

    stream1.through2(stream2)(connectStepLeg).compile.toList.unsafeRunSync().foreach(println(_))
    //stream1.through2(stream2)(connectUncons).compile.toList.unsafeRunSync().foreach(println(_))
  }

}
