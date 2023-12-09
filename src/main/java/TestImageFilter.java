package main.java;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.IIOException;

import javax.imageio.ImageIO;

import java.util.Objects;
import java.util.logging.*;

public class TestImageFilter {

    private static long sequentialTime = 0;
    private static long parallelTime = 0;

    private static final Logger LOGGER = Logger.getLogger("MyLogger");

    public static void main(String[] args) throws Exception {

        BufferedImage image = null;
        String srcFileName = null;
        int maxThreads = 32;

        boolean balancedLoadFilter = Boolean.parseBoolean(args[1].split("=")[1]);
        boolean heightAndWidthFilter = Boolean.parseBoolean(args[2].split("=")[1]);

        try {
            srcFileName = args[0].split("=")[1];
            File srcFile = new File(srcFileName);
            image = ImageIO.read(srcFile);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Usage: java TestAll <image-file>");
            System.exit(1);
        } catch (IIOException e) {
            System.out.println("Error reading image file " + srcFileName + " !");
            System.exit(1);
        }

        int w = image.getWidth();
        int h = image.getHeight();

        printResults(srcFileName, w, h, image, maxThreads, balancedLoadFilter, heightAndWidthFilter);

    }

    private static void printResults(String srcFileName, int w, int h, BufferedImage image, int maxThreads, boolean balancedLoadFilter, boolean heightAndWidthFilter) throws InterruptedException {
        Path path = Paths.get("./src/main/output");

        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                System.err.println("Failed to create directory: " + e.getMessage());
                return;
            }
        }

        String outputFileName = "./src/main/output/out" + srcFileName.charAt(srcFileName.length() - 5) + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            writer.write("Source image: " + srcFileName);
            writer.newLine();
            writer.write("Image size is " + w + "x" + h);
            writer.newLine();
            writer.newLine();
            writer.write("Starting sequential image filter.");
            System.out.println("Starting sequential image filter.");
            applyImageFilter(image, w, h, srcFileName);

            writer.newLine();
            writer.write("Sequential image filter took " + sequentialTime + " milliseconds.");
            writer.newLine();
            writer.write("Output image: Filtered" + srcFileName);
            writer.newLine();
            writer.newLine();
            writer.write("Available processors: " + Runtime.getRuntime().availableProcessors());
            writer.newLine();
            for (int nThreads = 1; nThreads <= maxThreads; nThreads *= 2) {
                writer.newLine();
                writer.write("Starting parallel image filter using " + nThreads + " threads.");

                System.out.println("Starting parallel image filter using " + nThreads + " threads.");
                applyParallelImageFilter(image, w, h, srcFileName, nThreads, balancedLoadFilter, heightAndWidthFilter);
                double speedup = (double) sequentialTime / parallelTime;
                String efficiency = speedup >= (0.7 * nThreads) ? " ok" + " (>= " + (0.7 * nThreads) + ")" : "";

                writer.newLine();
                writer.write("Parallel image filter took " + parallelTime + " milliseconds using " + nThreads + " threads.");
                writer.newLine();
                writer.write("Output image verified successfully!");
                writer.newLine();
                writer.write("Speedup: " + speedup + efficiency);
                writer.newLine();
            }

            writer.newLine();
            writer.write("Output image (parallel filter): " + "ParallelFiltered" + srcFileName.split("/")[srcFileName.split("/").length - 1]);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing to file", e);
        }
    }

    private static void applyImageFilter(BufferedImage image, int w, int h, String srcFileName) throws IOException {
        int[] src = image.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        long startTime = System.currentTimeMillis();
        ImageFilter filter0 = new ImageFilter(src, dst, w, h);
        filter0.apply();
        long endTime = System.currentTimeMillis();

        sequentialTime = endTime - startTime;
        saveFilteredImage(w, h, srcFileName, "sequential", dst);
    }

    private static void applyParallelImageFilter(BufferedImage image, int w, int h, String srcFileName, int nThreads, boolean balancedLoadFilter, boolean heightAndWidthFilter) throws IOException, InterruptedException {
        int[] src = image.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        long startTime = System.currentTimeMillis();
        ParallelFJImageFilter filter1 = new ParallelFJImageFilter(src, dst, w, h);
        filter1.apply(nThreads, balancedLoadFilter, heightAndWidthFilter);
        long endTime = System.currentTimeMillis();

        parallelTime = endTime - startTime;

        saveFilteredImage(w, h, srcFileName, "parallel", dst);
    }

    private static void saveFilteredImage(int w, int h, String srcFileName, String type, int[] dst) throws IOException {
        BufferedImage dstImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        dstImage.setRGB(0, 0, w, h, dst, 0, w);

        String prefix = Objects.equals(type, "parallel") ? "ParallelFiltered" : "Filtered";

        String dstName = "./src/main/output/" + prefix + srcFileName.split("/")[srcFileName.split("/").length - 1];
        File dstFile = new File(dstName);
        ImageIO.write(dstImage, "jpg", dstFile);
    }

}
