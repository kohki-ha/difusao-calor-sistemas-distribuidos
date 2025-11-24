package trabalhofinal.difusaocalor.rmi;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/**
 * Pequeno lançador para registre um Worker no RMI Registry.
 * Uso: java WorkerServer <name> <port>
 * Exemplo: java WorkerServer Worker1 1099
 */
public class WorkerServer {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: WorkerServer <name> <port>");
            System.exit(1);
        }
        String name = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            LocateRegistry.createRegistry(port);
        } catch (Exception ignore) {
        }

        String url = "rmi://localhost:" + port + "/" + name;
        WorkerImpl impl = new WorkerImpl();
        Naming.rebind(url, impl);
        System.out.println("Worker bound at " + url + ". Pressione Ctrl+C para encerrar.");
        // manter processo vivo para atender chamadas remotas
        new java.util.concurrent.CountDownLatch(1).await();
    }
}
