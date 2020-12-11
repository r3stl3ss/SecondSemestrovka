import java.io.*;
import java.net.Socket;

class GameServer extends Thread {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private Room room;
    // TODO: переписать инициализацию

    public GameServer(Socket socket, Room room) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.room = room;
        // TODO: написать на основе чата текстовую реализацию игры и подключить запуск к основному чату
    }

    @Override
    public void run() {
        String message;
        sendToPlayer("вот игра типа началась"); // всем отправляем
        try {
            while (true) {
                message = in.readLine();
                for (ServerSomething gp : this.room.userList) {  // отправляем всем в комнате юзера
                    gp.send(message);// всем отправляем
                }
            }
        } catch (NullPointerException | IOException e) {

        }
    }

    private void sendToPlayer(String msg) {
        try {
            out.write(msg + "\n");
            out.flush();
        } catch (IOException ignored) {}

    }

    //суицид
    private void downService() {
        try {
            if(!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
                for (ServerSomething vr : Server.serverList) {
                    if(vr.equals(this)) vr.interrupt();
                    Server.serverList.remove(this);
                }
            }
        } catch (IOException ignored) {}
    }

}
