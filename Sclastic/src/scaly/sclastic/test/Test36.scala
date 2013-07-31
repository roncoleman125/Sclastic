package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.Sclastic
import scaly.sclastic.util.Config
import scaly.sclastic.util.CompilerHelper

/** Tests removal of "" see Test17 for real-world test case */
class Test36 {
  @Test
  def test {
    try {       
      Config.settings("debug") = "true"
        
      val testName = "test-files/a6.scala_"

      val methods = Sclastic.estimate(testName)
      
      CompilerHelper.report(methods)
      
    } catch {
      case e: Exception =>
        fail(e.getMessage)
    }
  }
}
