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

import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import scala.io.Source

object Config {
  val settings = HashMap[String,String]().withDefaultValue("")

  def setting(key: String): String = {
    settings.get(key) match {
      case Some(value) =>
        value
        
      case _ =>
      	""
    }
  }
  
  def debug: Boolean = {
    settings.get("debug") match {
      case Some(value) if(value == "true") =>
        true
        
      case _ =>
        false
    }
  }
  
  def lint: Boolean = {
    settings.get("lint") match {
      case Some(value) if(value == "true") =>
        true
        
      case _ =>
        false
    }
  }
  
  def loadConfig(path: String): Map[String, String] = {
    settings("init") match {
      case "true" =>

      case _ =>
        val input = Source.fromFile(path).getLines

        input.foreach { line =>
          val keyval = line.trim.split("\\s+")

          if(!line.startsWith("#") && line.length != 0)
        	  settings(keyval(0)) = keyval(1)
        }

        settings("init") = "true"
    }
    
    loadHofs

    settings
  }

  private val PHOFS = List("find", "filter", "dropWhile", "corresponds", "count", "exists", "filterNot", "indexWhere",
    "lastIndexWhere", "partition", "prefixLength", "segmentLength", "sortWith", "span", "takeWhile", "withFilter",
    "orElse")

  val phofs = HashMap[String, String]()

  def loadHofs: HashMap[String, String] = {
    phofs.size match {
      case 0 =>
        val path = settings("hofdb")

        try {
          val input = Source.fromFile(path).getLines

          input.foreach { line =>
            if(!line.startsWith("#")) {
                val fields = line.split("#")
                
            	phofs(fields(0)) = fields(0)
            }
          }
        } catch {
          case e: Exception =>
            println("WARNING: using default hofs")

            PHOFS.foreach { h =>
              phofs(h) = h
            }
        }
        
      case _ =>

    }

    phofs
  }
}