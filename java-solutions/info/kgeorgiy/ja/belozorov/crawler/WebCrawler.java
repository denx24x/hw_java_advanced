package info.kgeorgiy.ja.belozorov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final int downloaders;
    private final int extractors;
    private final int perHost;

    private ExecutorService downloadService;
    private ExecutorService extractService;
    private Map<String, HostExecutor> hostLimit;

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 5) {
            System.err.println("WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }

        Result result;
        try (WebCrawler crawler = new WebCrawler(
                new CachingDownloader(1),
                (args.length >= 3) ? (Integer.parseInt(args[2])) : (5),
                (args.length >= 4) ? (Integer.parseInt(args[3])) : (5),
                (args.length == 5) ? (Integer.parseInt(args[4])) : (5)
        )) {
            int depth = (args.length >= 2) ? (Integer.parseInt(args[1])) : (5);
            result = crawler.download(args[0], depth);
        } catch (IOException e) {
            System.err.println("Error creating downloader" + e.getMessage());
            return;
        } catch (NumberFormatException e) {
            System.err.println("Wrong number format " + e.getMessage());
            return;
        }


        for (String url : result.getDownloaded()) {
            System.out.println(url);
        }

        for (var entry : result.getErrors().entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue().getMessage());
        }
    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = downloaders;
        this.extractors = extractors;
        this.perHost = perHost;
        this.downloadService = Executors.newFixedThreadPool(downloaders);
        this.extractService = Executors.newFixedThreadPool(extractors);
        this.hostLimit = new ConcurrentHashMap<>();
    }

    @Override
    public Result download(String url, int depth) {
        Set<String> downloaded = new HashSet<>();

        Queue<String> result = new ConcurrentLinkedQueue<>();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(url);
        Phaser barrier = new Phaser(1);
        for (int i = 0; i < depth; i++) {
            Queue<String> data = new ConcurrentLinkedQueue<>();
            while (!queue.isEmpty()) {
                String current = queue.poll();
                downloaded.add(current);
                barrier.register();
                try {
                    hostLimit.computeIfAbsent(URLUtils.getHost(current), val -> new HostExecutor(downloadService, perHost)).execute(
                            () -> {
                                try {
                                    Document page = downloader.download(current);
                                    extractService.submit(() -> {
                                        try {
                                            List<String> links = page.extractLinks();
                                            data.addAll(links);
                                            result.add(current);
                                        } catch (IOException e) {
                                            errors.put(current, e);
                                        } finally {
                                            barrier.arriveAndDeregister();
                                        }
                                    });
                                } catch (IOException e) {
                                    errors.put(current, e);
                                    barrier.arriveAndDeregister();
                                }
                            }
                    );
                } catch (MalformedURLException e) {
                    errors.put(current, e);

                }
            }
            barrier.arriveAndAwaitAdvance();
            queue.addAll(data.stream().filter(Predicate.not(downloaded::contains)).distinct().toList());
        }

        return new Result(List.copyOf(result), errors);
    }

    @Override
    public void close() {
        extractService.shutdownNow();
        downloadService.shutdownNow();
        boolean interrupt = false;
        while (true) {
            try {
                if (extractService.awaitTermination(60, TimeUnit.SECONDS) &&
                        downloadService.awaitTermination(60, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                interrupt |= Thread.interrupted();
            }
        }
        if (interrupt) {
            Thread.currentThread().interrupt();
        }
    }
}
