public class Client {

    public static String ipAddr = "localhost";
    public static int port = 12000;
    public static void main(String[] args) {
        new ClientSomething(ipAddr, port);
    }
}