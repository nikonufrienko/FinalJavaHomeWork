package main;

import java.io.*;

public class LocalAccountStorage {
    public final String login;
    public final String password;
    public final String ip;
    public final String port;

    public LocalAccountStorage(String login, String password, String ip, String port) {
        this.login = login;
        this.password = password;
        this.ip = ip;
        this.port = port;
    }

    public void saveToFile() {
        try (FileWriter fileWriter = new FileWriter(new File("account.txt")) ) {
            PrintWriter writer = new PrintWriter(fileWriter);
            writer.println(login);
            writer.println(password);
            writer.println(ip);
            writer.println(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static LocalAccountStorage tryToLoad() {
        File file = new File("account.txt");
        if (file.exists()) {
            try (FileReader fileReader = new FileReader(file)) {
                BufferedReader reader = new BufferedReader(fileReader);
                String login = reader.readLine();
                String password = reader.readLine();
                String ip = reader.readLine();
                String port = reader.readLine();
                return new LocalAccountStorage(login, password, ip, port);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
