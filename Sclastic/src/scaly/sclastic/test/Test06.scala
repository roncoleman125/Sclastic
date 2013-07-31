package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.util.ParserHelper.{dequote, decomment}
import scaly.sclastic.compiler.MethodsCompiler.compile
import scaly.sclastic.compiler.Parser.parse
import scaly.sclastic.util.CompilerHelper

class Test06 {
  @Test
  def test {
    try {
      val testName = "test-files/MapReduceResult.scala_"

      val lines = dequote(decomment(testName))
      (0 until lines.size).foreach(n => println(n + ":" + lines(n)))

      val parseInfos = parse(testName,lines)
      
      val methods = compile(parseInfos,testName,lines)
      CompilerHelper.report(methods)
      
    } catch {
      case e: Exception =>
        fail(e.getMessage)
    }
  }
}
