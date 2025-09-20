import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class QuantizationAnalyzer {
    
    private ImageDisplay imageDisplay;
    private String imagePath;
    private BufferedImage originalImage;
    private PrintWriter csvWriter;
    
    public QuantizationAnalyzer(String imagePath) {
        this.imagePath = imagePath;
        this.imageDisplay = new ImageDisplay();
        
        // Load original image for error calculation
        this.originalImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        imageDisplay.readImageRGB(512, 512, imagePath, originalImage);
    }
    
    /**
     * Generate all possible combinations of Q1, Q2, Q3 that sum to N
     */
    private List<int[]> generateCombinations(int N) {
        List<int[]> combinations = new ArrayList<>();
        
        for (int q1 = 1; q1 <= 8 && q1 <= N-2; q1++) {
            for (int q2 = 1; q2 <= 8 && q2 <= N-q1-1; q2++) {
                int q3 = N - q1 - q2;
                if (q3 >= 1 && q3 <= 8) {
                    combinations.add(new int[]{q1, q2, q3});
                }
            }
        }
        
        return combinations;
    }
    
    /**
     * Calculate error metric between original and processed image
     */
    private long calculateError(BufferedImage original, BufferedImage processed) {
        long totalError = 0;
        
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int originalRGB = original.getRGB(x, y);
                int processedRGB = processed.getRGB(x, y);
                
                // Extract RGB components
                int origR = (originalRGB >> 16) & 0xff;
                int origG = (originalRGB >> 8) & 0xff;
                int origB = originalRGB & 0xff;
                
                int procR = (processedRGB >> 16) & 0xff;
                int procG = (processedRGB >> 8) & 0xff;
                int procB = processedRGB & 0xff;
                
                // Calculate absolute differences
                totalError += Math.abs(origR - procR);
                totalError += Math.abs(origG - procG);
                totalError += Math.abs(origB - procB);
            }
        }
        
        return totalError;
    }
    
    /**
     * Save output image for N=4 cases
     */
    private void saveOutputImage(BufferedImage image, String filename) {
        try {
            imageDisplay.saveImage(image, filename);
        } catch (Exception e) {
            System.err.println("Error saving image: " + e.getMessage());
        }
    }
    
    /**
     * Run comprehensive analysis
     */
    public void runAnalysis() {
        try {
            // Create CSV file
            csvWriter = new PrintWriter(new FileWriter("quantization_analysis.csv"));
            
            // Write CSV header
            // csvWriter.println("N,C,M,Q1,Q2,Q3,Error,ImagePath");
            csvWriter.println("N,<C M Q1 Q2 Q3>,Output Image,Error");
            
            // Test N=4, 6, 8
            int[] testValues = {4, 6, 8};
            
            for (int N : testValues) {
                System.out.println("Testing N=" + N + "...");
                
                List<int[]> combinations = generateCombinations(N);
                System.out.println("Generated " + combinations.size() + " combinations for N=" + N);
                
                int combinationCount = 0;
                for (int[] combo : combinations) {
                    int q1 = combo[0], q2 = combo[1], q3 = combo[2];
                    
                    // Test all 4 modes: C=1,M=1 / C=1,M=2 / C=2,M=1 / C=2,M=2
                    for (int C = 1; C <= 2; C++) {
                        for (int M = 1; M <= 2; M++) {
                            try {
                                // Process image with current parameters
                                BufferedImage processedImage = imageDisplay.processImage(imagePath, C, M, q1, q2, q3);
                                
                                // Calculate error
                                long error = calculateError(originalImage, processedImage);
                                
                                // Generate image filename (for N=4 only)
                                String imageFilename = "";
                                if (N == 4) {
                                    imageFilename = String.format("output_N%d_C%d_M%d_%d_%d_%d.png", 
                                                                N, C, M, q1, q2, q3);
                                    saveOutputImage(processedImage, imageFilename);
                                }

                                // Write to CSV (unified format for all N values)
                                String configStr = String.format("<%d %d %d %d %d>", C, M, q1, q2, q3);
                                csvWriter.printf("%d,%s,%s,%d%n", 
                                            N, configStr, imageFilename, error);
                                
                            } catch (Exception e) {
                                System.err.printf("Error processing N=%d, C=%d, M=%d, Q=(%d,%d,%d): %s%n", 
                                                N, C, M, q1, q2, q3, e.getMessage());
                            }
                        }
                    }
                    
                    combinationCount++;
                    if (combinationCount % 10 == 0) {
                        System.out.printf("Completed %d/%d combinations for N=%d%n", 
                                        combinationCount, combinations.size(), N);
                    }
                }
                
                System.out.println("Completed N=" + N + "\n");
            }
            
            csvWriter.close();
            System.out.println("Analysis complete! Results saved to quantization_analysis.csv");
            
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }
    
    /**
     * Analyze results from CSV and generate summary
     */
    public void analyzeResults() {
        try {
            Scanner scanner = new Scanner(new File("quantization_analysis.csv"));
            
            // Skip header
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
            
            // Maps to store best configurations for each case
            Map<String, Result> bestResults = new HashMap<>();
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                
                if (parts.length >= 7) {
                    int N = Integer.parseInt(parts[0]);
                    int C = Integer.parseInt(parts[1]);
                    int M = Integer.parseInt(parts[2]);
                    int Q1 = Integer.parseInt(parts[3]);
                    int Q2 = Integer.parseInt(parts[4]);
                    int Q3 = Integer.parseInt(parts[5]);
                    long error = Long.parseLong(parts[6]);
                    
                    String key = String.format("N%d_C%d_M%d", N, C, M);
                    
                    if (!bestResults.containsKey(key) || bestResults.get(key).error > error) {
                        bestResults.put(key, new Result(N, C, M, Q1, Q2, Q3, error));
                    }
                }
            }
            
            scanner.close();
            
            // Print analysis summary
            System.out.println("\n=== ANALYSIS SUMMARY ===");
            System.out.println("Best configurations for each case:\n");
            
            for (String key : bestResults.keySet()) {
                Result result = bestResults.get(key);
                System.out.printf("%s: Q=(%d,%d,%d), Error=%d%n", 
                                key, result.Q1, result.Q2, result.Q3, result.error);
            }
            
        } catch (FileNotFoundException e) {
            System.err.println("CSV file not found. Run analysis first.");
        }
    }
    
    // Helper class to store results
    private static class Result {
        int N, C, M, Q1, Q2, Q3;
        long error;
        
        Result(int N, int C, int M, int Q1, int Q2, int Q3, long error) {
            this.N = N; this.C = C; this.M = M;
            this.Q1 = Q1; this.Q2 = Q2; this.Q3 = Q3;
            this.error = error;
        }
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java QuantizationAnalyzer <imagePath>");
            return;
        }
        
        String imagePath = args[0];
        QuantizationAnalyzer analyzer = new QuantizationAnalyzer(imagePath);
        
        System.out.println("Starting comprehensive quantization analysis...");
        System.out.println("This may take several minutes...\n");
        
        // Run the analysis
        analyzer.runAnalysis();
        
        // Analyze and summarize results
        analyzer.analyzeResults();
    }
}