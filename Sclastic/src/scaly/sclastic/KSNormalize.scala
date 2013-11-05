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
import java.io.PrintWriter
import java.io.File

/**
 * This class normalizes the M and LOC distributions (output on the Runner console)
 * for input into the K-S test. The first row MUST be the header row. The second
 * row is assume to be bin "1".
 */
object KSNormalize {

  def main(args: Array[String]): Unit = {
    val path = args(0)
    
    val out = new PrintWriter(new File(path+"_"))
    
    val lines = Source.fromFile(path).getLines.toList
    
    (0 until lines.size).foreach { k =>
      k match {
        case 0 =>
          out.println(lines(k))
        case 1 =>
          out.println(lines(k))
        case _ =>         
          val bin = lines(k).split("\\s+")(0).toInt
          
          val priorBin = lines(k-1).split("\\s+")(0).toInt
          
          if(bin-1 == priorBin)
            out.println(lines(k))
          else {
            (priorBin+1 until bin).foreach { b =>
              out.println("%d %5d".format(b,0))
            }
            out.println(lines(k))
          }
      }
    }
    out.flush
    out.close
  }

}