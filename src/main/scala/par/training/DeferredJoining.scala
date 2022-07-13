package par.training

import cats.effect.{IO, IOApp}

import scala.concurrent.duration.DurationInt

object DeferredJoining extends IOApp.Simple {
  override def run: IO[Unit] = for {
    deferred <- IO.deferred[Int]
    _ <- (IO.println("Proc 1, sleeping") >> IO.sleep(4.seconds) >> deferred.complete(10)).start
    proc2 <- (IO.println("Proc 2, waiting for the first") >> deferred.get.flatMap(value => IO.println(s"Received value $value"))).start
    _ <- proc2.join
  } yield ()

}
