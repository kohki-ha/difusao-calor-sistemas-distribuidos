package trabalhofinal.difusaocalor.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementação do Worker que mantém a matriz em cache local.
 * Otimização: recebe matriz uma única vez, depois apenas blocos compactados.
 */
public class WorkerImpl extends UnicastRemoteObject implements Worker {

    private double[][] T;  // Matriz mantida em cache no worker
    private int n;

    protected WorkerImpl() throws RemoteException {
        super();
    }

    @Override
    public void initializeMatrix(double[][] matrix, int dimension) throws RemoteException {
        this.n = dimension;
        this.T = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, T[i], 0, n);
        }
    }

    @Override
    public double[][] computeBlock(double[][] block, int startRow, int endRow, double alpha, double dx,
            double dy, double dt) throws RemoteException {
        if (startRow > endRow || block == null || block.length == 0)
            return new double[0][];

        int rows = endRow - startRow + 1;
        double[][] result = new double[rows][n];

        double coefX = alpha * dt / (dx * dx);
        double coefY = alpha * dt / (dy * dy);

        // block contém: [0] = linha startRow-1, [1..rows] = linhas a calcular, [rows+1] = linha endRow+1
        for (int ii = 0; ii < rows; ii++) {
            int blockRowIdx = ii + 1;

            for (int j = 1; j < n - 1; j++) {
                double t = block[blockRowIdx][j];
                double tx = block[blockRowIdx + 1][j] - 2 * t + block[blockRowIdx - 1][j];
                double ty = block[blockRowIdx][j + 1] - 2 * t + block[blockRowIdx][j - 1];
                result[ii][j] = t + coefX * tx + coefY * ty;
            }

            result[ii][0] = block[blockRowIdx][0];
            result[ii][n - 1] = block[blockRowIdx][n - 1];
        }

        return result;
    }

    @Override
    public void updateMatrix(double[][] newT) throws RemoteException {
        if (newT == null || newT.length != n)
            return;
        for (int i = 0; i < n; i++) {
            System.arraycopy(newT[i], 0, T[i], 0, n);
        }
    }
}
