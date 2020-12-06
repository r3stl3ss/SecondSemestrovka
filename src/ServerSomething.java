import java.io.*;
import java.net.Socket;

import static java.lang.Character.isDigit;

class ServerSomething extends Thread {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    //комната конкретного юзера
    private Room room;

    //инициализируем наше всё и стартуем
    public ServerSomething(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        //история комнаты, не сервера
        start();
    }

    @Override
    public void run() {
        String message;
        //this.room - Объект room, то есть сама комната, просто room - номер комнаты
        //обработка служебных сообщений ведётся на сервере!
        int room;
        try {
            room = Integer.parseInt(in.readLine());
            message = in.readLine();
            this.room = Server.getRoom(room);
            this.room.story.printStory(out);
            this.room.userList.add(this);
            try {
                out.write(message + "\n");
                out.flush();
            } catch (IOException ignored) {
            }
            if (this.room.userList.size() == 1) {
                out.write("required roomsize");
                message = in.readLine();
                char[] digits = message.toCharArray();
                String amount;
                for (int i = 0; i < digits.length - 3; i++) {
                    if (!isDigit(digits[i]) || digits[0] == 0) {
                        // TODO: написать проверку на цифры, отправлять ошибку в случае не-цифры и ждать нового, правильного сообщения
                    }
                }
            }
            try {
                while (true) {
                    message = in.readLine();
                    if (message.equals("stop")) {
                        this.downService();
                        break;
                    }
                    System.out.println("Echoing: " + message); //дразнимся
                    this.room.story.addStoryEl(message); //у нас всё записано
                    for (ServerSomething vr : this.room.userList) {  //отправляем всем в комнате юзера
                        vr.send(message); // всем отправляем
                    }
                }
            } catch (NullPointerException ignored) {}
        } catch (IOException e) {
            this.downService();
        }
    }

    //получаем сообщение, выписываем его и чистим буфер
    private void send(String msg) {
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
