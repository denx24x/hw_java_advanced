package info.kgeorgiy.ja.belozorov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
class Counter {
    int val;
    RuntimeException exception = null;

    Counter(int v) {
        val = v;
    }

    public void dec() {
        val--;
    }
}

public class ParallelMapperImpl implements ParallelMapper {

    private final List<Thread> threadsList = new ArrayList<>();
    private final int threadCount;
    private final Queue<Runnable> data = new ArrayDeque<>();
    private final Object writeLock = new Object();


    public ParallelMapperImpl(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Thread count should greater when zero");
        }
        threadCount = threads;
        for (int i = 0; i < threadCount; i++) {
            threadsList.add(new Thread(() -> {
                while (true) {
                    Runnable val;
                    synchronized (writeLock) {
                        while (data.isEmpty()) {
                            try {
                                writeLock.wait();
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                        val = data.poll();
                    }
                    val.run();
                }
            }));
            threadsList.get(i).start();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));
        final Object readLock = new Object();
        final Counter finished = new Counter(args.size());
        synchronized (writeLock) {
            for (int t = 0; t < args.size(); t++) {
                int pos = t;
                T val = args.get(pos);
                data.add(() -> {
                    R ans;
                    try {
                        ans = f.apply(val);
                    } catch (RuntimeException e) {
                        synchronized (readLock) {
                            finished.exception = e;
                        }
                        return;
                    }
                    synchronized (readLock) {
                        result.set(pos, ans);
                        finished.dec();
                        if (finished.val == 0) {
                            readLock.notify();
                        }
                    }
                });
                writeLock.notify();
            }
        }
        synchronized (readLock) {
            while (finished.val != 0) {
                readLock.wait();
            }
            if (Objects.nonNull(finished.exception)) {
                throw finished.exception;
            }
            return result;
        }
    }

    @Override
    public void close() {
        for (Thread thread : threadsList) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException ignore) {
            }
        }
    }
}