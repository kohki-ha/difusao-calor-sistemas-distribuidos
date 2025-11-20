package trabalhofinal.difusaocalor.simulator;

/**
 * Implementação sequencial simples do simulador de difusão de calor.
 */
public class SequentialHeatSimulator extends AbstractHeatSimulator {

    public SequentialHeatSimulator(int n, double alpha) {
        super(n, alpha);
    }

    @Override
    protected void computeStep() {
        // esquema explícito de diferenças finitas (2D)
        double coefX = alpha * dt / (dx * dx);
        double coefY = alpha * dt / (dy * dy);

        // itera apenas sobre o interior (1..n-2)
        for (int i = 1; i < n - 1; i++) {
            for (int j = 1; j < n - 1; j++) {
                double t = T[i][j];
                double tx = T[i + 1][j] - 2 * t + T[i - 1][j];
                double ty = T[i][j + 1] - 2 * t + T[i][j - 1];
                newT[i][j] = t + coefX * tx + coefY * ty;
            }
        }
        // bordas serão aplicadas pelo método base applyBoundaries(newT)
    }
}
