package info.kgeorgiy.ja.belozorov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import static info.kgeorgiy.ja.belozorov.hello.HelloUDPUtils.*;
import static java.lang.Math.max;

public class HelloUDPClient implements HelloClient {
    private static final int DEFAULT_TIMEOUT = 300;
    public static void main(final String[] args) {
        if (args.length != 5) {
            System.err.println("Wrong arguments count");
        }
        try {
            final HelloUDPClient client = new HelloUDPClient();
            client.run(
                    args[0],
                    Integer.parseInt(args[1]),
                    args[2],
                    Integer.parseInt(args[3]),
                    Integer.parseInt(args[4])
            );
        } catch (final NumberFormatException e) {
            System.err.println("Wrong number format");
        }
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final Phaser barrier = new Phaser(1);
        final ExecutorService service = Executors.newFixedThreadPool(threads);


        for (int i = 1; i <= threads; i++) {
            final int index = i; // :NOTE: IntStream
            barrier.register();
            service.submit(() -> {
                try (final DatagramSocket socket = new DatagramSocket()) {
                    int timeout = DEFAULT_TIMEOUT;
                    socket.setSoTimeout(timeout);
                    socket.connect(new InetSocketAddress(host, port));
                    for (int g = 1; g <= requests; g++) {
                        final String original =  String.format("%s%d_%d", prefix, index, g);
                        while (true) {
                            try {
                                // :NOTE: reuse
                                final DatagramPacket answer = generatePackage(socket.getReceiveBufferSize());
                                socket.send(getStringPackage(original));
                                socket.receive(answer);
                                final String result = parseDatagram(answer);
                                System.err.println("Send " + original + "\nReceive " + result);
                                if (reformatString(result).equals("Hello, " + original)) {
                                    System.err.println(original + " - " + result);
                                    timeout = max(50, timeout - 50); // :NOTE: magic number
                                    socket.setSoTimeout(timeout);
                                    break;
                                }
                            } catch (final SocketTimeoutException ignore) {
                                timeout += 50;
                                socket.setSoTimeout(timeout);
                                System.err.println("Timeout " + original);
                            } catch (final IOException e) {
                                System.err.println("IOException " + original);
                            }
                        }
                    }
                    barrier.arrive();
                } catch (final SocketException e) {
                    System.err.println("SocketException");
                }
            });
        }
        barrier.arriveAndAwaitAdvance();
        HelloUDPUtils.closeService(service);
    }

    private String reformatString(final String value) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            result.append(Character.isDigit(c) ? Integer.toString(Character.getNumericValue(c)) : c);
        }
        return result.toString();
    }

}
