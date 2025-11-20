package trabalhofinal.difusaocalor.simulator;

/**
 * Classe base abstrata para simuladores de difusão de calor.
 * Contém comportamento comum: buffers, inicialização, swap seguro,
 * aplicação de condições de contorno, medição e cópia segura da matriz.
 */
public abstract class AbstractHeatSimulator {

    protected final int n;
    protected final double alpha;
    protected final double dx;
    protected final double dy;
    protected final double dt;

    protected final double[][] T;
    protected final double[][] newT;
    protected final double[][] initialT;

    protected boolean bordaCima = false;
    protected boolean bordaBaixo = false;
    protected boolean bordaEsquerda = false;
    protected boolean bordaDireita = false;

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
     * Avança a simulação em um passo. A implementação concreta calcula o interior
     * em newT.
     * A troca de buffers é feita de forma segura.
     */
    public final void step() {
        preStepHook();
        // copia T para newT (preserva condições de contorno e evita ler/escrever na
        // mesma matriz)
        copyTToNewT();
        // implementacao concreta atualiza newT com novos valores para o interior
        computeStep();
        // aplica condições de contorno sobre newT
        applyBoundaries(newT);
        // copia resultado de newT para T (swap lógico sem trocar referências finais)
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
     * Implementação concreta calcula o interior da nova matriz (escrever apenas em
     * newT).
     */
    protected abstract void computeStep();

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

    public double measureRunSeconds(int totalSteps, boolean warmup) {
        if (warmup) {
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
