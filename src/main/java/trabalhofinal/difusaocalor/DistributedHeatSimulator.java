package trabalhofinal.difusaocalor;

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
	private final ExecutorService executor;

	public DistributedHeatSimulator(int n, double alpha, List<String> workerUrls) {
		super(n, alpha);
		this.workerUrls = new ArrayList<>(workerUrls);
		this.executor = Executors.newFixedThreadPool(Math.max(1, workerUrls.size()));
	}

	public DistributedHeatSimulator(int n, double alpha, String... workerUrls) {
		this(n, alpha, java.util.Arrays.asList(workerUrls));
	}

	@Override
	protected void computeStep() {
		int interior = Math.max(0, n - 2);
		if (interior == 0 || workerUrls.isEmpty()) {
			localCompute(1, n - 2);
			return;
		}

		int workers = workerUrls.size();
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
			final String url = workerUrls.get(i);
			final int s = start;
			final int e = end;

			Callable<WorkerResult> task = () -> {
				try {
					Worker w = (Worker) Naming.lookup(url);
					double[][] block = w.computeBlock(getTemperatureCopy(), s, e, n, alpha, dx, dy, dt);
					return new WorkerResult(s, e, block, null);
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
				localCompute(1, n - 2);
			}
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
