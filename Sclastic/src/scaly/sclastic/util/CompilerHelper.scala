package scaly.cyclomatic.util

import scaly.sclastic.compiler.MethodsCompiler.Descriptor
import scaly.sclastic.compiler.Parser.parse
import java.io.PrintWriter

object CompilerHelper {
  def report(methods: List[Descriptor]): Unit = {report(new PrintWriter(Console.out),methods)}

  def report(out: PrintWriter, methods: List[Descriptor]): Unit = {
    methods.foreach { method =>
      val cc = method.m
      val len = method.len
      val name = method.name
      val struct = method.struct
      val path = method.path

      out.println("| %2d %3d %18.18s %20.20s %s".format(cc, len, name, struct, path))
    }
    
    out.flush
  }
}