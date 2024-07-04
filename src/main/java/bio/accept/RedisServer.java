package bio.accept;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class RedisServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(6380);
        while(true) {
            System.out.println("模拟RedisServer启动------- 等待连接");
            Socket socket = serverSocket.accept();
            System.out.println("------ 成功连接: " + UUID.randomUUID());
            System.out.println();
        }
    }
}
