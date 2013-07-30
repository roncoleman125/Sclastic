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