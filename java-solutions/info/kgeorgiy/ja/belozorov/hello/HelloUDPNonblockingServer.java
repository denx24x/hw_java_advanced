package info.kgeorgiy.ja.belozorov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class HelloUDPNonblockingServer implements HelloServer {
    private ExecutorService service;
    private ExecutorService dispatcher;

    private Selector selector;

    private DatagramChannel channel;
    private int BUFFER_SIZE;

    private volatile boolean open = false;

    public static void main(final String[] args) {
        if (args.length != 2) {
            System.err.println("Wrong arguments count");
        }
        try (final HelloUDPNonblockingServer server = new HelloUDPNonblockingServer()) {
            server.start(
                    Integer.parseInt(args[0]),
                    Integer.parseInt(args[1])
            );
        } catch (final NumberFormatException e) {
            System.err.println("Wrong number format");
        }
    }

    @Override
    public synchronized void start(final int port, final int threads) {
        if (open) {
            return;
        }
        service = Executors.newFixedThreadPool(threads);
        dispatcher = Executors.newSingleThreadExecutor();
        open = true;

        try {
            selector = Selector.open();
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.socket().bind(new InetSocketAddress(port));
            BUFFER_SIZE = channel.socket().getReceiveBufferSize();
            channel.register(selector, SelectionKey.OP_READ, new KeyAttachment());

            dispatcher.submit(() -> {
                        while (selector.isOpen() && !Thread.currentThread().isInterrupted()) {
                            try {
                                selector.select(key -> {
                                    try {
                                        if (key.isValid() && key.isWritable()) {
                                            handleWrite(key);
                                        }
                                        if (key.isValid() && key.isReadable()) {
                                            handleRead(key);
                                        }

                                    } catch (IOException e) {
                                        System.err.println(e);
                                    }
                                });
                            } catch (IOException e) {
                                System.err.println(e);
                            } catch (ClosedSelectorException e) {
                                return;
                            }
                        }
                    }
            );
        } catch (IOException e) {
            System.err.println(e);
        }


    }

    private void handleWrite(SelectionKey key) throws IOException {
        KeyAttachment attachment = (KeyAttachment) key.attachment();
        if (attachment.queue.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }
        AttachmentPair data = attachment.queue.poll();
        channel.send(data.data, data.sock);
        key.interestOpsOr(SelectionKey.OP_READ);

    }

    private void handleRead(SelectionKey key) throws IOException {
        KeyAttachment attachment = (KeyAttachment) key.attachment();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        SocketAddress address = channel.receive(buffer);
        buffer.flip();
        service.submit(() -> {
            String request = StandardCharsets.UTF_8.decode(buffer).toString();
            String response = "Hello, " + request;
            System.err.println(response);


            buffer.clear();
            buffer.put(response.getBytes(StandardCharsets.UTF_8));
            buffer.flip();


            attachment.queue.add(new AttachmentPair(buffer, address));
            key.interestOps(SelectionKey.OP_WRITE);
            selector.wakeup();
        });
    }

    @Override
    public synchronized void close() {
        if (!open) {
            return;
        }

        open = false;

        try {
            selector.close();
        } catch (IOException ignore) {
        }

        try {
            channel.close();
        } catch (IOException ignore) {
        }

        HelloUDPUtils.closeService(service);
        HelloUDPUtils.closeService(dispatcher);
    }

    private static class KeyAttachment {
        LinkedBlockingQueue<AttachmentPair> queue = new LinkedBlockingQueue<>();

        public KeyAttachment() {
        }

    }

    private static class AttachmentPair {
        ByteBuffer data;
        SocketAddress sock;

        public AttachmentPair(ByteBuffer data, SocketAddress sock) {
            this.data = data;
            this.sock = sock;
        }
    }
}
