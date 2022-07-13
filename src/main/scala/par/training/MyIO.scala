package par.training

import cats.effect.unsafe.implicits.global
import cats.effect.{Deferred, IO, IOApp}
import cats.{Applicative, Foldable, Monad, Monoid}
import cats.implicits._

import java.util.concurrent.{ExecutorService, Executors}
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}


// concurrency
// A1 A2 A3 A4 ->
// B1 B2 B3 B4 ->
// A1 B1 A2 B2 A3 A4 B3 B4 - zmapowane na jeden watek
// A1 B1
// B2 B3 A2 A3 B4 A4


// A1 A2 hjjkkk
//       A3 A4
// parallelism - x rzeczy na raz

// hardware - x wątków
// wątki systemowe - kernelowe
// JVM threads -> new Thread.execute( () => xx
//      green threads
//      Thread pool -
// virtual thread - Project LOOM

/// semantyczny blokowanie

/// Pure(3+5) -> Pure(8)
///Delay(3+5)

trait MyFiber[F[_], A] {
  def join(): F[A]
}

sealed trait MyIO[+A]
case class FlatMap[+A, B](io: MyIO[B], k: B => MyIO[A]) extends MyIO[A]
case class Map[+A, B](io: MyIO[B], k: B => A) extends MyIO[A]
case class Pure[+A](b: A) extends MyIO[A]
case class Delay[+A](eff: () => A) extends MyIO[A]
/// Async
case class Async[+A](k: (Either[Throwable, A] => Unit) => Unit) extends MyIO[A]
case class RaiseError[+A](throwable: Throwable) extends MyIO[A]

object MyIOUtil {
  val threadPool: ExecutorService = Executors.newFixedThreadPool(8)
  val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(threadPool)
}

object MyIOInterpreter {
  def unsafeRunSync[A](io: MyIO[A]): A = {
    @tailrec
    def loop(current: MyIO[Any], stack: List[Any => MyIO[Any]]): A = {
      current match {
        case FlatMap(io, func) => loop(io, func :: stack)
        case Map(io, func) => loop(io, func.andThen(p => Pure(p)) :: stack)
        case Delay(func) => loop(Pure(func()), stack)
        case Pure(v) => stack.headOption match {
          case None => v.asInstanceOf[A]
          case Some(bind) =>
            val nextIO = bind(v)
            loop(nextIO, stack.tail)
        }
      }
    }
    loop(io, List.empty)
  }

  // TU SKONCZYLISMY
  def unsafeRunAsync[A](io: MyIO[A], cb: Either[Throwable, A] => Unit): Unit = {
    def loop(current: MyIO[Any], stack: List[Any => MyIO[Any]], cb: Either[Throwable, A] => Unit): Unit = {
      current match {
        case FlatMap(io, func) => loop(io, func :: stack, cb)
        case Map(io, func) => loop(io, func.andThen(p => Pure(p)) :: stack, cb)
        case Delay(func) => loop(Pure(func()), stack, cb)
        case Pure(v) => stack.headOption match {
          case None => cb(Right(v.asInstanceOf[A]))
          case Some(bind) =>
            val nextIO = bind(v)
            loop(nextIO, stack.tail, cb)
        }
        case RaiseError(throwable) => throw throwable
        case Async(asyncFunc) =>
          val restOfComputation = { res: Either[Throwable, Any] =>
            val nextIO = res.fold(RaiseError(_), Pure(_))
            loop(nextIO, stack, cb)
          }
          asyncFunc(restOfComputation)
      }
    }
    loop(io, List.empty, cb)
  }
}

object MyIO {
  implicit class MyIOOps[A](io: MyIO[A]) {
    def pure(a: A): MyIO[A] = Pure(a)
    def delay(a: () => A): MyIO[A] = Delay(a)
    def flatMap[B](k: A => MyIO[B]): FlatMap[B, A] = FlatMap(io, k)
    def map[B](k: A => B): Map[B, A] = Map(io, k)
    def raiseError[B](throwable: Throwable): RaiseError[Nothing] = RaiseError(throwable)
    def async(k: (Either[Throwable, A] => Unit) => Unit): Async[A] = Async(k)
    def spawn: MyIO[Unit] = MyIO.spawn(io)
    def start: MyIO[MyFiber[MyIO, A]] = MyIO.start(io)
    def unsafeRunSync(): A = MyIOInterpreter.unsafeRunSync(io)
    def unsafeRunAsync(cb: Either[Throwable, A] => Unit): Unit = MyIOInterpreter.unsafeRunAsync(io, cb)
  }

  def pure[A](a: A): MyIO[A] = Pure(a)

  def shift(): MyIO[Unit] = Async { cb =>
    MyIOUtil.threadPool.execute(() => cb(Right(())))
  }

  def spawn[A](io: MyIO[A]): MyIO[Unit] = Async { cb =>
    shift().flatMap(_ => io).unsafeRunAsync(_ => ())
    cb(Right())
  }

  def start[A](io: MyIO[A]): MyIO[MyFiber[MyIO, A]]= {
    val deferred = Deferred.unsafe[IO, A]
    Async { cb =>
      shift().flatMap(_ => io)
        .unsafeRunAsync {
          case Right(value) => deferred.complete(value).unsafeRunSync()
          case Left(_) => () // exception thrown
        }
      cb(Right(() => Delay(() => deferred.get.unsafeRunSync())))
    }
  }

  def sequence[F[_], A](ios: F[MyIO[A]])(
    implicit foldableF: Foldable[F], applicativeF: Applicative[F], monoidF: Monoid[F[A]]): MyIO[F[A]] =
    foldableF.foldLeft[MyIO[A], MyIO[F[A]]](ios, MyIO.pure(monoidF.empty))((accIO, nextIO) =>
      for {
        acc <- accIO
        next <- nextIO
      } yield monoidF.combine(acc, applicativeF.pure(next))
    )

  def parSequence[A](ios: List[MyIO[A]]): MyIO[List[A]] = {
    val started = ios.foldLeft(MyIO.pure(List.empty[MyFiber[MyIO, A]])) { (accIO, nextIO) =>
      for {
        acc <- accIO
        next <- nextIO.start
      } yield acc :+ next
    }

    started.flatMap { fibers =>
      fibers.foldLeft(MyIO.pure(List.empty[A])){ (accIO, fiber) =>
        for {
          acc <- accIO
          fiberRes <- fiber.join()
        } yield acc :+ fiberRes
      }
    }
  }

}

object MyIOTest extends IOApp.Simple {

  val myExample1: Int = Pure(5)
    .map(_ + 5)
    .map(_ + 10)
    /// async
    .flatMap(p => Pure(p + 10))
    .unsafeRunSync()

  def myExampleAsync(): Unit = Async[Int] { cb =>
      MyIOUtil.threadPool.execute { () =>
        cb(Right(10))
      }
    }
    .map(_ + 10)
    .unsafeRunAsync {
      case Right(value) => println("Async first test: " + value)
      case Left(throwable) => throw throwable
    }

  val async1: MyIO[Int] = Delay(() => println("Start async1"))
    .flatMap { _ =>
      Thread.sleep(5000)
      println("Async1 finished")
      Pure(5)
    }

  val async2: MyIO[Int] = Delay(() => println("Start async2"))
    .flatMap { _ =>
      Thread.sleep(6000)
      println("Async2 finished")
      Pure(10)
    }

  val spawnTest = for {
    _ <- async1.spawn
    a2 <- async2.spawn
  } yield ()

  val fiberTest = for {
    fiber <- async1.start
    _ <- async2.spawn
    _ <- Delay(() => println("Started async1, waiting for him to finish" ))
    result <- fiber.join()
    _ <- Delay(() => println(s"Received $result"))
  } yield ()

  val sequenceTest: MyIO[List[Int]] = MyIO.sequence(List(async1, async2))
  val sequence2Test: MyIO[Option[Int]] = MyIO.sequence(Option(async1))

  val parSequenceTest = MyIO.parSequence(List(async1, async2)).flatMap { p =>
    println(s"Results $p")
    Delay(() => ())
  }

  override def run: IO[Unit] = IO { parSequenceTest.unsafeRunAsync(_ => ()) }

}

