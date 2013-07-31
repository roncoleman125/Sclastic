package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.Sclastic
import scaly.sclastic.util.Config
import scaly.sclastic.util.CompilerHelper

/** Tests "" and """ in same file */
class Test35 {
  @Test
  def test {
    try {       
      Config.settings("debug") = "true"
        
      val testName = "test-files/Boot.scala_"

      val methods = Sclastic.estimate(testName)
      
      CompilerHelper.report(methods)
      
    } catch {
      case e: Exception =>
        fail(e.getMessage)
    }
  }
}
