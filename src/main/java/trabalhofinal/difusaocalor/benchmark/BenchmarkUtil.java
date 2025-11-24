package trabalhofinal.difusaocalor.benchmark;

import trabalhofinal.difusaocalor.simulator.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utilitário para execução automatizada de benchmarks e coleta de estatísticas.
 * 
 * Permite comparar o desempenho das três versões do simulador:
 * - Sequencial (single-thread)
 * - Paralela (multi-thread local)
 * - Distribuída (RMI workers)
 * 
 * Cada método executa múltiplas repetições da simulação com parâmetros fixos
 * e retorna estatísticas descritivas (média, mediana, desvio padrão).
 * 
 * Inclui aquecimento (warmup) automático para permitir otimizações JIT antes
 * da medição real, garantindo resultados mais estáveis e representativos.
 */
public class BenchmarkUtil {

    /**
     * Encapsula estatísticas descritivas de um benchmark.
     * Contém tempos individuais de cada repetição e métricas agregadas.
     */
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

    /**
     * Executa benchmark da versão sequencial.
     * 
     * @param n       dimensão da malha quadrada (n×n)
     * @param alpha   coeficiente de difusividade térmica
     * @param steps   número de passos por repetição
     * @param repeats número de repetições para medir variabilidade
     * @return estatísticas com tempos de cada repetição e métricas agregadas
     */
    public static Stats runSequential(int n, double alpha, int steps, int repeats) {
        List<Double> times = new ArrayList<>();
        for (int r = 0; r < repeats; r++) {
            SequentialHeatSimulator sim = new SequentialHeatSimulator(n, alpha);
            sim.setBoundaryFlags(true, false, false, false); // Borda superior quente
            double s = sim.measureRunSeconds(steps, true); // Com warmup
            times.add(s);
        }
        return buildStats(times);
    }

    /**
     * Executa benchmark da versão paralela com número configurável de threads.
     * 
     * @param n           dimensão da malha
     * @param alpha       coeficiente de difusividade
     * @param steps       passos por repetição
     * @param repeats     número de repetições
     * @param threadCount número de threads (se ≤ 0, usa número de cores
     *                    disponíveis)
     * @return estatísticas de desempenho
     */
    public static Stats runParallel(int n, double alpha, int steps, int repeats, int threadCount) {
        List<Double> times = new ArrayList<>();
        int workers = threadCount <= 0 ? Runtime.getRuntime().availableProcessors() : threadCount;
        for (int r = 0; r < repeats; r++) {
            ParallelHeatSimulator sim = new ParallelHeatSimulator(n, alpha, workers);
            sim.setBoundaryFlags(true, false, false, false);
            double s;
            try {
                s = sim.measureRunSeconds(steps, true);
            } finally {
                sim.shutdown(); // Importante: libera pool de threads
            }
            times.add(s);
        }
        return buildStats(times);
    }

    /**
     * Executa benchmark da versão distribuída com workers RMI.
     * 
     * Reutiliza o mesmo simulador para todas as repetições (evita overhead
     * de reconexão RMI). Reseta o estado inicial entre repetições.
     * 
     * @param n          dimensão da malha
     * @param alpha      coeficiente de difusividade
     * @param steps      passos por repetição
     * @param repeats    número de repetições
     * @param workerUrls lista de URLs RMI dos workers (ex:
     *                   rmi://localhost:1099/Worker1)
     * @return estatísticas de desempenho (vazio se nenhum worker disponível)
     */
    public static Stats runDistributed(int n, double alpha, int steps, int repeats, List<String> workerUrls) {
        List<Double> times = new ArrayList<>();
        if (workerUrls == null || workerUrls.isEmpty())
            return buildStats(times);

        DistributedHeatSimulator sim = new DistributedHeatSimulator(n, alpha, workerUrls);
        sim.setBoundaryFlags(true, false, false, false);

        try {
            for (int r = 0; r < repeats; r++) {
                // Reinicia a matriz para cada repetição
                sim.resetToInitialState();
                double s = sim.measureRunSeconds(steps, true);
                times.add(s);
            }
        } finally {
            try {
                sim.shutdown();
            } catch (Exception ignore) {
            }
        }
        return buildStats(times);
    }

    /**
     * Calcula estatísticas descritivas a partir de uma lista de tempos.
     * 
     * Métricas calculadas:
     * - Média aritmética: indica tendência central
     * - Mediana: valor central, menos sensível a outliers
     * - Desvio padrão: mede variabilidade/consistência dos tempos
     * 
     * @param times lista de tempos de execução em segundos
     * @return objeto Stats com métricas (NaN se lista vazia)
     */
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
