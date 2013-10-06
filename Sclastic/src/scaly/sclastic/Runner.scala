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
package scaly.sclastic

import scaly.sclastic.util.ParserHelper._
import scaly.sclastic.util.Config._
import scaly.sclastic.compiler.Parser._
import scaly.sclastic.compiler.MethodsCompiler._
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.io.PrintWriter
import java.io.File
import scala.io.Source
import scala.collection.mutable.HashMap
import java.util.Date
import scaly.sclastic.util.Config
import scaly.sclastic.util.Statistics
import scaly.sclastic.util.FileWalker
import scaly.sclastic.compiler.MethodsCompiler
import scaly.sclastic.util.Lint
import scaly.sclastic.compiler.Parser

/** This object is the main driver of the long running experiment. */
object Runner { 
  // These are counters the program tracks
  var countRaw = 0
  var countStripped = 0
  var passed = 0
  var failed = 0
  var totalMethods = 0
  var count = 0
  var totalt = 0.0
    
  def main(args: Array[String]): Unit = {
    val begin = System.currentTimeMillis

    // Get the configuration
    val config = loadConfig(args(0))
    
    val root = config("root")

    val report = config("report")
    
    val dir = config("workdir")

    // Override settings in the config file with command line settings
    (1 until args.length-1).foreach ( k => config(args(k)) = args(k+1) )
    
    // Open the report file
    val out = new PrintWriter(new File(report))
    
    // Get the list of zips
    val zips = FileWalker.walk(root, ".zip")
    
    val numZips = zips.size
    
    // Initialize the complexity distribution
    val complexities = HashMap[Int,Int]().withDefaultValue(0)
    
    // Initialize the line count distribution
    val lengths = HashMap[Int,Int]().withDefaultValue(0)

    // Process each zip in turn
    zips.foreach { zip => 
      
      val startt = System.currentTimeMillis
      val starth = Lint.count
      
      // Get the name of the zip file without the path
      val input = zip.split(java.io.File.separator).last

      // Send progress report to user
      print((numZips-count)+": processing "+input+" ...")
      Console.out.flush
      
      out.println("** " + zip)
      out.flush

      // Open the zip file
      val rootzip = new ZipFile(zip);

      // Retrieve the zip entries
      import collection.JavaConverters._

      val entries = rootzip.entries.asScala

      // Process each file in turn in the zip
      var methodsPerProj = 0
      var summPerProj = 0
      var summ2PerProj = 0
      var sumlenPerProj = 0
      var sumlen2PerProj = 0
      
      val countStrippedStart = countStripped
      val countRawStart = countRaw
      
      val stats = entries.foldLeft(List[MethodsCompiler.Descriptor]()) { (stats, entry) =>
        try {
          // Process an entry in the zip
          val results = process(entry, rootzip)

          // The process may fail, if we cannot for some reason
          // cannot parse or compile the uncompressed file
          results match {
            case methods: List[Descriptor] if methods != null =>
              passed += 1
                            
              methods.foreach { method =>
                val m = method.m
                val len = method.len
                val name = cover(method.name)
                val struct = cover(method.struct)
                val path = cover(method.path)
                
                if (len > 0) {
                  out.println("| %2d %3d %18.18s %20.20s %s %s".format(m, len, name, struct, path, input))
                  out.flush

                  complexities(m) += 1
                  lengths(len) += 1

                  methodsPerProj += 1
                  
                  summPerProj += m
                  
                  summ2PerProj += m*m
                  
                  sumlenPerProj += len
                  
                  sumlen2PerProj += len * len
                  
                  totalMethods += 1
                }
              }
              
              stats ++ methods
              
            case _ =>
              stats
          }
          
        } catch {
          case e: Exception =>
            failed += 1
            out.println("exception in "+entry.getName+": " + e.getMessage)
//            println("*** FAILED: "+entry.getName)
            stats
        }
      }
      
      // Try to compute the stats for a given zip file
      // The only reason this can fail is for math reasons
      // e.g., divide by zero, etc.
      try {
        val result = Statistics.correla(stats)
        
        out.println("++ r = %.2f n = %d m = %.1f +- %.1f len = %.1f +- %.1f".
            format(result.r,
                result.n,
                result.um,
                Math.sqrt(result.sm),
                result.ulen,
                Math.sqrt(result.slen)))
        out.flush

        count += 1
        
        val endh  = Lint.count
        val dh = endh - starth
        
        val endt = System.currentTimeMillis

        val dt = endt - startt

        val rawPerEntry = countRaw - countRawStart
        val strippedPerEntry = countStripped - countStrippedStart
        
//                  var summPerProj = 0
//      			var summ2PerProj = 0
//      			var sumlenPerProj = 0
//      			var sumlen2PerProj = 0

        val um = summPerProj / methodsPerProj.toDouble
        val sm = Statistics.stdev(summPerProj, summ2PerProj, methodsPerProj)
        val ulen = sumlenPerProj / methodsPerProj.toDouble
        val slen = Statistics.stdev(summPerProj, summ2PerProj, methodsPerProj)
        
        println("done! %6.4f %d %d %d %d %f %f %f %f %4.1f s".
            format(result.r, dh, methodsPerProj, rawPerEntry, strippedPerEntry, um, sm, ulen, slen, dt / 1000.0))

        totalt += dt
      }
      catch {
        case e: Exception =>
          println("exception processing "+zip+": "+e.getMessage())
      }
    }
    
    out.flush
    out.close
    
    // Write out the summary report
    println("started: "+new Date(begin))
    println("elapsed: %.1f min".format(totalt/1000.0/60))
    
    config.foreach(setting => println(">> "+setting._1+": "+setting._2))

    println("zips: "+zips.size)
    
    println("passed: %d (%.1f%%)".format(passed,passed.toDouble/(passed + failed)*100.0))
    println("failed: %d (%.1f%%)".format(failed,failed.toDouble/(passed + failed)*100.0))
    println("raw: %.1f kloc".format(countRaw/1024.0))
    println("stripped: %.1f kloc (%.0f%%)".format(countStripped/1024.0,(countStripped-countRaw)/countRaw.toDouble*100))
    
    // Read by the file we just wrote out since it may be too
    // big to fit in memory
    val r = Statistics.correlFile(report)
    
    println("hofs: %d / %d %d %d".format(Lint.count,Lint.attempts,Parser.hard,Parser.soft))
    
    println("r: "+r)
    
    // Write out the complexity distribution
    // See http://stackoverflow.com/questions/13675046/scala-sort-list-of-tuples-by-attribute
    val ccsorted = complexities.toList.sorted.groupBy(_._2).values.map(_.head).toList.sorted
    
    println("%3s %7s %6s".format("cc","n","%"))
    
    ccsorted.foreach { cc =>
      println("%3d %7d %6.1f".format(cc._1,cc._2,cc._2/totalMethods.toDouble*100.0))
    }
    
    // Write out the length distribution
    println
    println("%4s %7s %6s".format("len","n","%"))
    
    val lensorted = lengths.toList.sorted.groupBy(_._2).values.map(_.head).toList.sorted
    lensorted.foreach { len =>
      println("%4d %7d %6.1f".format(len._1,len._2,len._2/totalMethods.toDouble*100.0))
    }
    
    println("DONE")
  }

  /** Uncompress and process a given Scala file */
  def process(e: ZipEntry, rootzip: ZipFile): List[Descriptor] = {
    val name = e.getName
        
    name.split(java.io.File.separator).last match {
      case "t6726-patmat-analysis.scala" =>
        null
        
      case path: String if name.endsWith(".scala") =>
        if(Config.debug) println("processing: "+path)
        
        val is = rootzip.getInputStream(e)

        val raw = scala.io.Source.fromInputStream(is).getLines().toList
        countRaw += raw.size

        val delines = dede(raw)
        countStripped += delines.size

        val methods = Sclastic.estimate(path, delines)
        
        is.close
        
        methods
        
      case _ =>
        null
    }
  }
  
  def cover(name: String): String = if(name == null || name.trim.length == 0) "NULL" else name
}