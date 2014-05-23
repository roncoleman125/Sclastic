package scaly.sclastic.util

object RankStatistics {
  val PTILE_25 = 0
  val PTILE_50 = 1
  val PTILE_75 = 2
  
  def correl(xs: List[Double], ys: List[Double]): Double = {
    // Transform data to ranks
    val xranks = rank(xs)
    val yranks = rank(ys)
    
    // Compute T from Eq. 5 from Conover (1980), p. 252
    val t = (0 until xranks.size).foldLeft(0.0) { (sum, k) =>
      sum + (xranks(k) - yranks(k)) * (xranks(k) - yranks(k))
    }
    
    val n = xranks.size
    
    val rho = 1 - 6 * t / (n * (n*n - 1))

    rho
  }
  
  /** Compute the corresponding rank of elements in the array */
  def rank(xs: List[Double]): List[Double] = {
    val xsSorted = xs.sort(_ < _)
    (0 until xsSorted.size).foreach(p => println((p+1)+". "+xsSorted(p)))
    
    // Compute the ranks accounting for ties, that is, using
    // the average index if there are ties
    val ranks = xs.map { x =>
      // Find and last indexes of the element
      val i = xsSorted.findIndexOf(a => a == x) + 1
      
      val j = xsSorted.findLastIndexOf(a => a == x) + 1
      
      // k is the number of ties
      val k = j - i + 1
      
      // Compute the average rank according to this formula which
      // works even when k == 1
      val rank = (k * i + k * (k-1)/2.) / k

      rank
    }
    
    ranks
  }
  
  /** Identify the quantiles of an unsorted distribution */
  def quantify(xs: List[Double]): List[Double] = {
    if(xs.size < 4)
      null
    else {
      val xsSorted = xs.sort(_ < _)
      val ptiles = List(0.25,0.50,0.75)
     
      ptiles.foldLeft(List[Double]()) { (as, a) =>
        val indx = (a * xs.size).toInt
       
        val v = if(indx % 2 != 0) xsSorted(indx-1) else (xsSorted(indx)+xsSorted(indx-1))/2
       
        as ++ List(v)
      }
    }
  }
  
  def main(args: Array[String]): Unit = {
    test01
  }
  
  /** Test data from Conover (1980) p. 252-253 */
  def test00 {
    val xs = List(86.,71,77,68,91,72,77,91,70,71,88,87)
    val ys = List(88.,77,76,64,96,72,65,90,65,80,81,72)
    
    val rho = correl(xs,ys);
    println("rho = "+rho+" expected 0.7378")    
  }
  
  def test01 {
    val xs = List(86.,71,77,68,91,72,77,91,70,71,88,87)
    val ys = List(88.,77,76,64,96,72,65,90,65,80,81,72)
    
    val ptiles = quantify(ys)
    println(ptiles)
  }
}