import com.sun.xml.internal.ws.api.model.wsdl.WSDLOutput;

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
    private boolean isCap = false;
    private boolean canWrite = true;

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
        short room;
        try {
            room = Short.parseShort(in.readLine());
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
                    // начинаем подготовку к игре
                    if (this.room.userList.size() == this.room.amountOfPlayers) {
                        this.room.playingRightNow = true;
                        ArrayList<String> justWords = new ArrayList<>();
                        fillByWords(justWords);
                        //Iterator<String> iter = justWords.iterator();
                        /*while (iter.hasNext()) { //генерация слов для игры
                            sendAll(iter.next());
                        }*/
                        ArrayList<PlayableWord> wordsForGame = makeWordsPlayable(justWords);
                        if (this.room.redTeam.isEmpty() && this.room.blueTeam.isEmpty()) {
                            divideOnTeams(this.room.redTeam, this.room.blueTeam, wordsForGame);
                        }
                        if (this.isCap) {
                            this.canWrite = false;
                        }
                        /*sendRedWords(wordsForGame);
                        sendBlueWords(wordsForGame);
                        sendKiller(wordsForGame);*/
                        // здесь игровой функционал
                        byte redWords = 0;
                        for (PlayableWord pw: wordsForGame) {
                            if (pw.currentColor == Color.red){
                                redWords++;
                            }
                        }
                        if (redWords == 9) {
                            this.room.redCapCanPrint = true;
                            while (this.room.playingRightNow) {
                                message = in.readLine();
                                if (message.startsWith("/confirm ")) {
                                    PlayableWord word = checkWord(message, wordsForGame);
                                    if (word.currentColor == Color.black) {
                                        sendReds("Вы проиграли - это слово было убийцей.");
                                        sendBlues("Вы выиграли - они нажали на убийцу.");
                                        break;
                                    } else if (word.currentColor == Color.white) {
                                        sendReds("Это слово было нейтральным - ход оппонентов.");
                                        sendBlues("Слово было нейтральным - ваш ход.");
                                        wordsForGame.remove(word);
                                        this.room.blueCapCanPrint = true;
                                        continue;
                                    } else if (word.currentColor == Color.blue) {
                                        sendReds("Это слово было вашим - вы можете выбрать ещё.");
                                        sendBlues("Они выбрали своё слово и теперь будут выбирать ещё.");
                                        wordsForGame.remove(word);
                                        continue;
                                    } else if (word.currentColor == Color.red) {
                                        sendReds("Это было вражеское слово! Теперь они ходят.");
                                        sendBlues("Вам везёт - они выбрали ваше слово. Вы ходите.");
                                        wordsForGame.remove(word);
                                        this.room.blueCapCanPrint = true;
                                        continue;
                                    }
                                }
                                sendAllInGame(message);
                            }
                        } else {
                            this.room.blueCapCanPrint = true;
                            while (this.room.playingRightNow) {
                                message = in.readLine();
                                if (message.startsWith("/confirm ")) {
                                    PlayableWord word = checkWord(message, wordsForGame);
                                    if (word.currentColor == Color.black) {
                                        sendBlues("Вы проиграли - это слово было убийцей.");
                                        sendReds("Вы выиграли - они нажали на убийцу.");
                                        break;
                                    } else if (word.currentColor == Color.white) {
                                        sendBlues("Это слово было нейтральным - ход оппонентов.");
                                        sendReds("Слово было нейтральным - ваш ход.");
                                        wordsForGame.remove(word);
                                        this.room.redCapCanPrint = true;
                                        continue;
                                    } else if (word.currentColor == Color.blue) {
                                        sendBlues("Это слово было вашим - вы можете выбрать ещё.");
                                        sendReds("Они выбрали своё слово и теперь будут выбирать ещё.");
                                        wordsForGame.remove(word);
                                        continue;
                                    } else if (word.currentColor == Color.red) {
                                        sendBlues("Это было вражеское слово! Теперь они ходят.");
                                        sendReds("Вам везёт - они выбрали ваше слово. Вы ходите.");
                                        wordsForGame.remove(word);
                                        this.room.redCapCanPrint = true;
                                        continue;
                                    }
                                }
                                sendAllInGame(message);
                            }
                        }
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

    private void sendBlues(String msg) {
        for (ServerSomething ss : this.room.userList) {  // отправляем всем в комнате юзера
            if (ss.curTeam == Color.blue) {
                ss.send(msg);
            }
        }
    }

    private void sendReds(String msg) {
        for (ServerSomething ss : this.room.userList) {  // отправляем всем в комнате юзера
            if (ss.curTeam == Color.red) {
                ss.send(msg);
            }
        }
    }

    private void sendAllInGame(String msg) {
        for (ServerSomething vr : this.room.userList) {// отправляем всем в комнате юзера
            String prefixes = "";
            if (this.isCap && this.curTeam == Color.red && this.room.redCapCanPrint) {
                prefixes+= "(Капитан)(Красный) ";
                this.room.redCapCanPrint = false;
                vr.send(prefixes + msg);
            } else if (this.isCap && this.curTeam == Color.blue && this.room.blueCapCanPrint) {
                prefixes+= "(Капитан)(Синий) ";
                this.room.blueCapCanPrint = false;
                vr.send(prefixes + msg);
            } else if (this.isCap && this.curTeam == Color.red) {

            } else if (this.curTeam == Color.red) {
                prefixes+= "(Красный) ";
                vr.send(prefixes + msg);
            } else {
                prefixes += "(Синий) ";
                vr.send(prefixes + msg);
            }
        }
    }

    private PlayableWord checkWord(String message, ArrayList<PlayableWord> words) {
        String parsedMessage = message.substring(9);
        for (PlayableWord pw: words) {
            if (parsedMessage.equals(pw.word)) {
                return pw;
            }
        }
        return null;
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
        Collections.shuffle(wordsForThisGame);
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

    private void divideOnTeams(List<ServerSomething> redTeam, List<ServerSomething> blueTeam, ArrayList<PlayableWord> wordsForGame) {
        if (!redTeam.isEmpty()) {
            return;
        } else {
            short peopleInTeam = (short) (this.room.amountOfPlayers / 2);
            Set<Short> busyUsers = new HashSet<Short>();
            if (this.room.amountOfPlayers % 2 == 1) {
                short marked = (short) (Math.random() * this.room.amountOfPlayers);
                if (Math.random() > 0.5) { // если нечётное кол-во человек, рандомно определяем, куда пойдёт лишний
                    this.room.userList.get(marked).curTeam = Color.red;
                } else {
                    this.room.userList.get(marked).curTeam = Color.blue;
                }
                busyUsers.add(marked);
            }
            while (redTeam.size() < peopleInTeam) { // наполняем команду красных
                short marked = (short) (Math.random() * this.room.amountOfPlayers);
                if (!busyUsers.contains(marked)) {
                    busyUsers.add(marked);
                    this.room.userList.get(marked).curTeam = Color.red;
                    redTeam.add(this.room.userList.get(marked));
                }
            }
            while (blueTeam.size() < peopleInTeam) { // наполняем команду синих
                short marked = (short) (Math.random() * this.room.amountOfPlayers);
                if (!busyUsers.contains(marked)) {
                    busyUsers.add(marked);
                    this.room.userList.get(marked).curTeam = Color.blue;
                    blueTeam.add(this.room.userList.get(marked));
                }
            }
            blueTeam.get((short) (Math.random() * (blueTeam.size()))).isCap = true; // рандомно генерируем кэпов
            redTeam.get((short) (Math.random() * (redTeam.size()))).isCap = true;
            for (ServerSomething ss : this.room.userList) {
                if (ss.curTeam == Color.blue && ss.isCap) {
                    ss.send("Вы капитан синих.");
                } else if (ss.curTeam == Color.red && ss.isCap) {
                    ss.send("Вы капитан красных.");
                } else if (ss.curTeam == Color.red) {
                    ss.send("Вы красный и вы отгадываете");
                } else if (ss.curTeam == Color.blue) {
                    ss.send("Вы синий и вы отгадываете");
                }
            }
            String row = "";
            for (ServerSomething ss : this.room.userList) {
                if (ss.isCap) {
                    ss.send("Таблица слов на эту игру: \n");
                    for (int i = 0; i < 25; i++) {
                        if (wordsForGame.get(i).currentColor == Color.red) {
                            row += "к ";
                        } else if (wordsForGame.get(i).currentColor == Color.blue) {
                            row += "c ";
                        } else if (wordsForGame.get(i).currentColor == Color.white) {
                            row += "б ";
                        } else {
                            row += "ч ";
                        }
                        if (i % 5 == 4) {
                            ss.send(row);
                            row = "";
                        }
                    }
                }
            }
        }
    }


    //три метода чисто для теста. nevermind
    private void sendBlueWords(ArrayList<PlayableWord> wordList) {
        for (ServerSomething ss: this.room.userList) {
            if (ss.curTeam == Color.BLUE) {
                for (PlayableWord pw: wordList) {
                    if (pw.currentColor == Color.BLUE) {
                        ss.send("Синее слово: " + pw.printWord(pw));
                    }
                }
            }
        }
    }

    private void sendRedWords(ArrayList<PlayableWord> wordList) {
        for (ServerSomething ss: this.room.userList) {
            if (ss.curTeam == Color.RED) {
                for (PlayableWord pw: wordList) {
                    if (pw.currentColor == Color.RED) {
                        ss.send("Красное слово: " + pw.printWord(pw));
                    }
                }
            }
        }
    }

    private void sendKiller(ArrayList<PlayableWord> wordList) {
        for (PlayableWord pw: wordList) {
            if (pw.currentColor == Color.BLACK) {
                sendAll("КИБОРГ-УБИЙЦА: " + pw.printWord(pw));
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
