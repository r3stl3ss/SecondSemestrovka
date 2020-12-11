import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameRoom {
    //класс, являющийся самой комнатой

    //номер комнаты
    int number;
    //лист юзеров данной комнаты
    List<GameServer> userList = Collections.synchronizedList(new ArrayList<>());
    //история сообщений комнаты
    Story story = new Story();
    //количество людей для игры
    Short amountOfPlayers = null;

    //конструктор комнаты (лист сам создаётся, нам нужен только номер)
    public GameRoom(int number) {
        this.number = number;
    }
}
