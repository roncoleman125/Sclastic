package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.Sclastic
import scaly.sclastic.util.Config
import scaly.sclastic.util.CompilerHelper
import scaly.sclastic.util.Lint

/** Tests: elastic vs. null books */
class Test43 {
  @Test
  def test {
    try {      
      Config.loadConfig("configs/null-config.txt")
      
      Config.settings("debug") = "true"
        
//      val testName = "test-files/ConcurrentLinkedBlockingQueue.scala_"
      val testName = "test-files/TypedActorSpec.scala_"


      val methods = Sclastic.estimate(testName)
      
      CompilerHelper.report(methods)
      
    } catch {
      case e: Exception =>
        fail(e.getMessage)
    }
  }
}