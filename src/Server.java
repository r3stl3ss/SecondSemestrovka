import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class Server {

    //Все порты до 8к заняты системой
    public static final int PORT = 12000;
    public static CopyOnWriteArrayList<ServerSomething> serverList = new CopyOnWriteArrayList<>(); // вы сказали, так хорошо. мы вам верим
    public static Story story;
    public static List<Room> roomList = Collections.synchronizedList(new ArrayList<Room>());

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        story = new Story();
        System.out.println("Server Started");
        try {
            while (true) {
                Socket socket = server.accept();
                try {
                    ServerSomething newUser = new ServerSomething(socket); // создаём коннект новичка
                    serverList.add(newUser); // Добавляем в общий пул
                } catch (IOException e) {
                    socket.close();
                }
            }
        } finally {
            server.close();
        }
    }


    //Возвращаем рум по номеру
    public static Room getRoom(short number){
        boolean hasRoom = false;
        for(Room room: roomList){
            if(room.number == number){
                //Если комната существует, то возвращаем
                return room;
            }
        }
        //если комнаты нет, то создаём
        Room newRoom = new Room(number);
        //Заносим в общ лист
        roomList.add(newRoom);
        //возвращаем
        return newRoom;
    }

    public static String roomToString(Room room) {
        String stringyRoom = "Идентификатор комнаты: " + room.number + "\nТекущее количество человек: " + room.userList.size() + "\nРазмер: " + room.amountOfPlayers + "\n";
        return stringyRoom;
    }
}