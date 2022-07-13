package fs2streams

import fs2._

object PureStreams {

  def main(args: Array[String]): Unit = {

    val stream1 = Stream(1,2,3,4)
      .filter(_ % 2 == 0)
      .map(_ + 1)
      .flatMap(number => Stream(number,number))

    println(stream1.compile.toList)


    // infinite stream

    println(Stream.constant("Bla").map(_ + "a").take(10).compile.toList)
    println(Stream(1,2,3,4).repeatN(2).compile.toList)
    println(Stream(1,2,3,4).repeat.take(8).compile.toList)

  }

}
