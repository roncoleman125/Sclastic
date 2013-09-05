package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.util.ParserHelper.{dequote, decomment}
import scaly.sclastic.compiler.MethodsCompiler.compile
import scaly.sclastic.compiler.Parser.parse
import scaly.sclastic.Sclastic
import scaly.sclastic.util.CompilerHelper
import scaly.sclastic.util.Config

/** Tests snippet 2, "A study of complexity in functional programs" */
class Test39 {
  @Test
  def test {
    try {
      val testName = "test-files/a8.scala_"
        
      Config.settings("debug") = "true"
        
      Config.loadConfig("configs/github-config.txt")
        
      val methods = Sclastic.estimate(testName)
      
      CompilerHelper.report(methods)
      
    } catch {
      case e: Exception =>
        fail(e.getMessage)
    }
  }
}
