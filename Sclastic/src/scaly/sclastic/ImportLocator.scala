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

import scala.io.Source
import scaly.sclastic.util.Config
import scala.collection.mutable.HashMap
import java.io.PrintWriter
import java.io.File

/**
 * This object generates a report of uniquely missing imports. The inputs are 
 * three files: imports, packages (i.e., exports), and structures (i.e., classes
 * traits, or objects) that are recorded by the Sclastic compiler.
 */
object ImportLocator {

  def main(args: Array[String]): Unit = {
    val config = Config.loadConfig(args(0))

    val mports = loadReport(config("imports"))
    
    val pkgs = loadReport(config("pkgs"))
    
    val structs = loadReport(config("structs"))
    
    val out = new PrintWriter(new File("import-misses.txt"))
    
    // Scan the list of imports to see
    val misses = mports.foldLeft(List[String]()) { (misses,mport) =>
      // If we find the import referenced as a package or
      // a class, object, or trait, this is a HIT
      if(pkgs.contains(mport._1) || structs.contains(mport._1))
        misses
      else {
        // If the import is neither in the list of packages or
        // the list of classes, traits, or objects, this is a MISS.
        out.println(mport._1)
        
        misses ++ List(mport._1) 
      }
    }
    
    out.flush
    out.close
    
//     Would love to do this in Scala except the KSH is much faster. See misses.ksh.
//    val misses = loadReportRaw("imports-raw")
//    println("counting misses...")
//    val miscount = (0 until misses.size).foldLeft(0) { (total, k) =>
//      val miss = misses(k)
//      println(k+". "+miss)
//      
//      rawImports.foldLeft(0) { (count,mport) =>
//        if(mport == miss) count + 1 else count
//      } + total
//    }
    
    println("num imports = "+mports.size)
    println("num pkgs = "+pkgs.size)
    println("num structs = "+structs.size)
    println("import misses = "+misses.size)
  }
  
  /** Loads the report which it returns as a hash map, that is, w/o duplicates. */
  def loadReport(path: String): HashMap[String,String] = {
    val lines = Source.fromFile(path).getLines
    
    lines.foldLeft(HashMap[String,String]()) { (map,line) =>
      val component = line.split("\\.")(0).trim
      
      map(component) = component

      map
    }
  }
  
  /** Loads the report "raw" meaning, with duplicates */
  def loadReportRaw(path: String): List[String] = {
    val lines = Source.fromFile(path).getLines
    
    lines.foldLeft(List[String]()) { (list,line) =>
      val component = line.split("\\.")(0).trim

      list ::: List(component)
    }
  }
}