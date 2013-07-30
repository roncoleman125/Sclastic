package scaly.sclastic.compiler

import scaly.sclastic.util.ParserHelper._

case class ParseInfo(pkg: String, struct: String, name: String, lineno: Int, decisions: Int = 0) {
  def opening = name.startsWith(OPENING_)
  
  def closing = name.startsWith(CLOSING_)
  
  def nesting = if(name.startsWith(OPENING_) || name.startsWith(CLOSING_)) name.substring(2).toInt else 0
  
  def amethod = !name.startsWith(DEMARK)
  
  def decides = name.startsWith(DECISIONS)
}