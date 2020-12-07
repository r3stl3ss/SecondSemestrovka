import java.util.ArrayList;
import java.util.List;

public class Room {
    //класс, являющийся самой комнатой

    //номер комнаты
    int number;
    //лист юзеров данной комнаты
    List<ServerSomething> userList = new ArrayList<>();
    //история сообщений комнаты
    Story story = new Story();
    //флажок
    Boolean flag = false;
    //количество людей для игры
    Short amountOfPlayers = null;

    //конструктор комнаты (листсам создаётся, нам нужен только номер)
    public Room(int number) {
        this.number = number;
    }
}
