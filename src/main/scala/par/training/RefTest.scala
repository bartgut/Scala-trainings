package par.training

import cats.effect.{IO, IOApp, Ref}
import cats.implicits._

import scala.concurrent.duration.DurationInt

object RefTest extends IOApp.Simple {

  def process(ref: Ref[IO, Int]): IO[Unit] = for {
    _ <- IO.println("worker starting")
    _ <- IO.sleep(1.second)
    value <- ref.updateAndGet(_ + 1)
    _ <- IO.println(s"Current value: $value")
  } yield ()

  override def run: IO[Unit] = for {
    ref <- IO.ref[Int](0)
    _ <- List(process(ref), process(ref), process(ref)).parSequence.void
  } yield ()

}
