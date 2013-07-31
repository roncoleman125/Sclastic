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

object Simplifier { 
  case class Splice(start: Int, end: Int, decisions: Int)

  /** Collapse try-catch blocks */
  def patch(parseInfos: List[ParseInfo]): List[ParseInfo] = {
//    println(this)
//    (0 until parseInfos.size).foreach(k => println(k+": "+parseInfos(k)))
    // First pass: find the try-catch blocks
    val splices = splice(parseInfos)
//    splices.foreach(println(_))

    splices.size match {
      case 0 =>
        // If there are no try-catch blocks, we're done
        parseInfos
        
      case _ =>
        // Second pass: edit the parse info with the splices
        val edited = edit(parseInfos,splices)
        
        edited
    }

  }
  
  /** Edit the try-catch elements with the splice data */
  def edit(parseInfos: List[ParseInfo], splices: List[Splice]): List[ParseInfo] = {
//    (0 until splices.size).foreach(k => println(k+": "+splices(k)))
    
    val sz = parseInfos.size
    
    val rest = if(sz > 0 && splices.last.end < sz-1) parseInfos.slice(splices.last.end+1,sz) else List()
    
    val edited = (0 until splices.size).foldLeft(List[ParseInfo]()) { (list, k) =>
      val start = splices(k).start
      
      val end = splices(k).end
      
      val decides = splices(k).decisions
      
      val rangeStart = if(k == 0) 0 else splices(k-1).end+1
      
      val rangeEnd = start - 1
      
      val keeps = (rangeStart to rangeEnd).foldLeft(List[ParseInfo]()) { (keeping, j) =>
        keeping ++ List(parseInfos(j))
      }
      
      val pkg = parseInfos(start).pkg
      
      val struct = parseInfos(start).struct

      val nest = parseInfos(start+1).name.substring(2).toInt
      
      val name = DEMARK + "{" + nest
      
      val lineno = parseInfos(start).lineno
      
      val opening = ParseInfo(pkg,struct,name,lineno)
      
      val decision = ParseInfo(pkg,struct,DECISIONS,lineno,decides)
      
      val closing = ParseInfo(pkg,struct,DEMARK+"}"+nest,parseInfos(end).lineno)
      
      list ++ keeps ++ List(opening) ++ List(decision) ++ List(closing)
    }  
    
    val editedsz = edited.size
    val restsz = rest.size
    val result = edited ++ rest
    
//    (0 until result.size).foreach(k => println(k+">>: "+result(k)))
    
    result
  }
  
  /** Find try-catch splices in the parsing info */
  def splice(parseInfos: List[ParseInfo]): List[Splice] = {
//    (0 until parseInfos.size).foreach(k => println(k+">: "+parseInfos(k)))
    
    (0 until parseInfos.size).foldLeft(List[Splice]( )) { (splices, k) =>
      parseInfos(k).name match {   
        // Get the try but to avoid double counting, only consider non-nested trys
        case "#try" if splices.length == 0 || k > splices.last.end =>         
          // If we get here, we have a non-nested try
          val lv = parseInfos(k+1).nesting
          
          val CLOSING = DEMARK + "}" + lv
          
          (k until parseInfos.size).find(index1 => parseInfos(index1).name == CLOSING) match {
            case Some(index1) =>
              // Founds the end of the try block, if we get here

              (index1+1 until parseInfos.size-1).find(index2 => parseInfos(index2).name == CLOSING) match {
                case Some(index2) =>
                  // Found the end of the catch block, if we get here
                  
                  // Fold all the decisions within the try-catch
                  val totalDecisions = (k to index2).foldLeft(0) { (sum, l) =>
                    parseInfos(l).name match {
                      case DECISIONS =>
                        val decisions = parseInfos(l).decisions

                        sum + decisions
                      case _ =>
                        sum
                    }
                  }
                  // Add this splice info to the splices
                  splices ++ List(Splice(k,index2,totalDecisions))
                case None =>
                  splices
              }
            case None =>
              splices
          } 
        case _ =>
          splices
      }
    }   
  }
}