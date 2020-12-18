import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Room {
    //класс, являющийся самой комнатой

    //номер комнаты
    short number;
    //лист юзеров данной комнаты
    List<ServerSomething> userList = Collections.synchronizedList(new ArrayList<>());
    //история сообщений комнаты
    Story story = new Story();
    //количество людей для игры
    Short amountOfPlayers = null;
    //играет ли комната
    Boolean playingRightNow = false;

    //конструктор комнаты (лист сам создаётся, нам нужен только номер)
    public Room(short number) {
        this.number = number;
    }
}
