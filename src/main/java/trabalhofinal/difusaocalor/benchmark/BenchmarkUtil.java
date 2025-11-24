package trabalhofinal.difusaocalor.benchmark;

import trabalhofinal.difusaocalor.simulator.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utilitário para executar benchmarks de forma programática e retornar
 * estatísticas.
 */
public class BenchmarkUtil {

    public static class Stats {
        public final List<Double> runs;
        public final double mean;
        public final double median;
        public final double sd;

        public Stats(List<Double> runs) {
            this.runs = Collections.unmodifiableList(new ArrayList<>(runs));
            this.mean = calculateMean(runs);
            this.median = calculateMedian(runs);
            this.sd = calculateStdDev(runs, this.mean);
        }
    }

    public static Stats runSequential(int n, double alpha, int steps, int repeats) {
        List<Double> times = new ArrayList<>();
        
        for (int r = 0; r < repeats; r++) {
            SequentialHeatSimulator sim = new SequentialHeatSimulator(n, alpha);
            
            // Mede APENAS a execução real, UMA ÚNICA VEZ
            long start = System.nanoTime();
            for (int step = 0; step < steps; step++) {
                sim.step();
            }
            long end = System.nanoTime();
            
            double seconds = (end - start) / 1_000_000_000.0;
            times.add(seconds);
        }
        
        return new Stats(times);
    }

    public static Stats runParallel(int n, double alpha, int steps, int repeats, int threadCount) {
        List<Double> times = new ArrayList<>();
        int workers = threadCount <= 0 ? Runtime.getRuntime().availableProcessors() : threadCount;
        
        for (int r = 0; r < repeats; r++) {
            ParallelHeatSimulator sim = new ParallelHeatSimulator(n, alpha, workers);
            
            // Mede APENAS a execução real, UMA ÚNICA VEZ
            long start = System.nanoTime();
            for (int step = 0; step < steps; step++) {
                sim.step();
            }
            long end = System.nanoTime();
            
            double seconds = (end - start) / 1_000_000_000.0;
            times.add(seconds);
            
            sim.shutdown();
        }
        
        return new Stats(times);
    }

    public static Stats runDistributed(int n, double alpha, int steps, int repeats, List<String> workerUrls) {
        List<Double> times = new ArrayList<>();
        
        if (workerUrls == null || workerUrls.isEmpty()) {
            System.err.println("Aviso: nenhuma URL de worker fornecida para modo distribuído");
            return new Stats(times);
        }

        for (int r = 0; r < repeats; r++) {
            try {
                DistributedHeatSimulator sim = new DistributedHeatSimulator(n, alpha, workerUrls);
                
                // Mede APENAS a execução real, UMA ÚNICA VEZ
                long start = System.nanoTime();
                for (int step = 0; step < steps; step++) {
                    sim.step();
                }
                long end = System.nanoTime();
                
                double seconds = (end - start) / 1_000_000_000.0;
                times.add(seconds);
                
                sim.shutdown();
            } catch (Exception e) {
                System.err.println("Erro na simulação distribuída (repeat " + r + "): " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return new Stats(times);
    }

    private static double calculateMean(List<Double> values) {
        if (values.isEmpty())
            return Double.NaN;
        double sum = 0.0;
        for (double v : values)
            sum += v;
        return sum / values.size();
    }

    private static double calculateMedian(List<Double> values) {
        if (values.isEmpty())
            return Double.NaN;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    private static double calculateStdDev(List<Double> values, double mean) {
        if (values.isEmpty())
            return Double.NaN;
        double sum = 0.0;
        for (double v : values) {
            sum += (v - mean) * (v - mean);
        }
        return Math.sqrt(sum / values.size());
    }
}
