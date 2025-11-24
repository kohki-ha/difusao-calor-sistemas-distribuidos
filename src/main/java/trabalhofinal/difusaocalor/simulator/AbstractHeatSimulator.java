package trabalhofinal.difusaocalor.simulator;

/**
 * Classe base abstrata para todos os simuladores de difusão de calor.
 * 
 * Implementa o comportamento comum compartilhado pelas versões sequencial,
 * paralela e distribuída, incluindo:
 * - Gerenciamento de buffers duplos (T e newT) para evitar leitura/escrita
 * simultânea
 * - Inicialização e reset da matriz de temperaturas
 * - Aplicação de condições de contorno (bordas com temperatura fixa)
 * - Sincronização segura para acesso concorrente
 * - Métodos de medição de desempenho com aquecimento (warmup)
 * 
 * Subclasses concretas devem implementar apenas computeStep(), que define
 * como calcular um passo da simulação (sequencial, paralelo ou distribuído).
 * 
 * Equação resolvida: ∂T/∂t = α * (∂²T/∂x² + ∂²T/∂y²)
 * Método numérico: diferenças finitas explícitas
 */
public abstract class AbstractHeatSimulator {

    protected final int n; // Dimensão da malha quadrada (n×n)
    protected final double alpha; // Coeficiente de difusividade térmica do material
    protected final double dx; // Espaçamento espacial no eixo X
    protected final double dy; // Espaçamento espacial no eixo Y
    protected final double dt; // Passo de tempo da simulação

    protected final double[][] T; // Buffer de leitura: matriz atual de temperaturas
    protected final double[][] newT; // Buffer de escrita: próxima matriz de temperaturas
    protected final double[][] initialT; // Matriz inicial (para reset entre experimentos)

    // Flags para condições de contorno: bordas com temperatura fixa (100°C)
    protected boolean bordaCima = false;
    protected boolean bordaBaixo = false;
    protected boolean bordaEsquerda = false;
    protected boolean bordaDireita = false;

    // Lock para sincronização thread-safe do acesso aos buffers
    protected final Object bufferLock = new Object();

    protected AbstractHeatSimulator(int n, double alpha) {
        this(n, alpha, 1.0, 1.0, 0.1);
    }

    protected AbstractHeatSimulator(int n, double alpha, double dx, double dy, double dt) {
        if (n <= 0)
            throw new IllegalArgumentException("n deve ser positivo");
        this.n = n;
        this.alpha = alpha;
        this.dx = dx;
        this.dy = dy;
        this.dt = dt;
        this.T = new double[n][n];
        this.newT = new double[n][n];
        this.initialT = new double[n][n];
        initDefault();
        copyToInitial();
    }

    protected void initDefault() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                T[i][j] = 20.0;
                newT[i][j] = 20.0;
                initialT[i][j] = 20.0;
            }
        }
    }

    protected void copyToInitial() {
        for (int i = 0; i < n; i++)
            System.arraycopy(T[i], 0, initialT[i], 0, n);
    }

    public void setBoundaryFlags(boolean cima, boolean baixo, boolean esquerda, boolean direita) {
        this.bordaCima = cima;
        this.bordaBaixo = baixo;
        this.bordaEsquerda = esquerda;
        this.bordaDireita = direita;
        applyBoundaries(T);
        copyToInitial();
    }

    protected void applyBoundaries(double[][] mat) {
        if (bordaCima)
            for (int i = 0; i < n; i++)
                mat[0][i] = 100.0;
        if (bordaBaixo)
            for (int i = 0; i < n; i++)
                mat[n - 1][i] = 100.0;
        if (bordaEsquerda)
            for (int j = 0; j < n; j++)
                mat[j][0] = 100.0;
        if (bordaDireita)
            for (int j = 0; j < n; j++)
                mat[j][n - 1] = 100.0;
    }

    protected void copyTToNewT() {
        for (int i = 0; i < n; i++)
            System.arraycopy(T[i], 0, newT[i], 0, n);
    }

    /**
     * Avança a simulação em um passo de tempo.
     * 
     * Fluxo de execução (Template Method Pattern):
     * 1. preStepHook() - permite preparação prévia em subclasses
     * 2. Copia T -> newT (preserva condições de contorno)
     * 3. computeStep() - subclasse calcula novas temperaturas em newT
     * 4. Aplica condições de contorno sobre newT
     * 5. Copia newT -> T de forma thread-safe (swap lógico)
     * 6. postStepHook() - permite ações pós-passo em subclasses
     * 
     * O uso de dois buffers evita condições de corrida onde uma thread
     * lê enquanto outra escreve na mesma posição.
     */
    public final void step() {
        preStepHook();
        // Copia T para newT (preserva condições de contorno e evita ler/escrever na
        // mesma matriz)
        copyTToNewT();
        // Implementação concreta atualiza newT com novos valores para o interior
        computeStep();
        // Aplica condições de contorno sobre newT
        applyBoundaries(newT);
        // Copia resultado de newT para T (swap lógico sem trocar referências finais)
        synchronized (bufferLock) {
            for (int i = 0; i < n; i++)
                System.arraycopy(newT[i], 0, T[i], 0, n);
        }
        postStepHook();
    }

    protected void preStepHook() {
    }

    protected void postStepHook() {
    }

    /**
     * Método abstrato que deve ser implementado por subclasses.
     * Calcula as novas temperaturas para o interior da malha e escreve em newT.
     * 
     * A implementação não deve modificar T (apenas leitura) nem as bordas de newT.
     * As bordas serão tratadas automaticamente por applyBoundaries().
     */
    protected abstract void computeStep();

    /**
     * Executa múltiplos passos consecutivos da simulação.
     * Subclasses podem sobrescrever para implementar otimizações em lote.
     * 
     * @param steps número de iterações a executar
     */
    public void runSteps(int steps) {
        for (int s = 0; s < steps; s++)
            step();
    }

    public double[][] getTemperatureCopy() {
        synchronized (bufferLock) {
            double[][] copy = new double[n][n];
            for (int i = 0; i < n; i++)
                System.arraycopy(T[i], 0, copy[i], 0, n);
            return copy;
        }
    }

    public void resetToInitialState() {
        synchronized (bufferLock) {
            for (int i = 0; i < n; i++)
                System.arraycopy(initialT[i], 0, T[i], 0, n);
        }
    }

    /**
     * Executa a simulação e mede o tempo total de execução.
     * 
     * @param totalSteps número de passos a executar
     * @param warmup     se true, executa alguns passos antes da medição para
     *                   aquecer a JVM
     *                   (JIT compilation, cache warming) e depois reseta o estado
     *                   inicial
     * @return tempo de execução em segundos (precisão de nanosegundos)
     */
    public double measureRunSeconds(int totalSteps, boolean warmup) {
        if (warmup) {
            // Aquecimento: executa até 10 passos para preparar JIT e cache
            int w = Math.min(10, totalSteps);
            for (int i = 0; i < w; i++)
                step();
            resetToInitialState();
        }
        long t0 = System.nanoTime();
        runSteps(totalSteps);
        long t1 = System.nanoTime();
        return (t1 - t0) / 1_000_000_000.0;
    }

    public int getSize() {
        return n;
    }

    public double getDt() {
        return dt;
    }
}
