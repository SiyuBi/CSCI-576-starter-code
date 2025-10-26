import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

/**
 * CS 576 - Assignment 2: DCT-based Image Compression
 * Implements JPEG-like encoder/decoder with multiple delivery modes
 */
public class ImageDisplay {

	// GUI components
	private JFrame frame;
	private JLabel lbOriginal;
	private JLabel lbDecoded;
	
	// Image data
	private BufferedImage originalImage;
	private BufferedImage decodedImage;
	
	// Image dimensions (fixed for this assignment)
	private static final int WIDTH = 352;
	private static final int HEIGHT = 288;
	private static final int BLOCK_SIZE = 8;
	
	// DCT coefficients storage (quantized)
	// [channel][blockY][blockX][u][v]
	private int[][][][][] dctCoefficients;
	
	// Parameters
	private int quantizationLevel;  // N: 0-7
	private int deliveryMode;       // M: 1=baseline, 2=spectral, 3=successive bit
	private int latency;            // L: milliseconds
	
	/**
	 * Constructor
	 */
	public ImageDisplay() {
		int numBlocksX = WIDTH / BLOCK_SIZE;   // 352/8 = 44
		int numBlocksY = HEIGHT / BLOCK_SIZE;  // 288/8 = 36
		dctCoefficients = new int[3][numBlocksY][numBlocksX][BLOCK_SIZE][BLOCK_SIZE];
	}
	
	// ============================================================
	// PART 1: IMAGE I/O
	// ============================================================
	
	/**
	 * Read RGB image from file
	 * Format: R plane, G plane, B plane (each width*height bytes)
	 */
	private void readImageRGB(String imgPath) {
		try {
			int frameLength = WIDTH * HEIGHT * 3;
			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			byte[] bytes = new byte[frameLength];
			raf.read(bytes);
			raf.close();
			
			originalImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

			int ind = 0;
			for (int y = 0; y < HEIGHT; y++) {
				for (int x = 0; x < WIDTH; x++) {
					int r = bytes[ind] & 0xff;
					int g = bytes[ind + HEIGHT * WIDTH] & 0xff;
					int b = bytes[ind + HEIGHT * WIDTH * 2] & 0xff;
					
					int pix = 0xff000000 | (r << 16) | (g << 8) | b;
					originalImage.setRGB(x, y, pix);
					ind++;
				}
			}
			
			System.out.println("Image loaded: " + imgPath);
			
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + imgPath);
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error reading file: " + imgPath);
			e.printStackTrace();
		}
	}

	// ============================================================
	// PART 2: ENCODER
	// ============================================================
	
	/**
	 * Encode the image using DCT
	 * Steps: Break into blocks -> DCT -> Quantize
	 */
	private void encodeImage() {
		System.out.println("Encoding image...");
		
		// TODO: For each channel (R, G, B)
		//   1. Extract channel data
		//   2. Break into 8x8 blocks
		//   3. Apply DCT to each block
		//   4. Quantize DCT coefficients
		//   5. Store in dctCoefficients array
		
		System.out.println("Encoding complete.");
	}
	
	/**
	 * Apply 2D DCT to an 8x8 block
	 * Input: 8x8 pixel block (spatial domain)
	 * Output: 8x8 DCT coefficients (frequency domain)
	 * 
	 * Formula: F(u,v) = (1/4) * C(u) * C(v) * 
	 *          Σ(x=0 to 7) Σ(y=0 to 7) f(x,y) * cos[(2x+1)uπ/16] * cos[(2y+1)vπ/16]
	 * where C(u) = 1/√2 if u=0, else 1
	 */
	private double[][] applyDCT(int[][] block) {
		double[][] dct = new double[BLOCK_SIZE][BLOCK_SIZE];
		
		// For each DCT coefficient F(u,v)
		for (int u = 0; u < BLOCK_SIZE; u++) {
			for (int v = 0; v < BLOCK_SIZE; v++) {
				double sum = 0.0;
				
				// Sum over all spatial positions (x,y)
				for (int x = 0; x < BLOCK_SIZE; x++) {
					for (int y = 0; y < BLOCK_SIZE; y++) {
						double cosX = Math.cos((2 * x + 1) * u * Math.PI / 16.0);
						double cosY = Math.cos((2 * y + 1) * v * Math.PI / 16.0);
						sum += block[x][y] * cosX * cosY;
					}
				}
				
				// Apply normalization factors
				double cu = (u == 0) ? (1.0 / Math.sqrt(2)) : 1.0;
				double cv = (v == 0) ? (1.0 / Math.sqrt(2)) : 1.0;
				
				dct[u][v] = 0.25 * cu * cv * sum;
			}
		}
		
		return dct;
	}
	
	/**
	 * Quantize DCT coefficients
	 * Formula: F'[u,v] = round(F[u,v] / 2^N)
	 */
	private int quantize(double value) {
		int quantStep = (int) Math.pow(2, quantizationLevel);
		return (int) Math.round(value / quantStep);
	}
	
	// ============================================================
	// PART 3: DECODER
	// ============================================================
	
	/**
	 * Decode based on delivery mode
	 */
	private void decodeImage() {
		System.out.println("Decoding with mode " + deliveryMode + "...");
		
		switch (deliveryMode) {
			case 1:
				decodeBaseline();
						break;
			case 2:
				decodeSpectralSelection();
						break;
			case 3:
				decodeSuccessiveBit();
						break;
					default:
				System.err.println("Invalid delivery mode: " + deliveryMode);
		}
		
		System.out.println("Decoding complete.");
	}
	
	/**
	 * Mode 1: Baseline Sequential Decoding
	 * Decode blocks one by one, left-to-right, top-to-bottom
	 */
	private void decodeBaseline() {
		// TODO: 
		// for each block (row by row, col by col):
		//   1. Dequantize all 64 coefficients
		//   2. Apply IDCT
		//   3. Update decodedImage
		//   4. Refresh display
		//   5. Sleep(latency)
	}
	
	/**
	 * Mode 2: Progressive Decoding - Spectral Selection
	 * First decode DC for all blocks, then add AC1, AC2, ... AC63
	 */
	private void decodeSpectralSelection() {
		// TODO:
		// for coeff_index from 0 to 63:
		//   for each block:
		//     1. Dequantize coefficients [0..coeff_index]
		//     2. Set rest to zero
		//     3. Apply IDCT
		//     4. Update decodedImage
		//   5. Refresh display
		//   6. Sleep(latency)
	}
	
	/**
	 * Mode 3: Progressive Decoding - Successive Bit Approximation
	 * Decode using 1 bit, then 2 bits, ... until all bits
	 */
	private void decodeSuccessiveBit() {
		// TODO:
		// Determine max bits needed
		// for bit_depth from 1 to max_bits:
		//   for each block:
		//     1. Dequantize using only [bit_depth] most significant bits
		//     2. Apply IDCT
		//     3. Update decodedImage
		//   4. Refresh display
		//   5. Sleep(latency)
	}
	
	/**
	 * Dequantize DCT coefficients
	 * Formula: F[u,v] = F'[u,v] * 2^N
	 */
	private double dequantize(int quantizedValue) {
		int quantStep = (int) Math.pow(2, quantizationLevel);
		return quantizedValue * quantStep;
	}
	
	/**
	 * Apply Inverse DCT to recover 8x8 block
	 * Input: 8x8 DCT coefficients (frequency domain)
	 * Output: 8x8 pixel block (spatial domain)
	 * 
	 * Formula: f(x,y) = (1/4) * Σ(u=0 to 7) Σ(v=0 to 7) 
	 *          C(u) * C(v) * F(u,v) * cos[(2x+1)uπ/16] * cos[(2y+1)vπ/16]
	 * where C(u) = 1/√2 if u=0, else 1
	 */
	private int[][] applyIDCT(double[][] dct) {
		int[][] block = new int[BLOCK_SIZE][BLOCK_SIZE];
		
		// For each spatial position (x,y)
		for (int x = 0; x < BLOCK_SIZE; x++) {
			for (int y = 0; y < BLOCK_SIZE; y++) {
				double sum = 0.0;
				
				// Sum over all frequency coefficients (u,v)
				for (int u = 0; u < BLOCK_SIZE; u++) {
					for (int v = 0; v < BLOCK_SIZE; v++) {
						double cu = (u == 0) ? (1.0 / Math.sqrt(2)) : 1.0;
						double cv = (v == 0) ? (1.0 / Math.sqrt(2)) : 1.0;
						double cosX = Math.cos((2 * x + 1) * u * Math.PI / 16.0);
						double cosY = Math.cos((2 * y + 1) * v * Math.PI / 16.0);
						
						sum += cu * cv * dct[u][v] * cosX * cosY;
					}
				}
				
				// Apply normalization and clamp to [0, 255]
				int pixelValue = (int) Math.round(0.25 * sum);
				block[x][y] = Math.max(0, Math.min(255, pixelValue));
			}
		}
		
		return block;
	}
	
	// ============================================================
	// PART 4: GUI & DISPLAY
	// ============================================================
	
	/**
	 * Initialize and show GUI with original and decoded images side-by-side
	 */
	private void initializeGUI() {
		frame = new JFrame("DCT Compression - Original vs Decoded");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		
		// Left: Original image
		lbOriginal = new JLabel(new ImageIcon(originalImage));
		c.gridx = 0;
		c.gridy = 0;
		frame.add(lbOriginal, c);
		
		// Right: Decoded image (initially empty/black)
		decodedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		lbDecoded = new JLabel(new ImageIcon(decodedImage));
		c.gridx = 1;
		c.gridy = 0;
		frame.add(lbDecoded, c);
		
		frame.pack();
		frame.setVisible(true);
	}
	
	/**
	 * Update the decoded image display
	 */
	private void updateDisplay() {
		lbDecoded.setIcon(new ImageIcon(decodedImage));
		lbDecoded.repaint();
	}
	
	/**
	 * Sleep for latency milliseconds
	 */
	private void sleep() {
		if (latency > 0) {
			try {
				Thread.sleep(latency);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	// ============================================================
	// PART 5: MAIN PIPELINE
	// ============================================================
	
	/**
	 * Main processing pipeline
	 */
	public void run(String imagePath, int N, int M, int L) {
		this.quantizationLevel = N;
		this.deliveryMode = M;
		this.latency = L;
		
		System.out.println("=== DCT Compression Pipeline ===");
		System.out.println("Image: " + imagePath);
		System.out.println("Quantization Level (N): " + N);
		System.out.println("Delivery Mode (M): " + M);
		System.out.println("Latency (L): " + L + " ms");
		System.out.println("================================\n");
		
		// Step 1: Read image
		readImageRGB(imagePath);
		
		// Step 2: Initialize GUI
		initializeGUI();
		
		// Step 3: Encode (DCT + Quantization)
		encodeImage();
		
		// Step 4: Decode (Dequantization + IDCT) with specified mode
		decodeImage();
		
		System.out.println("\nProcessing complete!");
	}
	
	// ============================================================
	// MAIN ENTRY POINT
	// ============================================================
	
	public static void main(String[] args) {
		if (args.length != 4) {
			System.out.println("Usage: java ImageDisplay <InputImage> <QuantizationLevel> <DeliveryMode> <Latency>");
			System.out.println("  InputImage: path to .rgb file (352x288)");
			System.out.println("  QuantizationLevel (N): 0-7 (0=no quantization, 7=high compression)");
			System.out.println("  DeliveryMode (M): 1=baseline, 2=spectral selection, 3=successive bit");
			System.out.println("  Latency (L): milliseconds delay between blocks/iterations");
			System.out.println("\nExample: java ImageDisplay Example.rgb 3 1 100");
			return;
		}
		
		String imagePath = args[0];
		int N = Integer.parseInt(args[1]);
		int M = Integer.parseInt(args[2]);
		int L = Integer.parseInt(args[3]);
		
		// Validate parameters
		if (N < 0 || N > 7) {
			System.err.println("Error: QuantizationLevel must be between 0 and 7");
			return;
		}
		if (M < 1 || M > 3) {
			System.err.println("Error: DeliveryMode must be 1, 2, or 3");
			return;
		}
		if (L < 0) {
			System.err.println("Error: Latency must be non-negative");
			return;
		}
		
		// Run the program
		ImageDisplay display = new ImageDisplay();
		display.run(imagePath, N, M, L);
	}
}