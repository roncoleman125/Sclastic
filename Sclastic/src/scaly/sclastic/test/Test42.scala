package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.Sclastic
import scaly.sclastic.util.Config
import scaly.sclastic.util.CompilerHelper
import scaly.sclastic.util.Lint

/** Tests snippet 3, "A study of complexity in functional programs" */
class Test42 {
  @Test
  def test {
    try {
      Config.loadConfig("configs/github-config.txt")
      Config.phofs.foreach { h =>
        println(h._1)
      }
      
    } catch {
      case e: Exception =>
        fail(e.getMessage)
    }
  }
}
