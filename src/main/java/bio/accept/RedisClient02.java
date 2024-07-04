package bio.accept;

import java.io.IOException;
import java.net.Socket;

public class RedisClient02 {
    public static void main(String[] args) throws IOException {
        System.out.println("----Redis client2 start ");
        Socket socket = new Socket("127.0.0.1", 6380);
        System.out.println("----RedisClient02 connection over ");
    }
}
