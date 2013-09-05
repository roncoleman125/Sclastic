package scaly.sclastic

import scaly.sclastic.util.Config
import scala.io.Source
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File
import scala.util.Random
import java.awt.Color

object BackScatter {
  val SIZE_X = 500
  val SIZE_Y = 500
  
  def main(args: Array[String]): Unit = {

    val config = Config.loadConfig(args(0))
    
    val bimg =	new BufferedImage(SIZE_X,SIZE_Y,BufferedImage.TYPE_INT_RGB);
    
    val g2d = bimg.createGraphics()
    
    g2d.setColor(new Color(255,255,255));
    
	g2d.fillRect(0,0,SIZE_X,SIZE_Y);
	
    g2d.setColor(Color.BLACK)
    
    val report = config("report")
    
    val ran = new Random
    
    val lines = Source.fromFile(report).getLines
    
    var inscope = 0
    var count = 0
    var summ = 0
    var sumlen = 0
    lines.foreach { line =>
      val fields = line.split("\\s+")
      fields(0) match {
        case "|" =>
          // M
          val m = fields(1).toInt
          summ += m
          val x = m + ran.nextDouble - 0.5
          
          // LOC
          val len = fields(2).toInt
          sumlen += len
          val y = len + ran.nextDouble - 0.5
          
          g2d.fillRect((x*10).toInt-1, (y*10).toInt-1, 2, 2);
          
          count += 1
          
          if(x < SIZE_X && y < SIZE_Y)
            inscope += 1
          
        case _ =>
      }
    }
    
    val cogm = summ / count.toDouble
    val coglen = sumlen / count.toDouble
    
    g2d.setColor(Color.RED)
    g2d.fillRect((cogm*10).toInt-4, (coglen*10).toInt-4, 8, 8);
//    g2d.fillRect(200,50,10,10)
    
    val dir = config("workdir")

    val dimg = horizontalflip(rotate(bimg,180))
    
    val name = config.get("plot").getOrElse("bscat.png")
    
    ImageIO.write(dimg,"png",new File(dir+name));
    
    println("count = "+count)
    println("in range = "+ inscope)
  }
  
  def horizontalflip(img: BufferedImage): BufferedImage = {  
        val w = img.getWidth 
        val h = img.getHeight 
        val dimg = new BufferedImage(w, h, img.getType());  
        val g = dimg.createGraphics();  
        g.drawImage(img, 0, 0, w, h, w, 0, 0, h, null);  
        g.dispose();  
        dimg;  
    }
  
def verticalflip(img: BufferedImage): BufferedImage = {  
        val w = img.getWidth 
        val h = img.getHeight  
        val dimg = new BufferedImage(w, h, img.getColorModel().getTransparency());  
        val g = dimg.createGraphics();  
        g.drawImage(img, 0, 0, w, h, 0, h, w, 0, null);  
        g.dispose();  
        dimg;  
    } 
  
def rotate(img: BufferedImage , angle: Int): BufferedImage = {  
        val w = img.getWidth  
        val h = img.getHeight  
        val dimg = new BufferedImage(w, h, img.getType());  
        val g = dimg.createGraphics();  
        g.rotate(Math.toRadians(angle), w/2, h/2);  
        g.drawImage(img, null, 0, 0);  
        dimg;  
    } 
  
}