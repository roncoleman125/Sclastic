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
import scaly.sclastic.compiler.Parser.parse
import scala.collection.mutable.HashMap
import scaly.sclastic.compiler.Parser
import java.io.PrintWriter
import java.io.File

object Lint {
  def main(args: Array[String]): Unit = {
//    Config.setting("debug") = "true"
    Config.settings("debug") = "true"
      
    // See http://danielwestheide.com/blog/2013/01/23/the-neophytes-guide-to-scala-part-10-staying-dry-with-higher-order-functions.html
    // See http://gleichmann.wordpress.com/2010/11/28/high-higher-higher-order-functions/
    val hofs = List(
        "val even  =  ( x :Int )  =>  x % 2 == 0", // not detected!!!
        "val  filter = ( predicate :Int => Boolean, xs :List[Int] ) ",
        "val odd : Int => Boolean  =  ( x :Int )  =>  x % 2 == 1",
        "type SizeChecker = Int => Boolean",
        "def complement(predicate: A => Boolean) = (a: A) => !predicate(a)",
        "def complement[A](predicate: A => Boolean) = (a: A) => !predicate(a)",
        "def count(p: (A) => Boolean): Int"
        )
    
    hofs.foreach(checkHof(_))
    
    println(Lint.count)
    println(Lint.bcount)

  }
  
  var count = 0
  var attempts = 0
  var bcount = 0

  // See http://www.tutorialspoint.com/scala/scala_regular_expressions.htm
  val RegexType = """\s*type\s*(.+)\s*=.*=>\s*Boolean.*""".r
  
  val RegexVal =  """\s*val\s*(.+)\s*=.*=>\s*Boolean.*""".r
  
  val RegexTypedVal =  """\s*val\s*(.+)\s*\:.*=>\s*Boolean.*""".r
  
  val RegexDef = """\s*def\s+([^\[\(]+)\(.*\s*=>\s*Boolean.*""".r
  
  val RegexGenericDef = """\s*def\s+(.+)\s*\[.*\].*=>\s*Boolean.*""".r
  
  val RegexBoolean = """.*=>\s*Boolean.*""".r

  def clear = count = 0

  def checkHof(line: String) {
    attempts += 1
    
    if(Config.debug) print(line+" >> ")
    val point = line match {
      case RegexType(name) =>
        if(Config.debug) println("type = "+name)
        1
        
      case RegexVal(name) =>
        if(Config.debug) println("val = "+name)
        1
        
      case RegexTypedVal(name) =>
        if(Config.debug) println("typed val = "+name)
        1        

      case RegexDef(name) =>
        if(Config.debug) println("def = "+name)
        1
        
      case RegexGenericDef(name) =>
        if(Config.debug) println("def generic = "+name)
        1

      case _ =>
        if(Config.debug) println("NO MATCH ")
        0
    }
    
    RegexBoolean.findFirstIn(line) match {
      case Some(s) =>
        bcount +=1
      case None =>
    }
//    line match {
//      case RegexBoolean(b) =>
//        bcount += 1
//        
//      case _ =>
//        
//    }
    
    count += point
  }
  
  val hofsReport = Config.settings("workdir") + "report-hofs.txt"
  val outHofs = new PrintWriter(new File(hofsReport))
  
  val HOFBOOL = """.*=>\s*Boolean.*""".r
  
  def reportHof(name: String, pkg: String, struct: String, lineno: Int, line: String, path: String) {
    attempts += 1
    
    HOFBOOL.findFirstIn(line) match {
      case Some(hof) =>
        count += 1
        
        outHofs.println("%s#%s#%s".format(name, line, path))
        outHofs.flush

      case None =>
    }
  }
  
  val importsReport = Config.settings("workdir") + "report-imports.txt"
  val outImports = new PrintWriter(new File(importsReport))
  
  def reportImport(path: String, name: String) {
    outImports.println(name)
    outImports.flush
  }
  
  val pkgsReport = Config.settings("workdir") + "report-pkgs.txt"
  val outPkgs = new PrintWriter(new File(pkgsReport))
  
  def reportPkg(path: String, name: String) {
    outPkgs.println(name)
    outPkgs.flush
  }
        
//  val TYPE = 0
//  val CC = 1
//  val LEN = 2
//  val METHOD = 3
//  val STRUCT = 4
//  val SRC = 5
//  val ZIP = 6
//  val HAZARD_INIT = "init"
//  val HAZARD_DECISION = "decision"
//  def main(args: Array[String]): Unit = {
//    val config = loadConfig(args(0))
//
//    val report = config("report")
//    
//    val dir = config("workdir")  
//    
//    val lines = Source.fromFile(report).getLines
//    
//    var badCC = 0
//    var badLen = 0
//    var methods = 0
//    var hazards = HashMap[String,Int]().withDefaultValue(0)
//
//    lines.foreach { line =>
//      val fields = line.split("\\s+")
//      
//      fields(TYPE) match {
//        case "|" =>
//          methods += 1
//          
//          val cc = fields(CC).toInt
//          
//          val len = fields(LEN).toInt
//          
//          val method = fields(METHOD)
//          
//          // All complexities MUST be greater than or equal to 1
//          if(cc < 1)
//            badCC += 1
//            
//          // All lengths must be greater than 1
//          if(len < 1)
//            badLen += 1
//            
//          // Check if a constructor is longer than "empty" and has some
//          // some complexity. This suggest that the decision may be part
//          // part of the following method and not in the constructor.
//          if(method.startsWith(HAZARD_INIT) && cc > 1 && len > 2)
//            hazards("init") += 1
//            
//          // Check if the method name is one of the conditionals. We may have
//          // parsed the decision incorrectly
//          if(Parser.CONDITIONALS.contains(method))
//            hazards("decision") += 1
//      }
//    }
//  }
  
//  	var count = 0
//	
//	val Pattern1 = """\s*val\s*(.+)\s*=.*=>\s*Boolean.*""".r
//	val Pattern2 = """\s*def\s*(.+)\s*\(.*=>\s*Boolean.*\).*""".r
//	
//	def clear = count = 0
//	
//	def checkhof(line: String) {
//	  line match {
//	    case Pattern1(name) =>    
//          if(Config.debug)
//            println(line+" >> VAL = "+name)
//            
//	      count += 1
//	      
//	    case Pattern2(name) =>	            
//          if(Config.debug)
//            println(line+" >> DEF = "+name)
//            
//	      count += 1
//	      
//	    case _ =>
//	      if(Config.debug)
//	        println(line+" >> NO MATCH")
//	  }
//	}
}