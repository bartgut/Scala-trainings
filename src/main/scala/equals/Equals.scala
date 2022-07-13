package equals

import scala.collection.mutable

object Equals {

  class Point(val x: Int, val y: Int) {
    override def hashCode(): Int = 41 * ( 41 + x.hashCode()) + y.hashCode()

    override def equals(obj: Any): Boolean = obj match {
      case that: Point => (that.canEqual(this)) && this.x == that.x && this.y == that.y
      case _ => false
    }

    def canEqual(other: Any) = other.isInstanceOf[Point]
  }

  class Point3(x: Int, y: Int, val z: Int) extends Point(x,y) {
    override def equals(obj: Any): Boolean = obj match {
      case that: Point3 => that.canEqual(this) &&  super.equals(that) && this.z == that.z
      case _ => false
    }

    override def canEqual(other: Any): Boolean = other.isInstanceOf[Point3]
  }
  // x.equals(null) => false
  // x.equals(y) == x.equals(y)
  // x.equals(x) == true
  // x.equals(y) == y.equals(x)
  // x.equals(y) == y.equals(z) ==> x.equals(z)

  def main(args: Array[String]): Unit = {
    val p = new Point(1,1)
    val set = mutable.HashSet(p)

    val p1 = new Point(1,1)
    val p2 = new Point3(1,1,1)

    val p3Anon = new Point(1,1) { override val x = 1 }
    println(p3Anon.getClass)
    println(p1.equals(p2))
    println(p2.equals(p1))
  }
}
