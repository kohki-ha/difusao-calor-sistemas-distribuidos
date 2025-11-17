package trabalhofinal.difusaocalor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utilitário para executar benchmarks de forma programática e retornar estatísticas.
 */
public class BenchmarkUtil {

    public static class Stats {
        public final List<Double> runs;
        public final double mean;
        public final double median;
        public final double sd;

        public Stats(List<Double> runs, double mean, double median, double sd) {
            this.runs = Collections.unmodifiableList(new ArrayList<>(runs));
            this.mean = mean;
            this.median = median;
            this.sd = sd;
        }
    }

    public static Stats runSequential(int n, double alpha, int steps, int repeats) {
        List<Double> times = new ArrayList<>();
        for (int r = 0; r < repeats; r++) {
            SequentialHeatSimulator sim = new SequentialHeatSimulator(n, alpha);
            sim.setBoundaryFlags(true, false, false, false);
            double s = sim.measureRunSeconds(steps, true);
            times.add(s);
        }
        return buildStats(times);
    }

    public static Stats runDistributed(int n, double alpha, int steps, int repeats, List<String> workerUrls) {
        List<Double> times = new ArrayList<>();
        if (workerUrls == null || workerUrls.isEmpty())
            return buildStats(times);

        for (int r = 0; r < repeats; r++) {
            DistributedHeatSimulator sim = new DistributedHeatSimulator(n, alpha, workerUrls);
            sim.setBoundaryFlags(true, false, false, false);
            try {
                double s = sim.measureRunSeconds(steps, true);
                times.add(s);
            } finally {
                try {
                    sim.shutdown();
                } catch (Exception ignore) {
                }
            }
        }
        return buildStats(times);
    }

    private static Stats buildStats(List<Double> times) {
        if (times.isEmpty())
            return new Stats(times, Double.NaN, Double.NaN, Double.NaN);

        double sum = 0.0;
        for (double t : times)
            sum += t;
        double mean = sum / times.size();
        List<Double> sorted = new ArrayList<>(times);
        Collections.sort(sorted);
        double median = sorted.get(sorted.size() / 2);
        double sd = 0.0;
        for (double t : times)
            sd += (t - mean) * (t - mean);
        sd = Math.sqrt(sd / times.size());
        return new Stats(times, mean, median, sd);
    }
}
