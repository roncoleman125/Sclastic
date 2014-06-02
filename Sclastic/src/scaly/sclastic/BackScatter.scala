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

import scaly.sclastic.util.Config
import scala.io.Source
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File
import scala.util.Random
import java.awt.Color
import java.awt.Font

/**
 * This object reads the main configuration report file and generates a
 * corresponding "backscatter" PNG image file.
 * @author Ron Coleman
 */
object BackScatter {
  val SIZE_X = 250
  val SIZE_Y = 250
  val BORDER_X = 0
  val BORDER_Y = 0
  
  def main(args: Array[String]): Unit = {

    val config = Config.loadConfig(args(0))
    
    val bimg =	new BufferedImage(SIZE_X+BORDER_X,SIZE_Y+BORDER_Y,BufferedImage.TYPE_INT_RGB);
    
    val g2d = bimg.createGraphics()
    
    g2d.setColor(new Color(255,255,255));
    
	g2d.fillRect(0,0,SIZE_X,SIZE_Y);
	
    g2d.setColor(Color.BLACK)
    
    println("loading data from report...")
    
    val report = config("report")
    
    val ran = new Random
    
    val lines = Source.fromFile(report).getLines
    
    var inscope = 0
    var count = 0
    var summ = 0
    var sumlen = 0
    
    println("analyzing...")
    
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
          val y = len + ran.nextDouble - 0.5 + BORDER_Y
          
          g2d.fillRect((x*5).toInt-1 + BORDER_X, (y*5).toInt-1 + BORDER_Y, 2, 2);
          
          count += 1
          
          if(x < SIZE_X && y < SIZE_Y)
            inscope += 1
          
        case _ =>
      }
      
    }
          
//    val font = new Font("Arial", Font.PLAIN, 10);
//    g2d.setFont(font)
//    g2d.drawString("25", SIZE_X/2, SIZE_Y/2)
    
//    val cogm = summ / count.toDouble
//    val coglen = sumlen / count.toDouble
    
//    g2d.setColor(Color.RED)
//    g2d.fillRect((cogm*10).toInt-4, (coglen*10).toInt-4, 8, 8);
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