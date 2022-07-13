package fs2streams

import cats.effect.{IO, Resource}
import fs2._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

// Stream[F[_], A] -> IO[Int] -> Stream[IO, Int]
object EffectfulStreams {

  def main(args: Array[String]): Unit = {

    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

    println("Throwing error: " +
      Stream.raiseError[IO](new IllegalStateException("Fatal Error")).handleErrorWith(_ => Stream(0)).compile.toList)

    println("Effects: " +  Stream(1,2,3,4).evalMap(IO(_))
      .map(_ + 1)
      .evalMap(p => IO(p + 1))
      .evalFilter(p => IO(p % 2 == 0))
      .compile
      .toList
      .unsafeRunSync())

    Stream.range(0,10).evalMap(p => IO { println(p) }).compile.drain.unsafeRunSync()
    println("Awake: " + Stream.awakeEvery[IO](2.seconds)
      .take(2)
      .evalMap(time => IO { println(time) } >> IO(time.toSeconds))
      .compile
      .toList
      .unsafeRunSync())

    println("Eval + Iterator: " + Stream.resource(Resource.make(IO { List(1,2,3,4) })(_ => IO { println("Closing the resource") }))
      .map(_.iterator)
      .flatMap(it =>
        Stream.eval(IO { if (it.hasNext) Option(it.next()) else None }).repeat.takeWhile(_.isDefined))
      .compile
      .toList
      .unsafeRunSync())

    /// Pulling

    def takeToSum[F[_]](sum: Int): Stream[F, Int] => Stream[F, Int] = {
      def go(inputStream: Stream[F, Int], currentSum: Int): Pull[F, Int, Unit] =
        inputStream.pull.uncons1.flatMap {
          case Some((head, tail)) =>
            val newSum = currentSum + head
            if (newSum > sum) Pull.done
            else Pull.output1(head) >> Pull.output1(head) >> go(tail, newSum)
          case None => Pull.done
        }
      one => go(one, 0).stream
    }

    println("Through example: " + Stream.eval(IO { List(1,2,3,4,5,6,7) })
      .flatMap(Stream.emits)
      .through(takeToSum(10))
      .compile
      .toList
      .unsafeRunSync())


  }

}
