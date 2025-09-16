
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	BufferedImage imgTwo;

	// Modify the height and width values here to read and display an image with
  	// different dimensions. 
	int width = 512;
	int height = 512;

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);
					ind++;
				}
			}
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void showIms(String[] args){

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);
		
		// Create processed image
		BufferedImage imgTwo = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		// TODO: image processing
		Graphics2D g = imgTwo.createGraphics();
		g.drawImage(imgOne, 0, 0, null);
		g.dispose();

		// Use labels to display both images
		frame = new JFrame("Original vs Processed");
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		// left: original image label
		lbIm1 = new JLabel(new ImageIcon(imgOne));
		// right: processed image label
		JLabel lbIm2 = new JLabel(new ImageIcon(imgTwo));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		
		// left side: original image
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(lbIm1, c);
		
		// right side: processed image
		c.gridx = 1;
		c.gridy = 0;
		frame.getContentPane().add(lbIm2, c);

		frame.pack();
		frame.setVisible(true);
	}
	
	// RGB to YUV conversion
	private double[] rgbToYuv(int r, int g, int b) {
		double y = 0.299 * r + 0.587 * g + 0.114 * b;
		double u = -0.417 * r - 0.289 * g + 0.436 * b;
		double v = 0.615 * r - 0.515 * g - 0.100 * b;
		return new double[]{y, u, v};
	}

	// YUV to RGB conversion  
	private int[] yuvToRgb(double y, double u, double v) {
		int r = (int)(1.000 * y + 0.000 * u + 1.1398 * v);
		int g = (int)(1.000 * y - 0.3946 * u - 0.5806 * v);
		int b = (int)(1.000 * y + 2.0321 * u + 0.000 * v);
		
		// Clamp values to [0, 255]
		r = Math.max(0, Math.min(255, r));
		g = Math.max(0, Math.min(255, g));
		b = Math.max(0, Math.min(255, b));
		
		return new int[]{r, g, b};
	}

	// uniform quantization (M=1)
	private int uniformQuantize(int value, int bits) {
		int levels = (int)Math.pow(2, bits);
		int step = 256 / levels;
		int quantized = (value / step) * step + step/2;
		return Math.min(255, quantized);
	}

	// smart quantization (M=2)
	private int smartQuantize(int value, int bits, int[] histogram) {
		// TODO
		return value; // placeholder
	}

	public static void main(String[] args) {

		if (args.length != 6) {
			System.out.println("Usage: java ImageDisplay <imagePath> <C> <M> <Q1> <Q2> <Q3>");
			return;
		}
		
		String imagePath = args[0];
		int C = Integer.parseInt(args[1]); // 1=RGB, 2=YUV
		int M = Integer.parseInt(args[2]); // 1=uniform, 2=smart
		int Q1 = Integer.parseInt(args[3]);
		int Q2 = Integer.parseInt(args[4]);
		int Q3 = Integer.parseInt(args[5]);
		
		ImageDisplay ren = new ImageDisplay();
		// ren.processImage(imagePath, C, M, Q1, Q2, Q3);
		ren.showIms(args);
	}

}
