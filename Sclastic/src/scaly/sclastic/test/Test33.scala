package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.util.ParserHelper.{dequote, decomment}
import scaly.sclastic.compiler.MethodsCompiler.compile
import scaly.sclastic.compiler.Parser.parse
import scaly.sclastic.util.CompilerHelper
import scaly.sclastic.Sclastic
import scaly.sclastic.util.Config

/** Tests the m calculation when there is && and || */
class Test33 {
  @Test
  def test {
    try {       
      val testName = "test-files/a4.scala_"

      val methods = Sclastic.estimate(testName)
      
      CompilerHelper.report(methods)
      
    } catch {
      case e: Exception =>
        fail(e.getMessage)
    }
  }
}
