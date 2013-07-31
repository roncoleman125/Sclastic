package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.util.ParserHelper.{dequote, decomment}
import scaly.sclastic.compiler.MethodsCompiler.compile
import scaly.sclastic.compiler.Parser.parse
import scaly.sclastic.util.CompilerHelper

/* Test comments in quotes */
class Test17 {
  @Test
  def test {
    try {
      val testName = "test-files/AndroidDefaults.scala_"

      val lines = dequote(decomment(testName))
      (0 until lines.size).foreach(n => println(n + ":" + lines(n)))

      val parseInfos = parse(testName,lines)
      (0 until parseInfos.size).foreach(k => println(k+": "+parseInfos(k)))
      
      val methods = compile(parseInfos,testName,lines)
      CompilerHelper.report(methods)
      
    } catch {
      case e: Exception =>
        fail(e.getMessage)
    }
  }
  
  def foo = if(true) 0 else 1
}
