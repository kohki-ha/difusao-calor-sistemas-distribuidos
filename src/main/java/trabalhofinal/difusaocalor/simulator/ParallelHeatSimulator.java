package trabalhofinal.difusaocalor.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Versão paralelizada (multi-thread) do simulador de difusão de calor.
 * 
 * Estratégia de paralelização:
 * - Divide as linhas interiores da malha em blocos contíguos
 * - Cada thread processa independentemente um bloco de linhas
 * - Usa ExecutorService com pool de threads fixo
 * - CountDownLatch garante sincronização: todas as threads terminam antes do
 * swap de buffers
 * 
 * Vantagens:
 * - Explora paralelismo de dados (data parallelism)
 * - Sem dependências entre threads (cada uma lê de T e escreve em newT)
 * - Escalabilidade proporcional ao número de núcleos da CPU
 * 
 * Ideal para máquinas multi-core quando a malha é suficientemente grande
 * para compensar o overhead de criação/sincronização de threads.
 */
public class ParallelHeatSimulator extends AbstractHeatSimulator {

    private final ExecutorService executor; // Pool de threads para cálculos paralelos
    private final List<LineRange> ranges; // Divisão de linhas entre threads

    public ParallelHeatSimulator(int n, double alpha) {
        this(n, alpha, Runtime.getRuntime().availableProcessors());
    }

    public ParallelHeatSimulator(int n, double alpha, int threadCount) {
        super(n, alpha);
        int workers = Math.max(1, threadCount);
        this.executor = Executors.newFixedThreadPool(workers);
        this.ranges = buildRanges(workers);
    }

    /**
     * Divide as linhas interiores da malha em blocos balanceados para as threads.
     * 
     * Algoritmo de balanceamento:
     * - Calcula número de linhas interiores (n-2, excluindo bordas)
     * - Divide em no máximo 'workers' blocos (pode ser menos se houver poucas
     * linhas)
     * - Distribui linhas extras uniformemente: primeiros blocos recebem +1 linha
     * 
     * Exemplo: 100 linhas interiores, 3 threads -> [34 linhas, 33 linhas, 33
     * linhas]
     * 
     * @param workers número de threads no pool
     * @return lista de ranges, cada um definindo [startRow, endRow] para uma thread
     */
    private List<LineRange> buildRanges(int workers) {
        List<LineRange> list = new ArrayList<>();
        int interiorStart = 1; // Primeira linha interior
        int interiorEnd = n - 2; // Última linha interior
        if (interiorEnd < interiorStart)
            return list; // Malha muito pequena (n <= 2)

        int interior = interiorEnd - interiorStart + 1;
        int maxChunks = Math.min(workers, interior); // Não criar mais threads que linhas
        int base = interior / maxChunks; // Linhas por bloco (base)
        int remainder = interior % maxChunks; // Linhas extras a distribuir

        int current = interiorStart;
        for (int i = 0; i < maxChunks; i++) {
            // Primeiros 'remainder' blocos recebem uma linha extra
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

        // CountDownLatch: barreira de sincronização para aguardar todas as threads
        CountDownLatch latch = new CountDownLatch(ranges.size());

        // Submete uma tarefa para cada bloco de linhas
        for (LineRange range : ranges) {
            executor.execute(() -> {
                try {
                    // Cada thread calcula seu bloco independentemente
                    computeRange(range.start, range.end);
                } finally {
                    // Sempre decrementa o contador, mesmo se houver exceção
                    latch.countDown();
                }
            });
        }

        // Aguarda todas as threads terminarem antes de prosseguir
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel computation interrupted", e);
        }
    }

    /**
     * Calcula um bloco de linhas (execução em thread worker).
     * 
     * Este método é chamado em paralelo por múltiplas threads, cada uma
     * processando um intervalo [start, end] sem sobreposição.
     * 
     * Thread-safety: seguro porque cada thread escreve em posições distintas
     * de newT e apenas lê de T (que não é modificado durante computeStep).
     */
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
