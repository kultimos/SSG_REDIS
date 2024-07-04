package bio.read.multiThread;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class RedisServerBIOMultiThread {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(6379);
        while(true) {
            System.out.println("----redisServerBIOMultiThread 111 等待连接");
            Socket socket = serverSocket.accept();
            System.out.println("----redisServerBIOMultiThread 222 成功连接");
            new Thread( () -> {
                try {
                    InputStream inputStream = socket.getInputStream();
                    int length = -1;
                    byte[] bytes = new byte[1024];
                    System.out.println("----redisServerBIOMultiThread 333 等待读取" + UUID.randomUUID());
                    while((length = inputStream.read(bytes)) != -1) {
                        System.out.println("----redisServerBIOMultiThread 444 成功读取" + new String(bytes, 0, length));
                        System.out.println("==============");
                        System.out.println();
                    }
                    inputStream.close();
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            },Thread.currentThread().getName()).start();
        }
    }
}
