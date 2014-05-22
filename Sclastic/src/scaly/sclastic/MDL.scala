package scaly.sclastic

import scala.io.Source
import scala.collection.mutable.HashMap

/** Program computes the MDL statistics */
object MDL {
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("usage: MDL config-file(s)")
      exit(1)
    }

    // Initialize the number of bins
    val bins = HashMap[String, Int]().withDefaultValue(0)

    // Analyze each configuration
    args.foreach { path =>
      println(path)

      // Get the models and process each one
      val models = Source.fromFile(path).getLines.toList

      models.foreach { model =>
        val fields = model.split("\\s+")

        val name = fields(0)

        name match {
          // Get the number of bins
          case "bins" | "norms" =>
            bins(name) = fields(1).toInt

          // Else we have a model
          case _ =>
            val indx = fields.size - 1

            // R-squared value
            val r2 = fields(indx).toDouble

            // Get the untransformed formula
            val form = fields.slice(0, indx).foldLeft("") { (sum, s) => sum + s }

            // Parse and simplify the formula
            val formt = phi(form)

            // Compute L(H)
            val lofh = formt.size

            // Compute L(D|H)
            val b = bins("bins").toInt

            val lofdh = b / r2

            // Compute MDL
            val mdl = lofh + lofdh

            //println(form + " => " + formt)
            println("\t%4.2f %3d %4d %5.1f %5.1f".format(r2, lofh, b, lofdh, mdl))
        }
      }
    }

  }
  
  /** Transforms the formula to a string */
  def phi(record: String): String = {
//    print(record+" => ")
    var xeSeen = false
    var lseen = false
    
    val trans = record.foldLeft("") { (parsed,c) =>
      c match {
        case c: Char if(c.isDigit || c == '+' || c == '-' || c == '.') =>
          if(xeSeen) {
            xeSeen = false
            parsed + '^' + c

          }
          else
        	  parsed + c

        case c: Char if(c == 'l') =>
          lseen = true
          parsed + '*' + upper(c)
          
        case c: Char if(c == 'x' || c == 'e') =>
          if(lseen) {
            lseen = false
            parsed + upper(c)
          }
          else {
            xeSeen = true
            parsed + '*' + upper(c)
          }
          
        case _ =>
          parsed
      }
    }
    
    val form = simplify(trans)
//    println(form)
    
    form
  }
  
  /** Converts certain tokens to upper case */
  def upper(c: Char) = if(c == 'e' || c == 'l') c.toUpper else c

  /** Simplifies the formula */
  def simplify(form: String): String = {  
    // Get the index of the start of x^1
    val j = Math.max(form.indexOf("x^1+"),form.indexOf("x^1-"))
    
    // If x^1- or x^1+ not found this is not a linear form
    if(j == -1)
      form
    else
      // +1 gets the "x" and +3 steps past "^1"
      form.slice(0,j+1) + form.slice(j+3,form.size)
  }
}