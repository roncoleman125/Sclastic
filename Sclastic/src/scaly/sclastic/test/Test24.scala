package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.util.ParserHelper.{dequote, decomment}
import scaly.sclastic.compiler.MethodsCompiler.compile
import scaly.sclastic.compiler.Parser.parse
import scaly.sclastic.util.CompilerHelper

class Test24 {
  @Test
  def test {
    try {
    	println("world")
      
    } catch {
      case e: Exception =>
        fail(e.getMessage)
    }
  }
  def foo =
    List(1,2,3).
      foreach(
          "println"
          )
  
  def bar =
    if(true) 1
    else
      2
          
  print("hello ")
}
