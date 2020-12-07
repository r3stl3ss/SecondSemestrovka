import java.io.*;
import java.net.Socket;

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
            send(message + "\n");
            if (this.room.userList.size() == 1) {
                send("Используйте команду /amount число_игроков, чтобы решить, сколько человек будут играть\n");
            }
            try {
                while (true) {
                    message = in.readLine();
                    if (message.equals("stop")) {
                        this.downService();
                        break;
                    }
                    if (message.startsWith("/amount ")) {
                        if (this.room.amountOfPlayers == null) {
                            try {
                                Short wantedAmount = Short.parseShort(message.substring(8));
                                if (wantedAmount >= 4) {
                                    this.room.amountOfPlayers = wantedAmount;
                                    message = in.readLine();
                                } else {
                                    send("В эту игру можно играть минимум вчетвером, поэтому комната расширена до четырёх человек.");
                                    this.room.amountOfPlayers = 4;
                                    message = in.readLine();
                                }
                            } catch (NumberFormatException e) {
                                send("Количество игроков введено неправильно.");
                                message = in.readLine();
                            }
                        } else {
                            send("В этой комнате количество участников уже определено: " + this.room.amountOfPlayers);
                            message = in.readLine();
                        }
                    }
                    System.out.println("Echoing: " + message); //дразнимся
                    this.room.story.addStoryEl(message); //у нас всё записано
                    if (this.room.userList.size() == this.room.amountOfPlayers) {
                        for (ServerSomething vr: this.room.userList) {
                            vr.send("В этот момент должна была начаться игра");
                            //Game.start();
                        }
                    }
                    for (ServerSomething vr : this.room.userList) {  // отправляем всем в комнате юзера
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
