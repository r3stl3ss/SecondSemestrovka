import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;

class ServerSomething extends Thread {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private Color curTeam;
    private boolean isCap;

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
            send(message + "\n"); // здесь он выписывает ник и приветствие
            for (Room availableRoom: Server.roomList) {
                send(Server.roomToString(availableRoom));
            }
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
                                    send("Когда в комнате наберётся " + this.room.amountOfPlayers + " человек, игра начнётся.");
                                    continue;
                                } else {
                                    send("В эту игру можно играть минимум вчетвером, поэтому комната расширена до четырёх человек.");
                                    this.room.amountOfPlayers = 4;
                                    continue;
                                }
                            } catch (NumberFormatException e) {
                                send("Количество игроков введено неправильно.");
                                continue;
                            }
                        } else {
                            send("В этой комнате количество участников уже определено: " + this.room.amountOfPlayers);
                        }
                    }
                    System.out.println("Echoing: " + message); // дразнимся
                    this.room.story.addStoryEl(message); //у нас всё записано
                    if (this.room.userList.size() == this.room.amountOfPlayers) {
                        this.room.playingRightNow = true;
                        ArrayList<String> justWords = new ArrayList<>();
                        fillByWords(justWords);
                        Iterator<String> iter = justWords.iterator();
                        List<ServerSomething> redTeam;
                        List<ServerSomething> blueTeam;
                        while (iter.hasNext()) { //генерация слов для игры
                            sendAll(iter.next());
                        }
                        ArrayList<PlayableWord> wordsForGame = makeWordsPlayable(justWords);
                        divideOnTeams();
                        // здесь игровой функционал

                    }
                    else {
                        sendAll(message); // а тут просто чатик
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

    private void sendAll(String msg) {
        for (ServerSomething vr : this.room.userList) {  // отправляем всем в комнате юзера
            vr.send(msg); // всем отправляем
        }
    }

    private void fillByWords(ArrayList<String> wordListForGame) throws IOException {
        List<String> vocabulary = new ArrayList<>();
        FileReader fr = new FileReader("src/words/allwords.txt");
        BufferedReader reader = new BufferedReader(fr);
        String word = reader.readLine();
        while (word != null) {
            vocabulary.add(word);
            word = reader.readLine();
        }
        while (wordListForGame.size() < 25) {
            wordListForGame.add(vocabulary.get((int)(Math.random() * vocabulary.size())));
        }
    }

    private ArrayList<PlayableWord> makeWordsPlayable(ArrayList<String> wordsForThisGame) {
        double whoFirst = Math.random();
        ArrayList<PlayableWord> playableWords = new ArrayList<PlayableWord>();
        for (int i = 0; i < 8; i++) {
            playableWords.add(new PlayableWord(wordsForThisGame.get(i), Color.red));
        }
        for (int i = 8; i < 16; i++) {
            playableWords.add(new PlayableWord(wordsForThisGame.get(i), Color.blue));
        }
        if (whoFirst > 0.5) {
            playableWords.add(new PlayableWord(wordsForThisGame.get(16), Color.red));
        } else {
            playableWords.add(new PlayableWord(wordsForThisGame.get(16), Color.blue));
        }
        for (int i = 17; i < 24; i++) {
            playableWords.add(new PlayableWord(wordsForThisGame.get(i), Color.white));
        }
        playableWords.add(new PlayableWord(wordsForThisGame.get(24), Color.black));
        Collections.shuffle(playableWords);
        return playableWords;
    }

    private void divideOnTeams() {
        short peopleInTeam = (short)(this.room.amountOfPlayers / 2);
        Set<Integer> busyUsers = new HashSet<Integer>();
        int counter = 0;
        if (this.room.amountOfPlayers % 2 == 1) {
            int marked = (int)Math.random()*this.room.amountOfPlayers;
            if (Math.random() > 0.5) {
                this.room.userList.get(marked).curTeam = Color.red;
            } else {
                this.room.userList.get(marked).curTeam = Color.blue;
            }
            busyUsers.add(marked);
        }
        while (counter < peopleInTeam) {
            int marked = (int)Math.random()*this.room.amountOfPlayers;
            if (!busyUsers.contains(marked)) {
                this.room.userList.get(marked).curTeam = Color.red;
                busyUsers.add(marked);
                counter++;
            } else {
                continue;
            }
        }
        counter = 0;
        while (counter < peopleInTeam) {
            int marked = (int)Math.random()*this.room.amountOfPlayers;
            if (!busyUsers.contains(marked)) {
                this.room.userList.get(marked).curTeam = Color.blue;
                busyUsers.add(marked);
                counter++;
            } else {
                continue;
            }
        }
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
