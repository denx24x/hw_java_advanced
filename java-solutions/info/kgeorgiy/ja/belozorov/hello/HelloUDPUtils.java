package info.kgeorgiy.ja.belozorov.hello;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class HelloUDPUtils {
    public static void closeService(ExecutorService service) {
        service.shutdownNow();
        boolean interrupt = false;
        while (true) {
            try {
                if (service.awaitTermination(60, TimeUnit.SECONDS)) {
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

    public static String parseDatagram(DatagramPacket data){
        return new String(data.getData(), data.getOffset(), data.getLength(), StandardCharsets.UTF_8);
    }

    public static DatagramPacket generatePackage(int length){
        byte[] message = new byte[length];
        return new DatagramPacket(message, length);
    }

    public static DatagramPacket getStringPackage(String str){
        byte[] message = str.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(message, message.length);
    }
}
