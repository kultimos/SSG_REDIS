package bio.read;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class RedisServerBIO {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(6379);
        while(true) {
            System.out.println("-----111 等待连接");
            Socket socket = serverSocket.accept();
            System.out.println("-----222 成功连接");
            InputStream inputStream = null;
            inputStream = socket.getInputStream();
            int length = -1;
            byte[] bytes = new byte[1024];
            System.out.println("----333 等待读取");
            while ((length = inputStream.read(bytes)) != -1) {
                System.out.println("----444 读取成功" + new String(bytes, 0, length));
                System.out.println("==============" + "\t" + UUID.randomUUID());
                System.out.println();
            }
            inputStream.close();
            socket.close();
        }
    }
}
