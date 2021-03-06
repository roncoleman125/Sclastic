/*
 * Copyright (c) Sclastic Contributors
 * See CONTRIBUTORS.TXT for a full list of copyright holders.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Scaly Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE DEVELOPERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package scaly.sclastic.util

import scala.io.Source
import scaly.sclastic.compiler.MethodsCompiler.Descriptor

object Statistics {
  case class Statistic(cc: Double, len: Double)
  
  def correlFile(reportFileName: String): Double = {
    
    // Pass 0
    val lines0 = Source.fromFile(reportFileName).getLines
    
    var n = 0
    
    val sumsu = lines0.foldLeft(Tuple2[Double,Double](0,0)) { (sum, line) =>
      val fields = line.split("\\s+")
      
      fields(0) match {
        case "|" =>
            n += 1
            
        	val point = Statistic(fields(1).toInt,fields(2).toInt)
      
        	(sum._1 + point.cc, sum._2 + point.len)
        	
        case _ =>
          sum
      }

    }
    
    val means = (sumsu._1 / n,sumsu._2 / n)  
    println("methods: "+n)
    println("mean cc: %-6.4f len: %-6.4f".format(means._1,means._2))
    
    // Pass 1
    val lines1 = Source.fromFile(reportFileName).getLines
    
    val sumsv = lines1.foldLeft(Tuple2[Double,Double](0.0,0)) { (sum, line) =>
      val fields = line.split("\\s+")
      fields(0) match {
        case "|" =>
                
        	val point = Statistic(fields(1).toInt,fields(2).toInt)
        	(sum._1 + sqr(point.cc - means._1), sum._2 + sqr(point.len - means._2))
        case _ =>
          sum
      }

    }

    val vars = (sumsv._1 / (n-1), sumsv._2 / (n-1))
    println("var cc: %-6.3f len: %-6.3f".format(vars._1,vars._2))
    
    // Pass 2    
    val lines2 = Source.fromFile(reportFileName).getLines
    
    val sumr = lines2.foldLeft(0.0) { (sum, line) =>
      val fields = line.split("\\s+")
      
      fields(0) match {
        case "|" =>
    	  val point = Statistic(fields(1).toInt,fields(2).toInt)
    	  sum + (point.cc - means._1) / sqrt(vars._1) * ((point.len - means._2) / sqrt(vars._2))  
    	  
        case _ =>
          sum
      }
    }

    val r = sumr / (n - 1)
    
    r
  }
  
  case class Result(r: Double, n: Int, um: Double, ulen: Double, sm: Double, slen: Double)
  
  def correla(stats: List[Descriptor]): Result = {
    val n = stats.size
    
    val sumsu = stats.foldLeft(Tuple2[Double,Double](0,0)) { (sum, point) =>
      (sum._1 + point.m, sum._2 + point.len)
    }
    
    val means = (sumsu._1 / n,sumsu._2 / n)
    
    val sumsv = stats.foldLeft(Tuple2[Double,Double](0.0,0)) { (sum, point) =>
      (sum._1 + sqr(point.m - means._1), sum._2 + sqr(point.len - means._2))
    }
    
    val vars = (sumsv._1 / (n-1), sumsv._2 / (n-1))
    
    val sumr = stats.foldLeft(0.0) { (sum, point) =>
      sum + (point.m - means._1) / sqrt(vars._1) * ((point.len - means._2) / sqrt(vars._2))
    }
    
    val r = sumr / (n - 1)
    
    Result(r, n, means._1, means._2, vars._1, vars._2)   
  }
  
  def correlb(stats: List[Statistic]): Double = {   
    val n = stats.size
    println("n: "+n)
    
    val sumsu = stats.foldLeft(Tuple2[Double,Double](0,0)) { (sum, point) =>
      (sum._1 + point.cc, sum._2 + point.len)
    }
    
    val means = (sumsu._1 / n,sumsu._2 / n)
    println("means: "+means)
    
    val sumsv = stats.foldLeft(Tuple2[Double,Double](0.0,0)) { (sum, point) =>
      (sum._1 + sqr(point.cc - means._1), sum._2 + sqr(point.len - means._2))
    }
    
    val vars = (sumsv._1 / (n-1), sumsv._2 / (n-1))
    println("vars: "+vars)
    
    val sumr = stats.foldLeft(0.0) { (sum, point) =>
      sum + (point.cc - means._1) / sqrt(vars._1) * ((point.len - means._2) / sqrt(vars._2))
    }
    
    val r = sumr / (n - 1)
    
    r
  }
  

  case class Interval(ci: Double)
  case object Interval90 extends Interval(1.645)
  case object Interval95 extends Interval(1.96)
  case object Interval98 extends Interval(2.326)
  case object Interval99 extends Interval(2.5796)
  case class Confidence(lower: Double,upper: Double)
  
  /** See Shen and Lu, "Computation of Correlation Coefficient and Its Confidence Interval in SAS", paper 170-31, SUGI 31 */  
  def confidence(r: Double, n: Int, interval: Interval = Interval95): Confidence = {
    val fishersz = 0.5 * (ln(1+r)-ln(1-r))
    
    val sigmaz = 1 / sqrt(n - 3)
    
    val zetal = fishersz - interval.ci*sigmaz
    
    val zetau = fishersz + interval.ci*sigmaz
    
    val lower = (exp(2*zetal) - 1) / (exp(2*zetal)+1)
    
    val upper = (exp(2*zetau) - 1) / (exp(2*zetau)+1)
    
    Confidence(lower,upper)
  }
  
  def ln(a: Double) = Math.log(a)
  
  def exp(a: Double) = Math.exp(a)
  
  def sqr(a: Double) = a * a
  
  def sqrt(a: Double) = Math.sqrt(a)
  
  def stdev(sum: Double, sumsq: Double, n: Int) = sqrt((sumsq - sqr(sum)/n) / (n-1))

}