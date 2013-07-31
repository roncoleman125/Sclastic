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
import scala.collection.mutable.HashMap
import scaly.sclastic.util.ParserHelper._
import scaly.sclastic.util.Config

object MethodsCompiler {
  case class Descriptor(path: String, pkg: String, struct: String, name: String, len: Int, m: Int)

  protected case class Block(path: String, pkg: String, struct: String, name: String, start: Int, len: Int, decisions: Int = 0) {
    def decides: Boolean = name == DECISIONS
    
    def amethod: Boolean = !name.startsWith(DEMARK)
    
    def end: Int = start + len - 1
  }

  def compile(parseInfos: List[ParseInfo], path: String, lines: List[String]): List[Descriptor] = {
    val blocks = gatherBlocks(parseInfos, lines, path)
    if(Config.debug)
      (0 until blocks.size).foreach(k => println(k+": "+blocks(k)))

    val methods = describe(blocks)

    methods
  }

  /** Describes the methods by analyzing the blocks */
  private def describe(blocks: List[Block]): List[Descriptor] = {
//    println("BLOCKS")
//    (0 until blocks.size).foreach(k => println(k + ": " + blocks(k)))

    val n = blocks.size

    (0 until n).foldLeft(List[Descriptor]()) { (methods, blkno) =>
      val block = blocks(blkno)

      block.name match {
        case DECISIONS =>
          methods

        case _ =>
          val name = block.name
          val start = block.start
          val end = block.end

          val these = blocks.slice(blkno + 1, n)

          val decisions = these.filter(b => b.decides && b.start >= start)
          
          // Find candidate decisions
          val candidates = decisions.filter { decide =>
            (0 until these.size).find(k => /*these(k).start != decide.start*/ !these(k).decides && decide.start >= these(k).start && decide.end <= these(k).end) match {
              case Some(k) =>
                // If we get to this point, we can rule out this decision
                // because it is within scope of another block forward of this point
                false

              case None =>
                // If we get here there are no candiate blocks ahead containing
                // this decision, although there may be some before this point
                true
            }
          }

//          println("using decisions:")
          val cc = candidates.filter(p => p.start >= start && p.end <= end).foldLeft(0) { (m, decision) =>
//            println(decision)

            m + decision.decisions
          } + 1

          val path = block.path
          val pkg = block.pkg
          val struct = block.struct
          val innerLen = innerLength(blkno,blocks)

          val outer = Config.setting("length.outer")
          
          val len = if(outer != "true") innerLen else block.len
          
          methods ++ List(Descriptor(path, pkg, struct, name, len, cc))
      }
    }
  }
  
  private def innerLength(k: Int,blocks: List[Block]): Int = {
    val base = blocks(k)
    val start = base.start
    val end = base.end
    val blkssiz = blocks.size
    
    // It's necessary to skip "decides" blocks as they are usually contained
    // by other blocks
    val sz = (start to end).foldLeft(0) {(sum, lineno) =>
      (k+1 until blocks.size).find(index => !blocks(index).decides && lineno >= blocks(index).start && lineno <= blocks(index).end) match {
        case Some(index) =>
          sum
          
        case None =>
          sum + 1
      }
    }
    
    sz
  }

  /** Gathers the blocks and line counts */
  private def gatherBlocks(parseInfos: List[ParseInfo], lines: List[String], path: String): List[Block] = {
//    (0 until parseInfos.size).foreach(k => println(k+": "+parseInfos(k)))

    var level = 0

    (0 until parseInfos.size).foldLeft(List[Block]()) { (descriptors, k) =>
      val parseInfo = parseInfos(k)

      // Get the method length line count

      if (parseInfos(k).opening)
        level = parseInfos(k).nesting
        
      else if(parseInfos(k).closing)
        level = parseInfos(k).nesting - 1

      // If this construct is nesting, the nest info is in the next construct
      //      val nested = if(k < parseInfos.size-1) parseInfos(k+1).nesting else 0
      val nested = nesting(k, parseInfos)

      val sz =
        if (k == parseInfos.size - 1)
          lines.size - parseInfos(k).lineno

        else if (nested > 0) {

          val CLOSING = DEMARK + "}" + nested
          val OPENING = DEMARK + "{" + nested

          // Look ahead for the closing brace
          (k until parseInfos.size - 1).find(index => parseInfos(index).name == CLOSING && parseInfos(index + 1).name != OPENING) match {
            case Some(index) =>
              // If we reach this point we have something like this
              // #{1
              // ...
              // #}1
              // #{1
              // ...
              // #}1
              // However we want to reach this last #}1, not the middle ones
              parseInfos(index).lineno - parseInfos(k).lineno + 1

            case None =>
              // If we reach this point we have something like this
              // #{1
              // ..
              // #}1
              // #class
              // #{1
              // ...
              (k until parseInfos.size).find(index => parseInfos(index).name == CLOSING) match {
                case Some(index) =>
                  parseInfos(index).lineno - parseInfos(k).lineno + 1

                case None =>
                  printError("expected nesting end found none", "", parseInfo.lineno)
                  -1 // hushes compiler
              }

          }
        } //      else if(k == parseInfos.size - 1)
        //        lines.size - parseInfos(k).lineno
        else if (parseInfos(k).opening) {
          val CLOSING = DEMARK + "}" + parseInfos(k).nesting

          val len = parseInfos.slice(k, parseInfos.size).find(f => f.name == CLOSING) match {
            case Some(find) =>
              find.lineno - parseInfos(k).lineno + 1

            case None =>
              printError("expected matching " + DEMARK + "} not found", parseInfos(k).name, k)
              -1
          }

          len
        } else if (parseInfos(k).amethod) {
          // Look ahead to 1) the next method, 2) next "}" at this nesting level, or the end
          // and take the smallest value
          val name = parseInfos(k).name

          val CLOSING = DEMARK + "}" + level

          val brace = parseInfos.slice(k + 1, parseInfos.size).find(f => f.name == CLOSING) match {
            case Some(find) =>
              val a = find.lineno
              find.lineno

            case None =>
              Int.MaxValue
          }

          // Find next method line         
          val method = parseInfos.slice(k + 1, parseInfos.size).find(f => !f.name.startsWith(DEMARK)) match {
            case Some(find) =>
              val a = find.lineno
              find.lineno

            case None =>
              Int.MaxValue
          }
          
          // Find next artifact line
          // We know this artifact is NOT nested because of the nesting check above!!!
          // but the next artifact had better not be a decision -- not sure why I said this but it seems wrong!
          val artifact = parseInfos.slice(k + 1, parseInfos.size).find(f => f.name.startsWith(DEMARK) /*&& !f.decides*/) match {
            case Some(find) =>
              // NOTE: it is possible the next artifact is on the same line as the method
              val lineno = find.lineno
              
              lineno

            case None =>
              Int.MaxValue
          }

          val end = lines.size

          val possibilities = List(brace, method, artifact, end).sort(_ < _)

          val protolen = possibilities(0) - parseInfos(k).lineno

          val len = if(protolen == 0) protolen+1 else protolen
          
          len
        }
        else
          1

      val fileName = path.split(java.io.File.separator).last

      val name = parseInfo.name match {
        case "#class" | "#trait" | "#object" /*| "#reactions"*/ =>
          // Set the name stepping over the DEMARK
          INIT + parseInfo.name.substring(1)
        case _ =>
          parseInfo.name
      }

      val decisions = if (name == DECISIONS) parseInfo.decisions else 0

      val descriptor = Block(fileName, parseInfo.pkg, parseInfo.struct, name, parseInfo.lineno, sz, decisions)

      if (!name.startsWith(DEMARK) || parseInfo.decides)
        descriptors ++ List(descriptor)
      else
        descriptors
    }
  }

  private def nesting(k: Int, parseInfos: List[ParseInfo]): Int = {
    if(parseInfos(k).decides)
      0
      
    else if(k == parseInfos.size - 1)
      0
      
    else if(k == parseInfos.size - 2)
      parseInfos(k + 1).nesting
      
    else {
      parseInfos(k + 1).name match {
          case name: String if (parseInfos(k + 1).opening) =>
            parseInfos(k + 1).nesting

          case "#try" | "#catch" =>
            parseInfos(k + 2).nesting

          case _ =>
            0
        }
      }
  }

  /** Rolls up the decision count for nested structures */
  private def rollup(start: Int, end: Int, parseInfos: List[ParseInfo]): Int = {
    var level = -1

    // Map: level -> method name
    val methods = HashMap[Int, String]().withDefaultValue("")

    var method = ""

    var methodPlaceHolder = ""

    var levelBase = -1

    var levelTry = 0

    var levelCatch = 0

    var stateTry = false

    var stateCatch = false

    val roll = (start to end).foldLeft(0) { (sum, k) =>
      val defName = parseInfos(k).name

      defName match {
        case "#class" | "#object" | "#trait" | "#reactions" =>
          method = "<init>-" + defName.substring(1)

          sum

        // START nesting
        case name: String if (name.startsWith(OPENING_)) =>
          level = name.substring(2).toInt

          if (stateTry) {
            levelTry += 1
            stateTry = false
          } else if (stateCatch) {
            levelCatch += 1
            stateCatch = false
          }

          methods(level) = method

          // Set the initial base level that we're working on
          if (levelBase == -1)
            levelBase = level

          sum

        // END nesting
        case name: String if (name.startsWith(CLOSING_)) =>
          if (levelTry == 0 && levelCatch == 0) {
            level -= 1

            method = methods(level)
          } else {
            val lv = name.substring(2).toInt

            if (lv == levelTry)
              levelTry -= 1

            else if (lv == levelCatch)
              levelCatch -= 1
          }

          sum

        // START def
        case name: String if (!name.startsWith(DEMARK)) =>
          method = name

          sum

        // DECSIONS indicated
        case DECISIONS =>
          if (method == methods(levelBase))
            sum + parseInfos(k).lineno

          else
            sum

        case "#try" =>
          levelTry += 1

          stateTry = true

          sum

        case "#catch" =>
          levelCatch += 1

          stateCatch = true

          sum

        case _ =>
          sum
      }
    }

    roll
  }
}