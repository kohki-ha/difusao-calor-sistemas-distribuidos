package trabalhofinal.difusaocalor.benchmark;

import trabalhofinal.difusaocalor.simulator.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runner simples para comparar performance entre modos Sequencial, Paralelo e
 * Distribuído.
 * Uso:
 * java -cp target\classes trabalhofinal.difusaocalor.BenchmarkRunner <mode> <n>
 * <alpha> <steps> <repeats> [workerUrls]
 * mode = sequencial | paralelo | distribuido | both
 * workerUrls (apenas para distribuido) = rmi://host:port/Name,rmi://...
 * (vírgula separado)
 */
public class BenchmarkRunner {

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("Usage: BenchmarkRunner <mode> <n> <alpha> <steps> <repeats> [workerUrls]");
            return;
        }

        String mode = args[0].toLowerCase();
        int n = Integer.parseInt(args[1]);
        double alpha = Double.parseDouble(args[2]);
        int steps = Integer.parseInt(args[3]);
        int repeats = Integer.parseInt(args[4]);

        String workerUrlsText = args.length >= 6 ? args[5] : "";
        List<String> workerUrls = new ArrayList<>();
        if (!workerUrlsText.isEmpty()) {
            for (String p : workerUrlsText.split(",")) {
                String t = p.trim();
                if (!t.isEmpty())
                    workerUrls.add(t);
            }
        }

        String threadsProp = System.getProperty("benchmark.parallel.threads");
        int parallelThreads = Runtime.getRuntime().availableProcessors();
        if (threadsProp != null) {
            try {
                int parsed = Integer.parseInt(threadsProp);
                if (parsed > 0)
                    parallelThreads = parsed;
            } catch (NumberFormatException ignore) {
            }
        }

        Path report = Path.of("benchmark-report.csv");
        List<String> lines = new ArrayList<>();
        lines.add("mode,n,alpha,steps,repeat,seconds");

        boolean runSequential = mode.equals("sequencial") || mode.equals("both");
        boolean runParallel = mode.equals("paralelo") || mode.equals("both");
        boolean runDistributed = mode.equals("distribuido") || mode.equals("both");

        if (runSequential) {
            System.out.println("Running sequential benchmark: n=" + n + " alpha=" + alpha + " steps=" + steps
                    + " repeats=" + repeats);
            List<Double> times = new ArrayList<>();
            for (int r = 0; r < repeats; r++) {
                SequentialHeatSimulator sim = new SequentialHeatSimulator(n, alpha);
                sim.setBoundaryFlags(true, false, false, false);
                double s = sim.measureRunSeconds(steps, true);
                System.out.printf("Sequential run %d: %.6fs%n", r + 1, s);
                times.add(s);
                lines.add(String.format("sequencial,%d,%.6f,%d,%d,%.6f", n, alpha, steps, r + 1, s));
            }
            printStats("sequencial", times);
        }

        if (runParallel) {
            System.out.println("Running parallel benchmark: n=" + n + " alpha=" + alpha + " steps=" + steps
                    + " repeats=" + repeats + " threads=" + parallelThreads);
            List<Double> times = new ArrayList<>();
            for (int r = 0; r < repeats; r++) {
                ParallelHeatSimulator sim = new ParallelHeatSimulator(n, alpha, parallelThreads);
                sim.setBoundaryFlags(true, false, false, false);
                double s;
                try {
                    s = sim.measureRunSeconds(steps, true);
                } finally {
                    sim.shutdown();
                }
                System.out.printf("Parallel run %d: %.6fs%n", r + 1, s);
                times.add(s);
                lines.add(String.format("paralelo,%d,%.6f,%d,%d,%.6f", n, alpha, steps, r + 1, s));
            }
            printStats("paralelo", times);
        }

        if (runDistributed) {
            System.out.println("Running distributed benchmark: n=" + n + " alpha=" + alpha + " steps=" + steps
                    + " repeats=" + repeats + " workers=" + workerUrls.size());
            List<Double> times = new ArrayList<>();
            for (int r = 0; r < repeats; r++) {
                DistributedHeatSimulator sim;
                try {
                    if (workerUrls.isEmpty()) {
                        System.out.println("No worker URLs provided — aborting distributed run.");
                        break;
                    }
                    sim = new DistributedHeatSimulator(n, alpha, workerUrls);
                } catch (Exception ex) {
                    System.out.println("Failed to create DistributedHeatSimulator: " + ex.getMessage());
                    ex.printStackTrace();
                    break;
                }
                sim.setBoundaryFlags(true, false, false, false);
                double s;
                try {
                    s = sim.measureRunSeconds(steps, true);
                } catch (Exception ex) {
                    System.out.println("Distributed run failed: " + ex.getMessage());
                    ex.printStackTrace();
                    sim.shutdown();
                    break;
                }
                System.out.printf("Distributed run %d: %.6fs%n", r + 1, s);
                times.add(s);
                lines.add(String.format("distribuido,%d,%.6f,%d,%d,%.6f", n, alpha, steps, r + 1, s));
                try {
                    sim.shutdown();
                } catch (Exception ignore) {
                }
            }
            printStats("distribuido", times);
        }

        try {
            Files.write(report, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Report written to " + report.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Failed to write report: " + e.getMessage());
        }
    }

    private static void printStats(String name, List<Double> times) {
        if (times.isEmpty()) {
            System.out.println("No data for " + name);
            return;
        }
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

        System.out.println(String.format("Stats %s: runs=%d mean=%.6fs median=%.6fs sd=%.6fs", name, times.size(),
                mean, median, sd));
    }
}
