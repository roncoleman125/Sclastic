package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.util.Statistics
import scaly.sclastic.util.Statistics.Interval95
import scaly.sclastic.util.Statistics.Interval90
import scaly.sclastic.util.Statistics.Confidence


/** Tests confidence interval of r */
class Test37 {
  @Test
  def test {
    try {       
    	val r = 0.3057
    	
    	val n = 223493

    	val ci1 = Statistics.confidence(r, n, Interval90)
    	
    	assert(tolerance(0.302,ci1.lower,0.01))
    	assert(tolerance(0.309,ci1.upper,0.01))
    	
    	println("95%% lower = %.4f upper = %.4f".format(ci1.lower,ci1.upper))
    	
    	val ci2 = Statistics.confidence(r, n, Interval95)
    	println("99%% lower = %.4f upper = %.4f".format(ci1.lower,ci1.upper))
    } catch {
      case e: Exception =>
        fail(e.getMessage)
    }
  }
  
  def tolerance(target: Double, value: Double, t: Double): Boolean = {
    value >= target * (1-t) && value <= target * (1+t)
  }
}
