package info.kgeorgiy.ja.belozorov.crawler;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

public class HostExecutor {

    Queue<Runnable> requests = new ArrayDeque<>();
    ExecutorService executor;
    final int limit;
    int busy;

    public HostExecutor(ExecutorService service, int limit) {
        this.executor = service;
        this.limit = limit;
        this.busy = 0;
    }

    public synchronized void execute(Runnable func) {
        if (busy < limit) {
            busy++;
            executor.submit(() -> {
                func.run();
                this.tryExecute();
            });
        } else {
            requests.add(func);
        }
    }

    public synchronized void tryExecute() {
        if (requests.size() > 0) {
            Runnable func = requests.poll();
            executor.submit(() -> {
                func.run();
                this.tryExecute();
            });
        } else {
            busy--;
        }
    }

}
