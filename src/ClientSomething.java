import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

class ClientSomething {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private BufferedReader inputUser;
    private String nickname;
    private Room room;

    public ClientSomething(String addr, int port) {
        // пробуем коннектнуться
        try {
            this.socket = new Socket(addr, port);
        } catch (IOException e) {
            System.err.println("Socket failed"); // беды с сокетом
        }
        try {
            inputUser = new BufferedReader(new InputStreamReader(System.in));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.roomNumber();// выбираем комнату
            this.pressNickname();// вводим ник в чате
            new ReadMsg().start(); // запускаем потоки чтения и написания сообщений
            new WriteMsg().start();

        } catch (IOException e) {
            ClientSomething.this.downService(); // ошибка = отключение
        }

    }

    private void pressNickname() {
        System.out.print("Введите ник: ");
        try {
            nickname = inputUser.readLine();
            out.write("Здарова, " + nickname + "\n");
            out.flush();
        } catch (IOException ignored) {
        }
    }



    //cпрашиваем к какой комнате коннектнуться
    public void roomNumber(){
        System.out.println("Введите номер комнаты: ");
        try{
            short room = Short.parseShort(inputUser.readLine());
            out.write(room + "\n");
            this.room = Server.getRoom(room);
        } catch (IOException ignored){

        }
    }

    private void downService() {
        try {
            if (!socket.isClosed()) { // полное уничтожение сокета
                socket.close();
                in.close();
                out.close();
            }
        } catch (IOException ignored) {}
    }

    private class ReadMsg extends Thread {
        @Override
        public void run() {
            String str;
            try {
                while (true) {
                    str = in.readLine();
                    if (str.equals("stop")) { // стоп-слово
                        ClientSomething.this.downService();
                        break;
                    }
                    System.out.println(str);
                }
            } catch (IOException e) {
                ClientSomething.this.downService(); // ексепшен = смерть
            }
        }
    }
    // TODO: в клиенте прописать ситуацию, в которой сообщение кэпа отличается от остальных сообщений, и отправлять его на сервер, а на сервере оно должно обрабатываться особым способом


    public class WriteMsg extends Thread {
        @Override
        public void run() {
            while (true) {
                String userWord;
                try {
                    userWord = inputUser.readLine();
                    if (userWord.equals("stop")) { // работает стоп-слово
                        out.write("stop" + "\n");
                        ClientSomething.this.downService();
                        break;
                    } else if (userWord.startsWith("/amount ")) {
                        out.write(userWord + "\n");
                    } else {
                        out.write( nickname + ": " + userWord + "\n"); // чтоб при написании сообщения было понятно, кем и когда
                    }
                    out.flush();
                } catch (IOException e) {
                    ClientSomething.this.downService(); // если что-то не так - убиваем
                }

            }
        }
    }
}