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
package scaly.sclastic.compiler

import scaly.sclastic.util.ParserHelper._
import scaly.sclastic.compiler.Simplifier._
import scala.collection.mutable.HashMap
import scaly.sclastic.util.Lint._
import scaly.sclastic.util.Config
import java.io.PrintWriter
import java.io.File
import scaly.sclastic.util.Lint

/** Parses out Scala lexical elements */
object Parser { 
  val decisions = HashMap[Int,Int]().withDefaultValue(0)
  
  var nest = 0

  var struct = ""

  var pkg = ""

  var ovride = false
  
  /** Parses the lines into elementary parse constructs as pass #1 */
  def parse(path: String, lines: List[String]): List[ParseInfo] = {       
    nest = 0
    
    struct = ""
      
    pkg = ""
      
    ovride = false
    
    decisions.clear

    val finds = (0 until lines.size).foldLeft(List[ParseInfo]()) { (db, lineno) =>
      val line = lines(lineno)
      
      val tokensUnfluffed = line.trim.split("\\s+")
      
      val tokens = fluff(line).trim.split("\\s+")
//      
//      def reportHof(name: String) {
//        HOFBOOL.findFirstIn(line) match {
//          case Some(h) =>
//            out.println("+ %s %s %s %d: %s || %s".format(name, pkg, struct, lineno, lines(lineno), path))
//            out.flush
//            
//          case None =>
//        }
//      }      
      
      tokens.size match {
        case 0 =>
          db ++ List(ParseInfo(pkg, struct, "<<NONE", lineno))

        case _ =>            
          head(tokens(0).trim) match {
            
            case "package" if tokens.size >= 2 =>
              parsePkg(line,lineno,db)

            case "class" | "trait" | "object" if tokens.size >= 2 =>
              val name = tokens(1)

              struct = head(name)

              db ++ List(ParseInfo(pkg, struct, DEMARK + tokens(0), lineno)) ++ updateNest(line,lineno)

            case "case" if tokens.size > 2 && (tokens(1) == "class" || tokens(1) == "object") => 
              val name = tokens(2)
              
              struct = head(name)
              
              db ++ List(ParseInfo(pkg, struct, DEMARK + tokens(1), lineno)) ++ updateNest(line,lineno) 
              
            case "abstract" if tokens.size > 2 =>
              // Assume "abstract class" and skip over "class"                
              val name = tokens(2)

              struct = head(name)

              db ++ List(ParseInfo(pkg, struct, DEMARK + tokens(1), lineno)) ++ updateNest(line,lineno) 

            case "sealed" if tokens.size > 2 =>
              tokens(1) match {
                case "class" | "trait" | "object" =>
                  val name = tokens(2)

                  struct = head(name)

                  db ++ List(ParseInfo(pkg, struct, DEMARK + tokens(1), lineno)) ++ updateNest(line,lineno) 

                case "abstract" if tokens.size > 3 =>
                  val name = tokens(3)

                  struct = head(name)

                  db ++ List(ParseInfo(pkg, struct, DEMARK + tokens(2), lineno)) ++ updateNest(line,lineno) 

                case _ =>
                  printError("expected seal class or abstract class", line, lineno)
                  db
              }
            case "override" | "implicit" if tokens.size >= 2 =>
              findDef(tokens,"def") match {
                case k: Int if k != -1 && k < tokens.size-2 =>                 
                  val name = tokens(k+1)
                  if(Config.lint) Lint.reportHof(name, pkg, struct, lineno, line, path)

                  val method = head(name)

                  db ++ List(ParseInfo(pkg, struct, method, lineno)) ++ updateNest(line,lineno) 

//                case "object" if tokens.size >= 3 =>
//                  db ++ List(ParseInfo(pkg, struct, DEMARK + "object", lineno)) ++ updateNest(line,lineno) 
                  
                case _ =>
                  db ++ List(ParseInfo(pkg, struct, DEMARK + tokens(1), lineno)) ++ updateNest(line,lineno) 
              }
            
              
            case "val" | "var" | "lazy" | "reactions" if tokens.size >= 2 =>
              // These often occur inside a block, not outside
              // Something like this: "def foo = val abc = 0" won't even parse, although
              // this "def foo = {val abc = 0}" does.
              db ++ List(ParseInfo(pkg, struct, DEMARK+tokens(0), lineno)) ++ updateNest(line,lineno) 
              
//            case "reactions" if tokens.size >= 2 =>
//              db ++ List(ParseInfo(pkg, struct, tokens(0), lineno)) ++ updateNest(line,lineno) 
              
            case "public" | "protected" | "private" if tokens.size >= 2 =>
                  (0 until tokens.size).find { k =>
                    tokens(k) match {
                      case "class" | "object" | "trait" | "def" =>
                        true
                        
                      case _ =>
                        false
                    }
                  } match {
                    case Some(index) =>
                      val tipe = tokens(index)
                      
                      val name = if(tipe == "def") tokens(index+1) else DEMARK + tipe
                      if(Config.lint) Lint.reportHof(name, pkg, struct, lineno, line, path)
                      
                      db ++ List(ParseInfo(pkg, struct, name, lineno)) ++ updateNest(line,lineno)
                      
                    case None =>
                      val name = DEMARK + tokensUnfluffed(1)
                      
                      db ++ List(ParseInfo(pkg, struct, name, lineno)) ++ updateNest(line,lineno)
                  }
              
            case "def" if tokens.size >= 2 =>
              val name = tokens(1)
              if(Config.lint) Lint.reportHof(name, pkg, struct, lineno, line, path)

              val method = head(name)

              db ++ List(ParseInfo(pkg, struct, method, lineno)) ++ updateNest(line,lineno) 

            case "package" | "trait" | "abstract" | "class" | "override" | "def" =>
              printError("dont know what this is", line, lineno)
              db

            case _ =>                   
//              db ++ List(ParseInfo(pkg, struct,DEMARK+"stt",lineno)) ++ updateNest(line,lineno)
              db ++ updateNest(line,lineno)
          }
      }
    }
//    finds
    
    patch(finds)
  }

  def updateNest(aline: String, lineno: Int): List[ParseInfo] = { 
    val line = fluff(aline)
    if (lone(line, "try")) {
      List(ParseInfo(pkg, struct, DEMARK + "try", lineno))
    } 
    else if (lone(line, "catch")) {
      List(ParseInfo(pkg, struct, DEMARK + "catch", lineno))
    }
    else if(lone(line,"finally")) {
       List(ParseInfo(pkg, struct, DEMARK + "finally", lineno))
    }
    // } {
    else if (endStartBlock(line)) {
      // End block1
//      val count1 = decisions(nest)
      
      nest -= 1

      val endIndex = line.indexOf("}")
      
      val count1 = countDecisions(line.slice(0,endIndex))
      
      val decides1 = if(count1 != 0) List(ParseInfo(pkg, struct, DECISIONS, lineno, count1)) else List()
      
      val block1 = decides1 ++ List(ParseInfo(pkg, struct, CLOSING_ + (nest + 1), lineno))

      // Start block2
      nest += 1
      validate(line,lineno)

      val count2 = countDecisions(line.slice(endIndex,line.length))

      val decides2 = if(count2 != 0) List(ParseInfo(pkg, struct, DECISIONS, lineno, count1)) else List()
      
      val block2 = List(ParseInfo(pkg, struct, OPENING_ + nest, lineno)) ++ decides2

      block1 ++ block2
    }
    
    // try { ...
    else if (startBlockWithTry(line)) {
      nest += 1
      
      val count = countDecisions(line)
      
      val decides = if(count != 0) List(ParseInfo(pkg, struct, DECISIONS, lineno, count)) else List()

      val opening = List(ParseInfo(pkg, struct, DEMARK + "try", lineno)) ++
        List(ParseInfo(pkg, struct, OPENING_ + nest, lineno)) ++
        decides
        
      // try { ... }
      val closing = if(startEndBlock(line)) {
        nest -= 1
        validate(line,lineno)
        
    	 List(ParseInfo(pkg, struct, CLOSING_ + nest, lineno))

      } else List()
      
      opening ++ closing
      
    }
    
    // ... } catch { ...
    else if (endBlockWithCatchBlock(line)) {
      // Close out first block
      val endIndex = line.indexOf("}")
      
      val count1 = countDecisions(line.slice(0,endIndex))

      validate(line,lineno) 
      nest -= 1
      
      val decides1 = if(count1 != 0) List(ParseInfo(pkg, struct, DECISIONS, lineno, count1)) else List()
      
      val block1 = decides1 ++ List(ParseInfo(pkg, struct, CLOSING_ + (nest + 1), lineno))

      // Open new a block
      nest += 1

      val count2 = countDecisions(line.slice(endIndex,line.length))

      val decides2 = if(count2 != 0) List(ParseInfo(pkg, struct, DECISIONS, lineno, count2)) else List()

      val block2 = List(ParseInfo(pkg, struct, DEMARK + "catch", lineno)) ++
      	List(ParseInfo(pkg, struct, OPENING_ + nest, lineno)) ++
      	decides2
        
      block1 ++ block2
    }
    
    // catch { ...
    else if(startBlockWithCatch(line)) {
      nest += 1
      
      val count = countDecisions(line)

      val decides = if(count != 0) List(ParseInfo(pkg, struct, DECISIONS, lineno, count)) else List()

      val opening = List(ParseInfo(pkg, struct, DEMARK + "catch", lineno)) ++
        List(ParseInfo(pkg, struct, OPENING_ + nest, lineno)) ++
        decides
        
      // catch { ... }
      val closing = if(startEndBlock(line)) {
        nest -= 1
        validate(line,lineno)
        
    	  List(ParseInfo(pkg, struct, CLOSING_ + nest, lineno))

      } else List()
      
      opening ++ closing
    }
    
    // { ...
    else if (startBlock(line)) {
      nest += 1
      assert(nest > 0)
      
      val count = countDecisions(line)

      val decides = if(count != 0) List(ParseInfo(pkg, struct, DECISIONS, lineno, count)) else List()

      List(ParseInfo(pkg, struct, OPENING_ + nest, lineno)) ++ decides
    }
   
    // ... } catch
    else if (endBlockWithCatch(line)) { // ...} catch
      val count = countDecisions(line)

      val decides = if(count != 0) List(ParseInfo(pkg, struct, DECISIONS, lineno, count)) else List()
      
      validate(line,lineno)     
      nest -= 1

      decides ++
        List(ParseInfo(pkg, struct, CLOSING_ + (nest + 1), lineno)) ++
        List(ParseInfo(pkg, struct, DEMARK + "catch", lineno))
    }
    // } ...
    else if (endBlock(line)) {
      val count = countDecisions(line)

      val decides = if(count != 0) List(ParseInfo(pkg, struct, DECISIONS, lineno, count)) else List()

      validate(line,lineno)     
      nest -= 1

      decides ++
        List(ParseInfo(pkg, struct, CLOSING_ + (nest + 1), lineno))
       
    }
    // { ... }
    else if(startEndBlock(line)) {
      val count = countDecisions(line)

      val decides = if(count != 0) List(ParseInfo(pkg, struct, DECISIONS, lineno, count)) else List()
      
      // Using nest+1 since the parse info is inside { ... }
      List(ParseInfo(pkg, struct, OPENING_ + (nest+1), lineno)) ++
        decides ++
        	List(ParseInfo(pkg, struct, CLOSING_ + (nest+1), lineno))
    }
    // It's none of the above but we must still count the line!
    else {
      val count = countDecisions(line)

      val decides = if(count != 0) List(ParseInfo(pkg, struct, DECISIONS, lineno, count)) else List()
      
      decides
    }
  }   
  
  /** Counts the decisions on this line */
  def countDecisions(line: String): Int = {
    val tokens = fluff(line).split("\\s+")  
    
    val num = (0 until tokens.size).count { k =>
      val token = head(tokens(k))
      
      token match {
        case "if" | /*"for" |*/ "while" =>
          true
          
        case "&&" | "||" /*| "==" | "!="*/ =>
          true
          
        case "case" if(k < tokens.size-1 && tokens(k+1) != "class") =>
          true
          
//        case            
//        "find" | 
//        "filter" | 
//        "dropWhile" | 
//        "corresponds" | 
//        "count" | 
//        "exists" |
//        "filterNot" |
//        "forall" |
//        "indexWhere" |
//        "lastIndexWhere" |
//        "partition" |
//        "prefixLength" |
//        "segmentLength" |
//        "sortWith" |
//        "span" |
//        "takeWhile" |
//        "withFilter" =>
//          true
          
        case _ =>
          ishof(k,tokens)
//          false
      }
    }
    num
  }

  def parsePkg(line: String, lineno: Int, db: List[ParseInfo]): List[ParseInfo] = {
    val pattern2 = """\s*package\s*\$\{(.+)\}\s*\{""".r
    
    val pattern1 = """\s*package\s*(.+)\s*\{""".r
    
    val pattern0 = """\s*package\s*(.+)""".r

    line match {
      case pattern2(name) =>
        pkg = if (pkg.length != 0) pkg + "." + name else "$" + name
        
        val simplerSpec = "package " + name +  " {"
        
        db ++ List(ParseInfo(pkg, struct, DEMARK + name, lineno)) ++ updateNest(simplerSpec, lineno)
        
      case pattern1(name) =>
        pkg = if (pkg.length != 0) pkg + "." + name else name
        
      	db ++ List(ParseInfo(pkg, struct, DEMARK + "package:" + name, lineno)) ++ updateNest(line, lineno)
      
      case pattern0(name) =>
        pkg = if (pkg.length != 0) pkg + "." + name else name
        
        db
        
      case _ =>
        printError("unexpected package specification",line,lineno)

        db
    }
  }

    
  def ishof(k: Int, tokens: Array[String]): Boolean = {
    val token = tokens(k)
    
    val inscope = Config.phofs.contains(token)

    // token is not even one of the methods
    if(!inscope)
      false
      
    // token at start of line
    else if(k == 0)
      false
    
    // token defines a method
    else if(tokens(k-1) == "def" || tokens(k-1) == "val" || tokens(k-1) == "var")
      false
      
    // token at end of line => it needs a function
    else if(k == tokens.size-1)
      false
      
    // Method without a function object
    else if(k < tokens.size-2 && tokens(k+1) == "(" && tokens(k+2) == ")")
      false
      
   // Method without a function object
   else if(k < tokens.size-1 && (tokens(k+1) == ")" || tokens(k+1) == "."))
     false
    
    // This is PROBABLY an HOF
    else   
      true
  }
  
  def validate(line: String, lineno: Int) {
    nest match {
      case nest if(nest > 0) =>
        
      case _ =>
        val err = "bad nesting on line "+lineno+": '"+line+"'"
        throw new Exception(err)
    }
  }
  
  def findDef(tokens: Array[String],token: String): Int = {
    (0 until tokens.size).find(k => tokens(k) == token) match {
      case Some(k) =>
        k
      case None =>
        -1
    }
  }
}
