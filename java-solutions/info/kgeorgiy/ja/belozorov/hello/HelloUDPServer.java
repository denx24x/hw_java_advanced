package info.kgeorgiy.ja.belozorov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static info.kgeorgiy.ja.belozorov.hello.HelloUDPUtils.*;

public class HelloUDPServer implements HelloServer {
    private ExecutorService service;
    private DatagramSocket socket;

    private boolean open = false;

    public static void main(final String[] args) {
        if (args.length != 2) {
            System.err.println("Wrong arguments count");
        }
        try (final HelloUDPServer server = new HelloUDPServer()) {
            server.start(
                    Integer.parseInt(args[0]),
                    Integer.parseInt(args[1])
            );
        } catch (final NumberFormatException e) {
            System.err.println("Wrong number format");
        }
    }

    @Override
    public void start(final int port, final int threads) {
        try {
            socket = new DatagramSocket(port);
        } catch (final SocketException e) {
            throw new RuntimeException("Failed to open socket");
        }

        service = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            service.submit(() -> {
                while (!socket.isClosed() && open) {
                    try {
                        // :NOTE: reuse
                        final DatagramPacket request = generatePackage(socket.getReceiveBufferSize());
                        socket.receive(request);

                        final DatagramPacket answer = getStringPackage("Hello, " + parseDatagram(request));
                        answer.setSocketAddress(request.getSocketAddress());
                        socket.send(answer);
                    } catch (final SocketException e) {
                        System.err.println(e);
                        return;
                    } catch (final IOException ignore) {
                        // :NOTE: ignore
                    }
                }
            });
        }
        open = true;
    }

    @Override
    public void close() {
        if (!open) {
            return;
        }
        open = false;

        socket.close();
        HelloUDPUtils.closeService(service);
    }
}
