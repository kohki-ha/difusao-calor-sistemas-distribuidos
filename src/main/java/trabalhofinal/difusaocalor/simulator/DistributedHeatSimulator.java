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
 * Versão distribuída do simulador que delega cálculos para workers RMI remotos.
 * 
 * Arquitetura mestre-trabalhador (master-worker):
 * - O coordenador (esta classe) divide as linhas da malha entre workers
 * - Cada worker RMI calcula independentemente seu bloco de linhas
 * - Workers podem estar em processos ou máquinas diferentes
 * - Comunicação via Java RMI (Remote Method Invocation)
 * 
 * Otimizações implementadas:
 * 1. Cache de matriz: enviada uma única vez no início (initializeMatrix)
 * 2. Blocos compactos: apenas vizinhanças necessárias são transmitidas
 * 3. Batching: múltiplos passos processados por chamada RMI (BATCH_SIZE)
 * 4. Fallback local: se worker falha, calcula localmente
 * 5. Execução assíncrona: coordenador aguarda workers com Future/Callable
 * 
 * Trade-offs:
 * - Overhead de serialização RMI pode dominar em malhas pequenas
 * - Vantajoso quando workers têm recursos computacionais dedicados
 * - Latência de rede é fator limitante (workers locais = menos ganho)
 */
public class DistributedHeatSimulator extends AbstractHeatSimulator {

	private final List<String> workerUrls; // URLs RMI dos workers (rmi://host:port/nome)
	private final List<Worker> workerCache; // Stubs RMI conectados aos workers
	private final ExecutorService executor; // Pool para chamadas RMI assíncronas

	/**
	 * Número de passos processados por lote em cada worker.
	 * Reduz chamadas RMI de N para N/BATCH_SIZE (~90% menos overhead).
	 */
	private static final int BATCH_SIZE = 10;

	public DistributedHeatSimulator(int n, double alpha, List<String> workerUrls) {
		super(n, alpha);
		this.workerUrls = new ArrayList<>(workerUrls);
		this.workerCache = new ArrayList<>();
		this.executor = Executors.newFixedThreadPool(Math.max(1, workerUrls.size()));
		initializeWorkers();
	}

	/**
	 * Inicializa conexões com todos os workers RMI.
	 * 
	 * Para cada URL fornecida:
	 * 1. Faz lookup no RMI registry para obter o stub
	 * 2. Envia a matriz inicial (única transmissão completa)
	 * 3. Se falhar, adiciona null ao cache (será tratado como fallback local)
	 * 
	 * Workers indisponíveis não interrompem a simulação - seus blocos
	 * são calculados localmente pelo coordenador.
	 */
	private void initializeWorkers() {
		for (String url : workerUrls) {
			try {
				Worker w = (Worker) Naming.lookup(url);
				workerCache.add(w);
				// Envia matriz inicial - única transferência completa da simulação
				w.initializeMatrix(T, n);
			} catch (Exception ex) {
				System.err.println("Aviso: falha ao conectar com worker " + url + ": " + ex.getMessage());
				workerCache.add(null); // Marca worker como indisponível
			}
		}
	}

	public DistributedHeatSimulator(int n, double alpha, String... workerUrls) {
		this(n, alpha, java.util.Arrays.asList(workerUrls));
	}

	/**
	 * Processa um lote de passos distribuindo trabalho entre workers RMI.
	 * 
	 * Otimização chave: em vez de fazer N chamadas RMI (uma por passo),
	 * faz apenas 1 chamada pedindo ao worker processar N passos internamente.
	 * 
	 * Algoritmo:
	 * 1. Divide linhas interiores entre workers disponíveis
	 * 2. Para cada worker:
	 * - Extrai bloco compacto (linhas + vizinhanças)
	 * - Chama computeMultipleSteps(batchSize) de forma assíncrona
	 * - Se worker falhar ou for null, calcula localmente
	 * 3. Aguarda todos os Futures completarem
	 * 4. Mescla resultados em newT
	 * 5. Faz swap manual de buffers
	 * 
	 * @param batchSize número de passos a processar em lote
	 */
	private void computeStepBatch(int batchSize) {
		int interior = Math.max(0, n - 2);

		// Fallback: se não há interior ou workers, calcula tudo localmente
		if (interior == 0 || workerCache.isEmpty()) {
			for (int b = 0; b < batchSize; b++) {
				localCompute(1, n - 2);
				swapBuffersManually();
			}
			return;
		}

		// Divide linhas interiores entre workers (balanceamento)
		int workers = workerCache.size();
		int base = interior / workers;
		int rem = interior % workers;

		List<Future<WorkerResult>> futures = new ArrayList<>();

		int cur = 1; // Linha inicial atual
		for (int i = 0; i < workers && cur <= n - 2; i++) {
			// Primeiros 'rem' workers recebem uma linha extra
			int chunk = base + (i < rem ? 1 : 0);
			if (chunk <= 0) {
				break;
			}
			int start = cur;
			int end = Math.min(n - 2, cur + chunk - 1);
			Worker w = workerCache.get(i);
			final int s = start;
			final int e = end;

			// Se worker indisponível, calcula localmente
			if (w == null) {
				for (int b = 0; b < batchSize; b++) {
					localCompute(s, e);
					swapBuffersManually();
				}
				cur = end + 1;
				continue;
			}

			// Cria tarefa assíncrona para chamar worker via RMI
			Callable<WorkerResult> task = () -> {
				try {
					// Extrai bloco compacto: [s-1, s, s+1, ..., e-1, e, e+1]
					// Inclui vizinhanças necessárias para o cálculo
					double[][] compactBlock = extractBlock(T, s - 1, e + 1);

					// Chamada RMI: processa batchSize passos remotamente
					double[][] resultBlock = w.computeMultipleSteps(compactBlock, s, e, alpha, dx, dy, dt, batchSize);
					return new WorkerResult(s, e, resultBlock, null);
				} catch (RemoteException re) {
					// Falha de comunicação RMI - será tratada como fallback
					return new WorkerResult(s, e, null, re);
				} catch (Exception ex) {
					return new WorkerResult(s, e, null, ex);
				}
			};

			futures.add(executor.submit(task));
			cur = end + 1;
		}

		// Aguarda e processa resultados de todos os workers
		for (Future<WorkerResult> f : futures) {
			try {
				WorkerResult r = f.get(); // Bloqueia até worker terminar
				if (r.exception == null && r.block != null) {
					// Sucesso: mescla resultado do worker em newT
					int rows = r.e - r.s + 1;
					for (int i = 0; i < rows; i++) {
						System.arraycopy(r.block[i], 0, newT[r.s + i], 0, n);
					}
				} else {
					// Falha: worker retornou exceção, calcula localmente
					for (int b = 0; b < batchSize; b++) {
						localCompute(r.s, r.e);
						swapBuffersManually();
					}
				}
			} catch (InterruptedException | ExecutionException e) {
				// Exceção ao aguardar Future: fallback completo
				for (int b = 0; b < batchSize; b++) {
					localCompute(1, n - 2);
					swapBuffersManually();
				}
			}
		}
		// Após processar batch completo, sincroniza buffers
		swapBuffersManually();
	}

	/**
	 * Troca buffers T ↔ newT de forma segura e aplica condições de contorno.
	 * Usado após processar um lote completo de passos.
	 */
	private void swapBuffersManually() {
		synchronized (bufferLock) {
			for (int i = 0; i < n; i++)
				System.arraycopy(newT[i], 0, T[i], 0, n);
		}
		applyBoundaries(T);
	}

	/**
	 * Implementação de passo único (sem batching).
	 * Mantida para compatibilidade, mas menos eficiente que computeStepBatch.
	 * Chamada quando runSteps() não é sobrescrito ou em casos especiais.
	 */
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

	/**
	 * Sobrescreve runSteps() para aplicar otimização de batching.
	 * 
	 * Em vez de fazer N passos individuais com N chamadas RMI,
	 * agrupa em lotes de BATCH_SIZE passos. Exemplo:
	 * - 200 passos → 20 lotes de 10 (200 chamadas RMI → 20 chamadas)
	 * - Redução de ~90% no overhead de comunicação
	 * 
	 * @param steps número total de passos a executar
	 */
	@Override
	public void runSteps(int steps) {
		int fullBatches = steps / BATCH_SIZE; // Lotes completos
		int remainder = steps % BATCH_SIZE; // Passos restantes

		// Processa lotes completos
		for (int i = 0; i < fullBatches; i++) {
			computeStepBatch(BATCH_SIZE);
		}

		// Processa passos restantes (se houver)
		if (remainder > 0) {
			computeStepBatch(remainder);
		}
	}

	/**
	 * Calcula um bloco de linhas localmente (fallback quando worker falha).
	 * Idêntico ao código do SequentialHeatSimulator, mas para um intervalo
	 * específico.
	 * 
	 * @param start primeira linha a calcular
	 * @param end   última linha a calcular
	 */
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
	 * Extrai um bloco compacto de linhas da matriz para enviar ao worker.
	 * 
	 * Otimização crucial: em vez de serializar a matriz completa (n×n)
	 * a cada chamada RMI, envia apenas o subconjunto necessário.
	 * 
	 * Exemplo: para calcular linhas 10-20, extrai linhas 9-21
	 * (inclui vizinhanças para o cálculo de diferenças finitas)
	 * 
	 * Redução de tráfego: matriz 2000×2000 com 4 workers →
	 * - Sem otimização: 32 MB por chamada × 4 = 128 MB
	 * - Com extração: ~8 MB por chamada × 4 = 32 MB (75% menos)
	 * 
	 * @param mat      matriz fonte
	 * @param startRow primeira linha a extrair (incluindo vizinhança)
	 * @param endRow   última linha a extrair (incluindo vizinhança)
	 * @return bloco compacto [startRow..endRow] × [0..n-1]
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
		// Workers são stateless e recebem blocos completos, não necessita sincronização
		// extra
	}

	/**
	 * Classe auxiliar para encapsular resultado de um worker.
	 * Permite retornar tanto sucesso (bloco calculado) quanto falha (exceção).
	 */
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
