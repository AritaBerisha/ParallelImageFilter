package main.java;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParallelFJImageFilter {
    private int[] src;
    private int[] dst;
    private final int width;
    private final int height;

    public ParallelFJImageFilter(int[] src, int[] dst, int w, int h) {
        this.src = src;
        this.dst = dst;
        width = w;
        height = h;
    }

    public void apply(int nThreads, Boolean balancedLoadFilter, Boolean heightAndWidthFilter) throws InterruptedException {
        int nrSteps = 100;
        if (balancedLoadFilter || nThreads <= 2) {
            for (int steps = 0; steps < nrSteps; steps++) {
                applyBalancedLoadFilter(nThreads, heightAndWidthFilter);
            }
        } else {
            for (int steps = 0; steps < nrSteps; steps++) {
                applyUnbalancedLoadFilter(nThreads, heightAndWidthFilter);
            }
        }

    }

    private void applyBalancedLoadFilter(int nThreads, Boolean heightAndWidthFilter) throws InterruptedException {
        try {
            ExecutorService executor = Executors.newFixedThreadPool(nThreads);
            int heightChunk = height / nThreads;

            for (int threadIndex = 0; threadIndex < nThreads; threadIndex++) {
                final int startHeight = threadIndex * heightChunk + 1;
                final int endHeight = (threadIndex == nThreads - 1) ? height - 1 : (threadIndex + 1) * heightChunk + 1;

                executeTask(nThreads, heightAndWidthFilter, executor, startHeight, endHeight);
            }

            executor.shutdown();

            if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                int[] help = src;
                src = dst;
                dst = help;
            } else {
                System.out.println("TIMEOUT: Not all threads with balanced load finished in time!");
            }

        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    private void applyUnbalancedLoadFilter(int nThreads, Boolean heightAndWidthFilter) throws InterruptedException {
        try {
            ExecutorService executor = Executors.newFixedThreadPool(nThreads);
            int chunkHeight = height / nThreads;

            for (int threadIndex = 0; threadIndex < nThreads; threadIndex++) {
                int addedHeight = (threadIndex == 0 ? chunkHeight : 0);
                final int startHeight = threadIndex * chunkHeight + 1;
                final int endHeight = (threadIndex == nThreads - 1) ? height - 1 : (threadIndex + 1) * chunkHeight + addedHeight + 1;

                executeTask(nThreads, heightAndWidthFilter, executor, startHeight, endHeight);

                //   Only the first Thread will have double the load, the others will get a reduced load
                chunkHeight = (threadIndex == 0 ? (height / (nThreads - 1)) : chunkHeight);
            }

            executor.shutdown();

            if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                int[] help = src;
                src = dst;
                dst = help;
            } else {
                System.out.println("TIMEOUT: Not all threads with unbalanced load finished in time!");
            }

        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeTask(int nThreads, Boolean heightAndWidthFilter, ExecutorService executor, int startHeight, int endHeight) {
        if (!heightAndWidthFilter) {
            executor.execute(() -> applyHeightFilter(startHeight, endHeight));
        } else {
            executor.execute(() -> {
                try {
                    applyHeightAndWidthFilter(startHeight, endHeight, nThreads);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void applyHeightFilter(int start, int end) {
        for (int i = start; i < end; i++) {
            filterImage(i, 1, width - 1);
        }
    }

    private void applyHeightAndWidthFilter(int startHeight, int endHeight, int nThreads) throws InterruptedException {
        try (ExecutorService executor = Executors.newFixedThreadPool(nThreads)) {

            for (int i = startHeight; i < endHeight; i++) {

                int widthChunk = width / nThreads;
                for (int nestedThreadIndex = 0; nestedThreadIndex < nThreads; nestedThreadIndex++) {
                    final int startWidth = nestedThreadIndex * widthChunk + 1;
                    final int endWidth = (nestedThreadIndex == nThreads - 1) ? width - 1 : (nestedThreadIndex + 1) * widthChunk + 1;
                    int finalI = i;

                    executor.execute(() -> filterImage(finalI, startWidth, endWidth));
                }
            }

            executor.shutdown();
            if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                System.out.println("TIMEOUT: Not all nested threads finished in time!");
            }
        }
    }

    private void filterImage(int i, int startWidth, int endWidth) {
        for (int j = startWidth; j < endWidth; j++) {
            float rt = 0, gt = 0, bt = 0;
            for (int k = i - 1; k <= i + 1; k++) {

                int index = k * width + j - 1;
                int pixel = src[index];
                rt += (float) ((pixel & 0x00ff0000) >> 16);
                gt += (float) ((pixel & 0x0000ff00) >> 8);
                bt += (float) (pixel & 0x000000ff);

                index = k * width + j;
                pixel = src[index];
                rt += (float) ((pixel & 0x00ff0000) >> 16);
                gt += (float) ((pixel & 0x0000ff00) >> 8);
                bt += (float) (pixel & 0x000000ff);

                index = k * width + j + 1;
                pixel = src[index];
                rt += (float) ((pixel & 0x00ff0000) >> 16);
                gt += (float) ((pixel & 0x0000ff00) >> 8);
                bt += (float) (pixel & 0x000000ff);

            }
            int index = i * width + j;
            int dPixel = (0xff000000) | (((int) rt / 9) << 16) | (((int) gt / 9) << 8) | (((int) bt / 9));
            dst[index] = dPixel;
        }
    }

}