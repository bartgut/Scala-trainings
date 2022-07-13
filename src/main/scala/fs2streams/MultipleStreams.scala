package fs2streams

import cats.effect.IO
import fs2._

import scala.concurrent.ExecutionContext

object MultipleStreams {
  def main(args: Array[String]): Unit = {

    implicit val conc: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

    val stream1 = Stream.range(1,20).evalMap(value => IO { Thread.sleep(250); value })
    val stream2 = Stream.range(1,10).evalMap(value => IO { Thread.sleep(750); value })

    // adding
    println("Concatenating 2 streams")
    val result0 = (stream1 ++ stream2).compile.toList.unsafeRunSync().mkString(", ")
    println(result0)

    // merge
    println("Merging 2 streams")
    val result1 = stream1.merge(stream2).compile.toList.unsafeRunSync().mkString(", ")
    println(result1)

    //zip
    println("Zipping 2 streams")
    val result2 = stream1.zipAllWith(stream2)(0,0){
      (one, two) => one + two }.compile.toList.unsafeRunSync.mkString(", ")
    println(result2)
  }
}
