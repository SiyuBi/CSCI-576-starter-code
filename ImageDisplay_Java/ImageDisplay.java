
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

	public void showIms(String[] args, int C, int M, int Q1, int Q2, int Q3){
		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);
		
		// Process the image using your quantization method
		BufferedImage imgTwo = processImage(args[0], C, M, Q1, Q2, Q3);

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

	private double uniformQuantizeDouble(double value, int bits, double min, double max) {
		int levels = (int)Math.pow(2, bits);
		double range = max - min;
		double step = range / levels;
		
		// clamp value to [min, max]
		double normalized = value - min;
		int regionIndex = (int)(normalized / step);
		
		if (regionIndex >= levels) regionIndex = levels - 1;
		if (regionIndex < 0) regionIndex = 0;
		
		// representative value: midpoint of the quantization interval
		double representative = regionIndex * step + step / 2.0 + min;
		
		return Math.max(min, Math.min(max, representative));
	}

	// Calculate histogram for RGB channels
	private int[] calculateHistogramRGB(BufferedImage img, int channel) {
		int[] histogram = new int[256];
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb = img.getRGB(x, y);
				int value = 0;
				switch(channel) {
					case 0: value = (rgb >> 16) & 0xff; break; // R channel
					case 1: value = (rgb >> 8) & 0xff; break;  // G channel
					case 2: value = rgb & 0xff; break;         // B channel
				}
				histogram[value]++;
			}
		}
		return histogram;
	}

	// Calculate histogram for YUV channels
	private int[] calculateHistogramYUV(BufferedImage img, int channel) {
		int[] histogram = new int[256];
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb = img.getRGB(x, y);
				int r = (rgb >> 16) & 0xff;
				int g = (rgb >> 8) & 0xff;
				int b = rgb & 0xff;
				
				double[] yuv = rgbToYuv(r, g, b);
				int value;
				
				switch(channel) {
					case 0: // Y channel [0,255]
						value = (int)Math.round(yuv[0]);
						break;
					case 1: // U channel [-128,127] -> map to [0,255]
						value = (int)Math.round(yuv[1] + 128);
						break;
					case 2: // V channel [-128,127] -> map to [0,255] 
						value = (int)Math.round(yuv[2] + 128);
						break;
					default:
						value = 0;
				}
				
				value = Math.max(0, Math.min(255, value));
				histogram[value]++;
			}
		}
		return histogram;
	}

	// Smart quantization for RGB channels
	private int smartQuantizeRGB(int value, int bits, int[] histogram) {
		int[] boundaries = calculateOptimalBoundaries(histogram, bits);
		
		// Find which region the value belongs to
		for (int i = 0; i < boundaries.length - 1; i++) {
			if (value >= boundaries[i] && value < boundaries[i + 1]) {
				return calculateRepresentative(boundaries[i], boundaries[i + 1], histogram);
			}
		}
		
		// Handle boundary case
		return calculateRepresentative(boundaries[boundaries.length-2], boundaries[boundaries.length-1], histogram);
	}

	// Smart quantization for YUV channels with custom range
	private double smartQuantizeYUV(double value, int bits, int[] histogram, double minVal, double maxVal) {
		int[] boundaries = calculateOptimalBoundaries(histogram, bits);
		
		// Convert boundaries to actual YUV range
		double[] realBoundaries = new double[boundaries.length];
		for (int i = 0; i < boundaries.length; i++) {
			realBoundaries[i] = mapToRange(boundaries[i], 0, 255, minVal, maxVal);
		}
		
		// Find corresponding region
		for (int i = 0; i < realBoundaries.length - 1; i++) {
			if (value >= realBoundaries[i] && value < realBoundaries[i + 1]) {
				return (realBoundaries[i] + realBoundaries[i + 1]) / 2.0;
			}
		}
		return (realBoundaries[realBoundaries.length-2] + realBoundaries[realBoundaries.length-1]) / 2.0;
	}

	// Calculate optimal boundaries using equal probability method
	private int[] calculateOptimalBoundaries(int[] histogram, int bits) {
		int levels = (int)Math.pow(2, bits);
		int[] boundaries = new int[levels + 1];
		int[] cdf = calculateCDF(histogram);
		int totalPixels = cdf[255];
		
		boundaries[0] = 0;
		boundaries[levels] = 255;
		
		// Pixels per region for equal probability
		int pixelsPerRegion = totalPixels / levels;
		
		int currentLevel = 1;
		for (int i = 1; i < 256 && currentLevel < levels; i++) {
			if (cdf[i] >= currentLevel * pixelsPerRegion) {
				boundaries[currentLevel] = i;
				currentLevel++;
			}
		}
		
		return boundaries;
	}

	// Calculate cumulative distribution function
	private int[] calculateCDF(int[] histogram) {
		int[] cdf = new int[256];
		cdf[0] = histogram[0];
		
		for (int i = 1; i < 256; i++) {
			cdf[i] = cdf[i-1] + histogram[i];
		}
		return cdf;
	}

	// Calculate weighted representative value for a region
	private int calculateRepresentative(int start, int end, int[] histogram) {
		long weightedSum = 0;
		long totalWeight = 0;
		
		for (int i = start; i <= end && i < 256; i++) {
			weightedSum += i * histogram[i];
			totalWeight += histogram[i];
		}
		
		if (totalWeight == 0) return (start + end) / 2;
		return (int)(weightedSum / totalWeight);
	}

	// Map value from one range to another
	private double mapToRange(double value, double fromMin, double fromMax, double toMin, double toMax) {
		return (value - fromMin) / (fromMax - fromMin) * (toMax - toMin) + toMin;
	}

	private BufferedImage processImage(String imagePath, int C, int M, int Q1, int Q2, int Q3) {
		// Read original image
		BufferedImage original = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, imagePath, original);
		
		// Create processed image
		BufferedImage processed = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		// Pre-calculate histograms if smart quantization is needed
		int[] histR = null, histG = null, histB = null;
		int[] histY = null, histU = null, histV = null;
		
		if (M == 2) { // Smart quantization requires histogram analysis
			if (C == 1) { // RGB space
				histR = calculateHistogramRGB(original, 0); // R channel
				histG = calculateHistogramRGB(original, 1); // G channel  
				histB = calculateHistogramRGB(original, 2); // B channel
			} else { // YUV space
				// Convert entire image to YUV first, then build histograms
				histY = calculateHistogramYUV(original, 0); // Y channel
				histU = calculateHistogramYUV(original, 1); // U channel
				histV = calculateHistogramYUV(original, 2); // V channel
			}
		}
		
		// Process each pixel
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb = original.getRGB(x, y);
				int r = (rgb >> 16) & 0xff;
				int g = (rgb >> 8) & 0xff;
				int b = rgb & 0xff;
				
				if (C == 1) { // RGB space quantization
					if (M == 1) { // Uniform quantization
						r = uniformQuantize(r, Q1);
						g = uniformQuantize(g, Q2);
						b = uniformQuantize(b, Q3);
					}
					else if (M == 2) { // Smart quantization
						r = smartQuantizeRGB(r, Q1, histR);
						g = smartQuantizeRGB(g, Q2, histG);
						b = smartQuantizeRGB(b, Q3, histB);
					}
				} else { // C == 2, YUV space quantization
					// Convert RGB to YUV
					double[] yuv = rgbToYuv(r, g, b);
					
					if (M == 1) { // Uniform quantization
						// Handle possibly negative U, V values
						double yQuant = uniformQuantizeDouble(yuv[0], Q1, 0, 255);
						double uQuant = uniformQuantizeDouble(yuv[1], Q2, -128, 127);
						double vQuant = uniformQuantizeDouble(yuv[2], Q3, -128, 127);
						
						// Convert back to RGB
						int[] rgbArr = yuvToRgb(yQuant, uQuant, vQuant);
						r = rgbArr[0];
						g = rgbArr[1];
						b = rgbArr[2];
					}
					else if (M == 2) { // Smart quantization
						double yQuant = smartQuantizeYUV(yuv[0], Q1, histY, 0, 255);    // Y: [0,255]
						double uQuant = smartQuantizeYUV(yuv[1], Q2, histU, -128, 127); // U: [-128,127]
						double vQuant = smartQuantizeYUV(yuv[2], Q3, histV, -128, 127); // V: [-128,127]
						
						// Convert back to RGB
						int[] rgbArr = yuvToRgb(yQuant, uQuant, vQuant);
						r = rgbArr[0];
						g = rgbArr[1];
						b = rgbArr[2];
					}
				}
				
				int newRgb = (r << 16) | (g << 8) | b;
				processed.setRGB(x, y, newRgb);
			}
		}
		
		return processed;
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
		ren.showIms(args, C, M, Q1, Q2, Q3);
	}

}
