import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.lang.Math;
import java.util.Arrays;

public class MoravecDetector {

   public static void main(String[] args) throws IOException 
   {

      BufferedImage Image = ImageIO.read(MoravecDetector.class.getResource("64X64.jpg"));
      
         double[][] result     = convertTo2DWithoutUsingGetRGB(Image);
         long startTime        = System.nanoTime();  
         double[][] cornerness = moravacAlgorithm(result,64,64,10000);         
         long endTime          = System.nanoTime();
         
         String s = toString(cornerness);
         System.out.println(s);
         System.out.println(String.format(toString(endTime - startTime)));
         
         displayCorner(cornerness, Image, 64, 64);
   }

   
   //-- IMAGE SCANNING METHODS--//
   //-- convertTo2dWitoutUsingRGB -->
   
   //-- produces a 2D array represented all pixels of an input image. --//
   //-- pixel(x,y) represented as int[x][y] result --//
   //-- each pixel contains an RGB 32-bit value where 1-8:=>Alpha 9-16:=>Red 17-24:=>Green 25-32:=>Blue
   private static double[][] convertTo2DWithoutUsingGetRGB(BufferedImage image) {

      final byte[] pixels 			= ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
      final int width 	 			= image.getWidth();
      final int height 	  		    = image.getHeight();
      final boolean hasAlphaChannel = image.getAlphaRaster() != null;

      double[][] result = new double[height][width];
      if (hasAlphaChannel) {
         final int pixelLength = 4;
         for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
            int argb = 0;
            argb += (((int) pixels[pixel] 	  & 0xff) << 24); // alpha
            argb +=  ((int) pixels[pixel + 1] & 0xff); // blue
            argb += (((int) pixels[pixel + 2] & 0xff) << 8); // green
            argb += (((int) pixels[pixel + 3] & 0xff) << 16); // red
            result[row][col] = argb;
            col++;
            if (col == width) {
               col = 0;
               row++;
            }
         }
      } else {
         final int pixelLength = 3;
         for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
            int argb = 0;
            argb += -16777216; // 255 alpha
            argb += ((int) pixels[pixel] & 0xff); // blue
            argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
            argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
            
          //-- Get gray-scale intensity --//
            int red = (argb >> 16) & 0x000000FF;
		    int green = (argb>>8 ) & 0x000000FF;
			int blue = (argb) & 0x000000FF;
			double intensity = .333*(red) + .333*(green) + .333*(blue);
            
            result[row][col] = intensity;
            col++;
            if (col == width) {
               col = 0;
               row++;
            }
         }
      }

      return result;
   }
   
   //-- Moravec Algorithm -->
   //-- We assume a patch size of 3x3  --//
   
   private static double[][] moravacAlgorithm(double[][] imageData, int imgWidth, int imgLength, int threshold) 
   {
	   double     cornerness[][] = new double[imgWidth][imgLength];
	   double 	  intenseVar[][] = new double[imgWidth][imgLength];
	   double old_intenseVar[][] = new double[imgWidth][imgLength];
	   
	   double cornerVal     = 0;
	   double min_cornerVal = 999999999;
	   
	   
	   //-- For each pixel (x, y) in the image calculate the intensity variation from a shift (u, v) as: --//
	   for (int x = 3; x <= imgWidth-3; x += 1)
	   {
		   for (int y = 3; y <= imgLength-3; y += 1)
		   {		   	  	   
			   for ( int u = -1; u <= 1; u += 1)
			   {
				   for ( int v = -1; v <= 1; v += 1)	   
				   {
					   if (v==0 && u == 0)
					   {}
					   else
					   {
					   	old_intenseVar[x][y] =  intenseVar[x][y];
					   		intenseVar[x][y] =  Math.pow(imageData[x+u][y+v]-imageData[x][y],2.0);
					   		intenseVar[x][y] =  intenseVar[x][y] + old_intenseVar[x][y];
					   }
				   }
			   }		   		   
		   }
	   }	
	   
	   for (int x = 3; x <= imgWidth-3; x += 1)
	   {
		   for (int y = 3; y <= imgLength-3; y += 1)
		   {		   
			   for ( int u = -1; u <= 1; u += 1)
			   {
				   for ( int v = -1; v <= 1; v += 1)	   
				   {  		
					   		cornerVal = intenseVar[x+u][y+v];
					   		if(cornerVal < min_cornerVal)
					   		{
					   			min_cornerVal = cornerVal;
					   		}				   		
				   }
				   if ( min_cornerVal < threshold )
				   {
					    min_cornerVal = 0;
				   }
				   cornerness[x][y] = min_cornerVal;
				   min_cornerVal    = 999999999;
			   }
		   }
	   }
	   return cornerness;   
   }
   
   
   
   //-- UTLITY METHODS --//
   private static String toString(long nanoSecs) {
      int minutes    = (int) (nanoSecs / 60000000000.0);
      int seconds    = (int) (nanoSecs / 1000000000.0)  - (minutes * 60);
      int millisecs  = (int) ( ((nanoSecs / 1000000000.0) - (seconds + minutes * 60)) * 1000);


      if (minutes == 0 && seconds == 0)
         return millisecs + "ms";
      else if (minutes == 0 && millisecs == 0)
         return seconds + "s";
      else if (seconds == 0 && millisecs == 0)
         return minutes + "min";
      else if (minutes == 0)
         return seconds + "s " + millisecs + "ms";
      else if (seconds == 0)
         return minutes + "min " + millisecs + "ms";
      else if (millisecs == 0)
         return minutes + "min " + seconds + "s";

      return minutes + "min " + seconds + "s " + millisecs + "ms";
   }
   
   public static String toString(double[][] M) {
	    String separator = ", ";
	    StringBuffer result = new StringBuffer();

	    // iterate over the first dimension
	    for (int i = 0; i < M.length; i++) {
	        // iterate over the second dimension
	        for(int j = 0; j < M[i].length; j++){
	            result.append(M[i][j]);
	            result.append(separator);
	        }
	        // remove the last separator
	        result.setLength(result.length() - separator.length());
	        // add a line break.
	        result.append("\n");
	    }
	    return result.toString();
   }
   
   private static void displayCorner(double[][] imageData, BufferedImage image, int imgWidth, int imgLength) throws IOException
   {
	   int pixelValue = 0xFF0000;
	   for (int x = 0; x <= imgWidth-1; x += 1)
	   {
		   for (int y = 0; y <= imgLength-1; y += 1)
		   {		   
			   if(imageData[x][y] > 0)
			   {
				   image.setRGB( y, x, pixelValue);
			   }			
		   }
	   }
	   ImageIO.write(image, "jpg", new File("C:\\Users\\Vanya\\desktop\\corners.jpg"));
   }
   
}