package scaly.sclastic.util

import scala.io.Source

object KendallTau {
  def main(args: Array[String]): Unit = {
    val config = Config.loadConfig(args(0))
    
    val dir = config("workdir")
    var n = 0
    val input = Source.fromFile(dir+"kt-report.txt").getLines().foldLeft(List[Tuple2[Int,Int]]()) { (xs,line) =>
      n += 1
      println(n+": "+line)
      val values = line.split("\\s+")
      val x = values(0).toInt
      val y = values(1).toInt
      
      xs ++ List(Tuple2(x,y))
    }
    println("input size = "+input.size)
  }
}