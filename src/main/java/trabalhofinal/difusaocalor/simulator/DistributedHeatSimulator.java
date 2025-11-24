package trabalhofinal.difusaocalor.simulator;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import trabalhofinal.difusaocalor.rmi.Worker;

/**
 * Simulador distribuído que delega blocos de linhas para trabalhadores RMI.
 *
 * Estratégia simples: cada passo copia T->newT e pede a cada worker calcular um
 * intervalo de linhas interiores (i=1..n-2). O worker recebe a matriz atual T
 * (leitura somente) e devolve apenas o bloco de linhas que atualizou. Se um
 * worker falhar, o bloco é calculado localmente como fallback.
 */
public class DistributedHeatSimulator extends AbstractHeatSimulator {

	private final List<String> workerUrls;
	private final List<Worker> workerCache;
	private final ExecutorService executor;
	private static final int BATCH_SIZE = 10; // Processar N steps por batch para reduzir RMI overhead

	public DistributedHeatSimulator(int n, double alpha, List<String> workerUrls) {
		super(n, alpha);
		this.workerUrls = new ArrayList<>(workerUrls);
		this.workerCache = new ArrayList<>();
		this.executor = Executors.newFixedThreadPool(Math.max(1, workerUrls.size()));
		initializeWorkers();
	}

	private void initializeWorkers() {
		for (String url : workerUrls) {
			try {
				Worker w = (Worker) Naming.lookup(url);
				workerCache.add(w);
				// Inicializa a matriz uma única vez no worker
				w.initializeMatrix(T, n);
			} catch (Exception ex) {
				System.err.println("Aviso: falha ao conectar com worker " + url + ": " + ex.getMessage());
				workerCache.add(null);
			}
		}
	}

	public DistributedHeatSimulator(int n, double alpha, String... workerUrls) {
		this(n, alpha, java.util.Arrays.asList(workerUrls));
	}

	private void computeStepBatch(int batchSize) {
		int interior = Math.max(0, n - 2);
		if (interior == 0 || workerCache.isEmpty()) {
			for (int b = 0; b < batchSize; b++) {
				localCompute(1, n - 2);
				swapBuffersManually();
			}
			return;
		}

		int workers = workerCache.size();
		int base = interior / workers;
		int rem = interior % workers;

		List<Future<WorkerResult>> futures = new ArrayList<>();

		int cur = 1;
		for (int i = 0; i < workers && cur <= n - 2; i++) {
			int chunk = base + (i < rem ? 1 : 0);
			if (chunk <= 0) {
				break;
			}
			int start = cur;
			int end = Math.min(n - 2, cur + chunk - 1);
			Worker w = workerCache.get(i);
			final int s = start;
			final int e = end;

			if (w == null) {
				for (int b = 0; b < batchSize; b++) {
					localCompute(s, e);
					swapBuffersManually();
				}
				cur = end + 1;
				continue;
			}

			Callable<WorkerResult> task = () -> {
				try {
					double[][] compactBlock = extractBlock(T, s - 1, e + 1);
					double[][] resultBlock = w.computeMultipleSteps(compactBlock, s, e, alpha, dx, dy, dt, batchSize);
					return new WorkerResult(s, e, resultBlock, null);
				} catch (RemoteException re) {
					return new WorkerResult(s, e, null, re);
				} catch (Exception ex) {
					return new WorkerResult(s, e, null, ex);
				}
			};

			futures.add(executor.submit(task));
			cur = end + 1;
		}

		for (Future<WorkerResult> f : futures) {
			try {
				WorkerResult r = f.get();
				if (r.exception == null && r.block != null) {
					int rows = r.e - r.s + 1;
					for (int i = 0; i < rows; i++) {
						System.arraycopy(r.block[i], 0, newT[r.s + i], 0, n);
					}
				} else {
					for (int b = 0; b < batchSize; b++) {
						localCompute(r.s, r.e);
						swapBuffersManually();
					}
				}
			} catch (InterruptedException | ExecutionException e) {
				for (int b = 0; b < batchSize; b++) {
					localCompute(1, n - 2);
					swapBuffersManually();
				}
			}
		}
		// Após batch, copia newT para T
		swapBuffersManually();
	}

	private void swapBuffersManually() {
		synchronized (bufferLock) {
			for (int i = 0; i < n; i++)
				System.arraycopy(newT[i], 0, T[i], 0, n);
		}
		applyBoundaries(T);
	}

	@Override
	protected void computeStep() {
		int interior = Math.max(0, n - 2);
		if (interior == 0 || workerCache.isEmpty()) {
			localCompute(1, n - 2);
			return;
		}

		int workers = workerCache.size();
		int base = interior / workers;
		int rem = interior % workers;

		List<Future<WorkerResult>> futures = new ArrayList<>();

		int cur = 1;
		for (int i = 0; i < workers && cur <= n - 2; i++) {
			int chunk = base + (i < rem ? 1 : 0);
			if (chunk <= 0) {
				break;
			}
			int start = cur;
			int end = Math.min(n - 2, cur + chunk - 1);
			Worker w = workerCache.get(i);
			final int s = start;
			final int e = end;

			if (w == null) {
				localCompute(s, e);
				cur = end + 1;
				continue;
			}

			Callable<WorkerResult> task = () -> {
				try {
					// Envia apenas o bloco necessário (startRow-1 até endRow+1) para reduzir
					// overhead
					double[][] compactBlock = extractBlock(T, s - 1, e + 1);
					double[][] resultBlock = w.computeBlock(compactBlock, s, e, alpha, dx, dy, dt);
					return new WorkerResult(s, e, resultBlock, null);
				} catch (RemoteException re) {
					return new WorkerResult(s, e, null, re);
				} catch (Exception ex) {
					return new WorkerResult(s, e, null, ex);
				}
			};

			futures.add(executor.submit(task));
			cur = end + 1;
		}

		for (Future<WorkerResult> f : futures) {
			try {
				WorkerResult r = f.get();
				if (r.exception == null && r.block != null) {
					int rows = r.e - r.s + 1;
					for (int i = 0; i < rows; i++) {
						System.arraycopy(r.block[i], 0, newT[r.s + i], 0, n);
					}
				} else {
					localCompute(r.s, r.e);
				}
			} catch (InterruptedException | ExecutionException e) {
				Throwable cause = e.getCause();
				System.err.println("Worker futuro falhou: " + (cause != null ? cause : e));
				localCompute(1, n - 2);
			}
		}
	}

	@Override
	public void runSteps(int steps) {
		// Usa batching para reduzir overhead RMI
		int fullBatches = steps / BATCH_SIZE;
		int remainder = steps % BATCH_SIZE;

		for (int i = 0; i < fullBatches; i++) {
			computeStepBatch(BATCH_SIZE);
		}

		if (remainder > 0) {
			computeStepBatch(remainder);
		}
	}

	private void localCompute(int start, int end) {
		if (start > end)
			return;
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

	/**
	 * Extrai um bloco compactado de linhas (startRow até endRow) com linhas
	 * vizinhas.
	 * Isso reduz o overhead de serialização em comparação com enviar a matriz
	 * inteira.
	 */
	private double[][] extractBlock(double[][] mat, int startRow, int endRow) {
		startRow = Math.max(0, startRow);
		endRow = Math.min(n - 1, endRow);
		if (startRow > endRow)
			return new double[0][];

		int rows = endRow - startRow + 1;
		double[][] block = new double[rows][n];
		for (int i = 0; i < rows; i++) {
			System.arraycopy(mat[startRow + i], 0, block[i], 0, n);
		}
		return block;
	}

	@Override
	protected void postStepHook() {
		// Não precisa sincronizar - workers são stateless e recebem blocos completos
	}

	private static class WorkerResult {
		final int s;
		final int e;
		final double[][] block;
		final Throwable exception;

		WorkerResult(int s, int e, double[][] block, Throwable exception) {
			this.s = s;
			this.e = e;
			this.block = block;
			this.exception = exception;
		}
	}

	public void shutdown() {
		executor.shutdownNow();
	}
}
