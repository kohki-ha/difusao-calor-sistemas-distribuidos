package trabalhofinal.difusaocalor.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface remota para um trabalhador que calcula um bloco de linhas da malha.
 */
public interface Worker extends Remote {

    /**
     * Calcula as novas temperaturas para as linhas de índice startRow..endRow (inclusive).
     * Recebe a matriz completa T apenas para leitura.
     * Retorna um bloco com (endRow-startRow+1) linhas e n colunas.
     */
    double[][] computeBlock(double[][] T, int startRow, int endRow, int n, double alpha, double dx, double dy, double dt)
            throws RemoteException;
}
