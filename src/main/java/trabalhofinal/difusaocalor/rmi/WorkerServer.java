package trabalhofinal.difusaocalor.rmi;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/**
 * Servidor RMI que inicializa e registra um worker no RMI Registry.
 * 
 * Este programa deve ser executado em cada máquina que atuará como worker
 * no sistema distribuído. Cada instância cria um RMI registry na porta
 * especificada e registra um objeto Worker que ficará disponível para
 * chamadas remotas do coordenador.
 * 
 * Uso:
 * java WorkerServer <nome> <porta>
 * 
 * Exemplo:
 * java WorkerServer Worker1 1099
 * java WorkerServer Worker2 1100
 * 
 * O processo permanece ativo aguardando chamadas remotas até ser
 * encerrado manualmente (Ctrl+C).
 */
public class WorkerServer {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: WorkerServer <name> <port>");
            System.exit(1);
        }
        String name = args[0];
        int port = Integer.parseInt(args[1]);

        // Tenta criar o RMI registry na porta especificada
        // Se já existir, ignora a exceção e reutiliza o registry existente
        try {
            LocateRegistry.createRegistry(port);
        } catch (Exception ignore) {
            // Registry já existe, pode ser compartilhado por múltiplos workers
        }

        // Constrói a URL RMI e registra o worker
        String url = "rmi://localhost:" + port + "/" + name;
        WorkerImpl impl = new WorkerImpl();
        Naming.rebind(url, impl);
        System.out.println("Worker bound at " + url + ". Pressione Ctrl+C para encerrar.");

        // Mantém o processo vivo para atender chamadas remotas indefinidamente
        new java.util.concurrent.CountDownLatch(1).await();
    }
}
