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

object ParserHelper {
  val DEMARK = "#"
  val OPENING_ = DEMARK + "{"
  val CLOSING_ = DEMARK + "}"
  val DECISIONS = DEMARK + "decisions"
  val INIT = "<init>-"
  
  /** De-comments and de-quotes a file to memory */
  def dede(path: String): List[String] = {
    dequote(decomment(path))
  }
  
  /** De-comments and de-quotes an in-memory file to memory */
  def dede(lines: List[String]): List[String] = {
    dequote(decomment(lines))
  }
  
  /** Removes comments from a file and returns the de-commented lines. */
  def decomment(path: String): List[String] = {
    decomment(Source.fromFile(path).getLines.toList)
  }
  
  /** Removes comments from lines */
  def decomment(lines: List[String]): List[String] = {
    var inblock = false

    // Get rid of line comments then filter block comments and empty lines
    dequote(lines).map { line => 
      val indx = line.indexOf("//")
      
      indx match {
        case j: Int if j == -1 =>
        	line
        	
        case _ =>
          line.substring(0,indx)
      }
    }.filter { aline =>
      val line = aline.trim

      if (startBlockComment(line) && !inblock)
        inblock = true

      // Note: This misses block comments in the middle of a statement,
      // rare but possible.
      if (endBlockComment(line) && inblock) {
        inblock = false
        false
      }
      else if (!inblock)
        line.length != 0
      else
        false
    }
  }
    
  /** Removes quotes from lines */
  var inquote = false 
  
  val QUOTE3 = "\"\"\""
  val QUOTE1 = "\""
    
  def dequote(lines: List[String]): List[String] = {
    // First remove the triple quotes
    
    // Assume we're not within a quote that spans multiple lines */
    var _inquote = false

    val lines3 = lines.map { line =>
      val result3 = dequoteLine(line, _inquote, QUOTE3)

      // Update the in-quote state
      _inquote = result3._1

      // De-quoted line
      result3._2
    }
    
    lines3

    // Second: remove the single quotes
    _inquote = false

    val lines1 = lines3.map { line =>
      val result1 = dequoteLine(line, _inquote, QUOTE1)

      // Update the in-quote state
      _inquote = result1._1

      // De-quoted line
      result1._2
    }
    
    lines1
    
//     Replace demarks with the quotes
//    val purged = lines1.map { line =>
//      line.replaceAll(DEMARK, QUOTE1)
//    }
//
//    purged
  }
  
  /** Remove the quotes from a line give an initial in-quote state and quotation mark */
  def dequoteLine(_line: String, _inquote: Boolean, mark: String = QUOTE3): (Boolean, String) = {
    // Get the quote character which we'll replace with a demark
    val q = mark(0).toString
    
    val demark = mark.replaceAll(q, DEMARK)
    
    // Get all the indexes of the mark
    val indexes = indexesOf(_line, mark)

    // If there are no marks on this line, the pieces will be the line.
    // Otherwise, we'll chap the line into pieces along the mark indexes.
    val pieces = if(indexes.size == 0) List(_line) else chop(_line, indexes)
    
    // Set the starting state in case we're moving across lines.
    inquote = _inquote
    
    // Rebuild the line with (or without) the original text depending
    // on the indexes and the in-quote state.
    val line = (0 until pieces.size).foldLeft("") { (line, k) =>
      // If we have a segment with a quotation mark and we're not
      // already in a quote, filter that piece. Else
      if(pieces(k).startsWith(mark)) {
        if(!inquote) {
        	inquote = true
        	
        	line + demark
        }
        // Otherwise, let the piece though
        else {
          inquote = false
          
          line + demark + pieces(k).substring(mark.size)
        }
      }
      
      // If we're not in a quote, let the piece through
      else if(!inquote)
        line + pieces(k)
        
      // Otherwise, we must be inside a quote and we'll filter that piece of the line
      else
        line
      
    }  
    
    (inquote,line)
  }

  /** Get all indexes of a mark */
  def indexesOf(line: String, mark: String, offset: Int = 0): List[Int] = {
    // Get the substring starting from an index
    val s = line.substring(offset)

    // Get the relative index of the mark
    val k = s.indexOf(mark)

    // If there is no mark at this point, we're done
    // Otherwise, compile the absolute index of the mark which is k+offset 
    // Recursively invoke this method with a new offset stepping over the mark.
    if (k == -1)
      List()
    else
      List(k + offset) ++ indexesOf(line, mark, mark.size + offset + k)
  }

  /** Chop the line into a list of string based on indexes of marks
   *  Note: this method assumes indexes is NOT empty. */
  def chop(line: String, indexes: List[Int]): List[String] = {
    val sz = indexes.size

    val pieces = (1 until sz).foldLeft(List[String]()) { (chops, k) =>
      chops ++ List(line.substring(indexes(k - 1), indexes(k)))
    }
    
    val prefix = if(indexes.first != 0) List(line.substring(0,indexes.first)) else List()
    
    // Um...what if there is exactly on piece? This could probably be
    // improved by checking for that condition but this appears to work.
    val suffix = List(line.substring(indexes.last))

    prefix ++ pieces ++ suffix
  }
  
  def head(word: String): String = {
    val k = word.findIndexOf(p => !isValid(p))

    //    println(k+":"+word)

    if (k == -1)
      word
    else
      word.substring(0, k)
  }


  def startBlockComment(line: String): Boolean = line.trim.indexOf("/*") != -1

  def endBlockComment(line: String): Boolean = line.trim.indexOf("*/") != -1

  def startEndBlockComment(line: String): Boolean = startBlockComment(line) && endBlockComment(line)
  
//  def startBlock(line: String): Boolean = line.indexOf("{") != -1
  
//  def endBlock(line: String): Boolean = line.indexOf("}") != -1
  
  /** Test for: { .. */
  def startBlock(line: String): Boolean = {
    val tokens = line.split("\\s+")
    
    val ltIndex = tokenize(tokens,"{")
    
    val rtIndex = tokenize(tokens,"}")
    
    val tryIndex = tokenize(tokens,"try")
    
    ltIndex != -1 && rtIndex == -1 && tryIndex == -1
  }
  
  /** Tests for: ... } */
  def endBlock(line: String): Boolean = {
    val tokens = line.split("\\s+")
    
    val ltIndex = tokenize(tokens,"{")
    
    val rtIndex = tokenize(tokens,"}")
    
    val catchIndex = Math.max(tokenize(tokens,"catch"),tokenize(tokens,"finally"))
    
    rtIndex != -1 && ltIndex == -1 && catchIndex == -1
  }
  
  /** Tests for: ... try { */
  def startBlockWithTry(line: String): Boolean = {
    val tokens = line.split("\\s+")
    
    val ltIndex = tokenize(tokens,"{")
    
    val rtIndex = tokenize(tokens,"}")
    
    val tryIndex = tokenize(tokens,"try")

    if(tryIndex != -1 && ltIndex != -1 && tryIndex < ltIndex)
      true

    else
      false
  }
  
  /** Tests for: catch { ... */
  def startBlockWithCatch(line: String): Boolean = {
    val tokens = line.split("\\s+")
    
    val ltIndex = tokenize(tokens,"{")
    
    val catchIndex = Math.max(tokenize(tokens,"catch"),tokenize(tokens,"finally"))
    
    if(catchIndex != -1 &&
        ltIndex != -1 &&
        catchIndex < ltIndex)
    	true

    else
        false    
  }

  /** Tests for: ... } catch */
  def endBlockWithCatch(line: String): Boolean = { 
    val tokens = line.split("\\s+")
    
    val ltIndex = tokenize(tokens,"{")
    
    val rtIndex = tokenize(tokens,"}")
    
    val catchIndex = Math.max(tokenize(tokens,"catch"),tokenize(tokens,"finally"))

    if(catchIndex != -1 && rtIndex != -1 && rtIndex < catchIndex)
    	true
    else
        false
  }
  
  /** Tests for: ... } catch { ... */
  def endBlockWithCatchBlock(line: String): Boolean = {
    val tokens = line.split("\\s+")
    
    val ltIndex = tokenize(tokens,"{")
    
    val rtIndex = tokenize(tokens,"}")
    
    val catchIndex = Math.max(tokenize(tokens,"catch"),tokenize(tokens,"finally"))
    
    // ... } catch { ...
    if(catchIndex != -1 &&
        ltIndex != -1 &&
        rtIndex != -1 &&
        rtIndex < catchIndex &&
        catchIndex < ltIndex)
    	true
    else
    	false    
  }
  
  /** Test for: } { */
  def endStartBlock(line: String): Boolean = {
    val tokens = line.split("\\s+")
    
    val ltIndex = tokenize(tokens,"{")
    
    val rtIndex = tokenize(tokens,"}")
    
    val tryIndex = tokenize(tokens,"try")
    
    val catchIndex = Math.max(tokenize(tokens,"catch"),tokenize(tokens,"finally"))
    
    ltIndex != -1 && rtIndex != -1 && ltIndex > rtIndex && tryIndex == -1 && catchIndex == -1
  }
  
  /** Tests for: { ... } */
  def startEndBlock(line: String): Boolean = {
    val tokens = line.split("\\s+")
    
    val ltIndex = tokenize(tokens,"{")
    
    val rtIndex = tokenize(tokens,"}")
    
    ltIndex != -1 && rtIndex != -1 && ltIndex < rtIndex    
  }
  
  def fluff(line: String): String = {
    line.foldLeft("") { (s, c) =>
      c match {
        case '{' | '}' | '.' | '(' | ')' | '['  | ']' =>
          s + " " + c.toString + " "
        
        case _ =>
          s + c.toString
      }
    }.trim.replaceAll("\\s\\s"," ")
  }
  
  def tokenize(tokens: Array[String],token: String): Int = {
    (0 until tokens.size).find(index => tokens(index) == token) match {
      case Some(index) =>
        index
        
      case None =>
        -1
    }
  }
  
  def lone(line: String,token: String): Boolean = {
    val tokens = line.trim.split("\\+")
    
    tokens.size > 0 && tokens.last == token
  }
  
  def isValid(c: Char): Boolean = {
    c.isLetterOrDigit || c == '_' || c == '$'
  }

  def printError(msg: String, line: String, lineno: Int): Unit = {
    throw new Exception("opps! >>> " + msg + " >>> on line " + lineno + ": '" + line + "'")
  } 
  

}