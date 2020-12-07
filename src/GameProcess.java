import java.io.*;
import java.net.Socket;
import java.util.List;

public class GameProcess extends Thread {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private Room room;

    public GameProcess(Socket socket, List<ServerSomething> usersList) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        //история комнаты, не сервера
        start();
        // TODO: написать на основе чата текстовую реализацию игры и подключить запуск к основному чату
    }

}
