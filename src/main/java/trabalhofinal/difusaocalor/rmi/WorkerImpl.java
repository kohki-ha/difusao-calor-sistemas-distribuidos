package trabalhofinal.difusaocalor.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementação simples do Worker que calcula localmente o bloco requisitado.
 */
public class WorkerImpl extends UnicastRemoteObject implements Worker {

    protected WorkerImpl() throws RemoteException {
        super();
    }

    @Override
    public double[][] computeBlock(double[][] T, int startRow, int endRow, int n, double alpha, double dx, double dy,
            double dt) throws RemoteException {
        if (startRow > endRow)
            return new double[0][];

        int rows = endRow - startRow + 1;
        double[][] block = new double[rows][n];

        double coefX = alpha * dt / (dx * dx);
        double coefY = alpha * dt / (dy * dy);

        for (int ii = 0; ii < rows; ii++) {
            int i = startRow + ii;
            // colunas interiores 1..n-2
            for (int j = 0; j < n; j++) {
                // preservar borda como cópia do estado atual caso seja borda
                if (i == 0 || i == n - 1 || j == 0 || j == n - 1) {
                    block[ii][j] = T[i][j];
                } else {
                    double t = T[i][j];
                    double tx = T[i + 1][j] - 2 * t + T[i - 1][j];
                    double ty = T[i][j + 1] - 2 * t + T[i][j - 1];
                    block[ii][j] = t + coefX * tx + coefY * ty;
                }
            }
        }

        return block;
    }
}
