package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scala.io.Source
import scaly.sclastic.Runner._
import scaly.sclastic.util.Statistics
import scaly.sclastic.util.Statistics.Statistic

/*
 * Sould output:
	means: (1.7727272727272727,7.75)
	vars: (2.458773784355178,110.98255813953489)
	r: 0.31886958624762163
*/
class Test16 {
  @Test
  def test {
    val lines = Source.fromFile("test-files/0-report.txt").getLines
    
    val stats = lines.foldLeft(List[Statistics.Statistic]()) { (stats, line) =>
      val fields = line.split("\\s+")
      
      stats ++ List(Statistic(fields(1).toDouble,fields(2).toDouble))
    }
    
    val r = Statistics.correlb(stats)
    
    println("r: "+r)
  }
}
