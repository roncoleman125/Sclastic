package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.Sclastic
import scaly.sclastic.util.Config
import scaly.sclastic.util.CompilerHelper

/** Tests snippet 3, "A study of complexity in functional programs" */
class Test40 {
  @Test
  def test {
    try {       
      Config.settings("debug") = "true"
        
      val testName = "test-files/a9.scala_"

      val methods = Sclastic.estimate(testName)
      
      CompilerHelper.report(methods)
      
    } catch {
      case e: Exception =>
        fail(e.getMessage)
    }
  }
}
