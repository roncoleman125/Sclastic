package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.util.ParserHelper.{dequote, decomment}
import scaly.sclastic.compiler.MethodsCompiler.compile
import scaly.sclastic.compiler.Parser.parse
import scaly.sclastic.util.CompilerHelper
import scaly.sclastic.util.Config
import scaly.sclastic.Sclastic

class Test02 {
  @Test
  def test {
    try {
      val testName = "test-files/MongoCollection.scala_"

      Config.settings("debug") = "true"
        
      val methods = Sclastic.estimate(testName)
      
      CompilerHelper.report(methods)
      
    } catch {
      case e: Exception =>
        println(e)
        fail(e.getMessage)
    }
  }
}
