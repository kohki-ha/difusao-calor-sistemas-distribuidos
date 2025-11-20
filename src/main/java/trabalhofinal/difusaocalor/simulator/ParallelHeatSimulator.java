package trabalhofinal.difusaocalor.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementação paralela usando múltiplas threads locais para atualizar blocos
 * de linhas da malha.
 */
public class ParallelHeatSimulator extends AbstractHeatSimulator {

    private final ExecutorService executor;
    private final List<LineRange> ranges;

    public ParallelHeatSimulator(int n, double alpha) {
        this(n, alpha, Runtime.getRuntime().availableProcessors());
    }

    public ParallelHeatSimulator(int n, double alpha, int threadCount) {
        super(n, alpha);
        int workers = Math.max(1, threadCount);
        this.executor = Executors.newFixedThreadPool(workers);
        this.ranges = buildRanges(workers);
    }

    private List<LineRange> buildRanges(int workers) {
        List<LineRange> list = new ArrayList<>();
        int interiorStart = 1;
        int interiorEnd = n - 2;
        if (interiorEnd < interiorStart)
            return list;

        int interior = interiorEnd - interiorStart + 1;
        int maxChunks = Math.min(workers, interior);
        int base = interior / maxChunks;
        int remainder = interior % maxChunks;

        int current = interiorStart;
        for (int i = 0; i < maxChunks; i++) {
            int size = base + (i < remainder ? 1 : 0);
            int start = current;
            int end = start + size - 1;
            list.add(new LineRange(start, end));
            current = end + 1;
        }
        return list;
    }

    @Override
    protected void computeStep() {
        if (ranges.isEmpty())
            return;

        CountDownLatch latch = new CountDownLatch(ranges.size());
        for (LineRange range : ranges) {
            executor.execute(() -> {
                try {
                    computeRange(range.start, range.end);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel computation interrupted", e);
        }
    }

    private void computeRange(int start, int end) {
        double coefX = alpha * dt / (dx * dx);
        double coefY = alpha * dt / (dy * dy);
        for (int i = start; i <= end; i++) {
            for (int j = 1; j < n - 1; j++) {
                double t = T[i][j];
                double tx = T[i + 1][j] - 2 * t + T[i - 1][j];
                double ty = T[i][j + 1] - 2 * t + T[i][j - 1];
                newT[i][j] = t + coefX * tx + coefY * ty;
            }
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private static class LineRange {
        final int start;
        final int end;

        LineRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
