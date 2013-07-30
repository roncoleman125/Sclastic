package scaly.sclastic.util

import java.io.File

// See http://stackoverflow.com/questions/2056221/recursively-list-files-in-java
object FileWalker {
  def walk(path: String, suffix: String): List[String] = {
    
    val root = new File(path)
    
    val list = root.listFiles();
    
    list.foldLeft(List[String]()) { (accum, file) =>
      if(file.isDirectory())
        accum ++ walk(file.getAbsolutePath(),suffix)
        
      else if(file.toString().endsWith(suffix))
        accum ++ List(file.getAbsolutePath())
        
      else
        accum
    }
  }

}