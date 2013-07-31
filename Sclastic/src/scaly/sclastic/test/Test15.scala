package scaly.sclastic.test

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test
import scaly.sclastic.util.ParserHelper._
import scaly.sclastic.compiler.MethodsCompiler.compile
import scaly.sclastic.compiler.Parser.parse
import java.io.PrintWriter
import java.io.File
import scaly.sclastic.util.Config

class Test15 {
  @Test
  def test {
    val config = Config.loadConfig("test-config.txt")
    
    val report = config("report")
    
    val out = new PrintWriter(new File(report))

    val root = config("root")
    
    val list = scaly.sclastic.util.FileWalker.walk(root,".scala")
    list.foreach { file =>
      out.println("--- "+file +" ---")
      val lines = dequote(decomment(file))
      
      val parseInfos = parse(file,lines)
      
      val methods = compile(parseInfos,file,lines)
      (0 until methods.size).foreach { k => out.println(k+"] "+methods(k))}
      
      methods.count(method => method.m > 1) match {
        case 0 =>
          
        case _ =>  
          out.println("{{{{{{{{{{ DETAILS START")
          (0 until lines.size).foreach { k => out.println(k+": "+lines(k))}
          (0 until parseInfos.size).foreach { k => out.println(k+"** "+parseInfos(k))}
          out.println("}}}}}}} DETAILS END")
      }
      
      out.println("\n")

    }
    
    out.flush
    out.close
    
    println("done")
  }
}
