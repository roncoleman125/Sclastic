package scaly.sclastic.util

import scala.io.Source
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map

object StreamRankStatistics {
  def main(args:Array[String]): Unit = {
    val xs = args(1)
    val ys = args(2)
    
    val config = Config.loadConfig(args(0))
    
    val dir = config("workdir")
    
    val xsRanks = getRanks(dir+xs+"-uniq.txt",dir+xs+"-sorted.txt")
    
    println("*** xs")
    xsRanks.foreach { p =>
      println(p._1+" "+p._2)
      }
    
    val ysRanks = getRanks(dir+ys+"-uniq.txt",dir+ys+"-sorted.txt")
    
//    println("*** ys")
//    ysRanks.foreach { p =>
//      println(p._1+" "+p._2)
//    }
    
    val report = config("report")
    val results = Source.fromFile(dir+"xy.txt").getLines().foldLeft(Tuple2(0.0,0)) { (sum,line) =>
      println(sum._2+": "+line)
      if(line.startsWith("|")) {
        val values = line.split("\\s+")
        
        val xi = values(1).toInt
        val rxi = xsRanks(xi)
        
        val yi = values(2).toInt
        val ryi = ysRanks(yi)
        
        val t = sum._1 + (rxi - ryi) * (rxi - ryi)
        val n = sum._2 + 1
        (t,n)
      }
      else
        sum
    }
    
    val rho = 1 - 6 * results._1 / (results._2 * (results._2 * results._2 - 1))
    println("rho = "+rho)
  }
  
  def getRanks(pathUniqs: String,pathSorted: String): Map[Int,Double] = {
    val uniqs = Source.fromFile(pathUniqs).getLines().map{ a =>
      a.toInt
    }.toList
    
    // Index of unique
    var uniqIndx = 0
    var startIndx = 1
    var sortedIndx = 0
    var count = 0
    var rankSum = 0.0
    val lines = Source.fromFile(pathSorted).getLines()
    
    val ranks = HashMap[Int,Double]().withDefaultValue(0)
    
    var u = uniqs(uniqIndx)
    
    lines.foreach { line =>
      val p = line.toInt
      sortedIndx += 1

      if(p != u) {
        val rank = rankSum / count
//        println("u=%d rank=%f ranksum=%f count=%d".format(u,rank,rankSum,count))
        
        ranks(u) = rank
        count = 0
        
        u = p
        
        uniqIndx += 1
        
        rankSum = 0
        
        if(uniqIndx >= uniqs.size)
          System.exit(0)
      }
            
      rankSum += sortedIndx
      count += 1
    }
    
    val rank = rankSum / count
//    println("u=%d rank=%f ranksum=%f count=%d".format(u,rank,rankSum,count))
    
    ranks(u) = rank  
    
    ranks
  }

}