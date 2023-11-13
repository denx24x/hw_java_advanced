package info.kgeorgiy.ja.belozorov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.lang.Math.max;

public class HelloUDPNonblockingClient implements HelloClient {
    private static final int DEFAULT_TIMEOUT = 300;

    public static void main(final String[] args) {
        if (args.length != 5) {
            System.err.println("Wrong arguments count");
        }
        try {
            final HelloUDPNonblockingClient client = new HelloUDPNonblockingClient();
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
        Selector selector;
        List<DatagramChannel> channels = new ArrayList<>();
        InetSocketAddress addr;
        addr = new InetSocketAddress(host, port);

        try {
            selector = Selector.open();
        } catch (IOException e) {
            System.err.println(e);
            return;
        }

        try {

            for (int i = 0; i < threads; i++) {
                DatagramChannel channel = DatagramChannel.open();
                ;
                channels.add(channel);
                channel.connect(addr);
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_READ, new KeyAttachment(i, channel.socket().getReceiveBufferSize()));
            }
        } catch (IOException e) {
            for (int i = 0; i < channels.size(); i++) {
                try {
                    channels.get(i).close();
                } catch (IOException ignore) {
                }
            }
            try {
                selector.close();
            } catch (IOException ignore) {
            }
            System.err.println(e);
            return;
        }

        selector.keys().forEach(v -> v.interestOps(SelectionKey.OP_WRITE));

        int uncompleted = threads;
        int timeout = DEFAULT_TIMEOUT;

        while (uncompleted > 0) {
            try {
                if (selector.select(DEFAULT_TIMEOUT) > 0) {
                    for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                        SelectionKey key = i.next();
                        if (!key.isValid()) {
                            continue;
                        }
                        try {
                            KeyAttachment attachment = (KeyAttachment) key.attachment();
                            final String original = String.format("%s%d_%d", prefix, attachment.channel + 1, attachment.counter);
                            DatagramChannel channel = (DatagramChannel) key.channel();
                            if (key.isWritable()) {
                                ByteBuffer data = ByteBuffer.wrap(original.getBytes(StandardCharsets.UTF_8));
                                channel.send(data, addr);
                                key.interestOps(SelectionKey.OP_READ);
                                System.err.println("Send " + original);
                            } else if (key.isReadable()) {
                                ByteBuffer data = ByteBuffer.allocate(attachment.buffer_size);
                                channel.receive(data);
                                key.interestOps(SelectionKey.OP_WRITE);
                                data.flip();
                                timeout = max(50, timeout - 50);
                                String answer = StandardCharsets.UTF_8.decode(data).toString();
                                System.err.println("Receive " + answer);
                                if (reformatString(answer).equals("Hello, " + original)) {
                                    System.err.println(original + " - " + answer);
                                    attachment.counter++;
                                    if (attachment.counter > requests) {
                                        uncompleted--;
                                        key.cancel();
                                        channel.close();
                                    }
                                } else {
                                    key.interestOps(SelectionKey.OP_WRITE);
                                }
                            }
                        } catch (IOException e) {
                            System.err.println(e);

                        } finally {
                            i.remove();
                        }
                    }
                } else {
                    timeout += 50;
                    selector.keys().forEach(v -> v.interestOps(SelectionKey.OP_WRITE));
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        for (int i = 0; i < channels.size(); i++) {
            try {
                channels.get(i).close();
            } catch (IOException ignore) {
            }
        }
        try {
            selector.close();
        } catch (IOException ignore) {
        }

    }

    private String reformatString(final String value) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            result.append(Character.isDigit(c) ? Integer.toString(Character.getNumericValue(c)) : c);
        }
        return result.toString();
    }

    private static class KeyAttachment {
        private final int channel;
        private final int buffer_size;
        private int counter = 1;

        public KeyAttachment(int channel, int buffer_size) {
            this.channel = channel;
            this.buffer_size = buffer_size;
        }

    }

}