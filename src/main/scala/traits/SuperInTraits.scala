package traits

/// stackable modyfikatory
abstract class AbstractClass {
  def func1(): Unit
  def func2(p: String): Unit
}

abstract class AbstractClass2 {
  def func3()
}

trait StackModifier extends AbstractClass {
  abstract override def func2(p: String): Unit = {
    println("argument value:" + p)
    super.func2(p + "StackModifier")
  }
}

trait StackModifer2 extends AbstractClass {
  abstract override def func2(p: String): Unit = {
    println("argument value:" + p)
    super.func2(p + "StackModifier2")
  }
}

trait FilterModifier extends AbstractClass {
  abstract override def func2(p: String): Unit =
    if (p == "Hidden") println("Koniec")
    else super.func2(p)
}

class Trait extends AbstractClass {
  override def func1(): Unit = println("test")

  override def func2(p: String): Unit = {
    println("Stack result:" + p)
  }
}

class TraitStacked extends Trait
  with StackModifier
  with StackModifer2
  with FilterModifier

// 2 traits i ta sama funkcja

trait One {
  def func(): Unit = println("One")
}

trait Two {
  def func(): Unit = println("Two")
}

class TraitSameFunc extends Object
  with One
  with Two {
  override def func(): Unit = super[Two].func()
}

object Trait {
  def main(args: Array[String]): Unit = {
    new TraitStacked().func2("Start")
    new TraitStacked().func2("Hidden")
    new TraitSameFunc().func()
  }
}
