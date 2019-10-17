package com.company;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            boolean start = true;
            while (true) {
                if (!start) break;
                System.out.println("Enter your login: ");
                String login = scanner.nextLine();
                System.out.println("Enter your password: ");
                String password = scanner.nextLine();

                User yourData = new User(login, password);
                int checking = yourData.send(Utils.getURL() + "/regist");
                if (checking == 200) {
                    yourData.setPresent("online");
                    System.out.println("You've successfully log in.");
                    GetThread gt = new GetThread(yourData.getLogin(), checkMyChatName(yourData.getLogin()));
                    Thread th = new Thread(gt);
                    th.setDaemon(true);
                    th.start();
                    while (true) {
                        String forLogout = "";
                        System.out.println("Choose an option(enter the number): 1-send message, 2-show list of users, " +
                                "3-chat room options, 4-exit the application.");
                        String option = scanner.nextLine();
                        if (option.equals("1")) {
                            System.out.println("Choose message type(enter the number): 1-public message, 2-private message");
                            String messageType = scanner.nextLine();
                            if (messageType.equals("1")) {
                                System.out.println("Message for all users: ");
                                while (true) {
                                    String text = scanner.nextLine();
                                    if (text.isEmpty()) break;
                                    Message m = new Message(login, text);
                                    int res = m.send(Utils.getURL() + "/add");
                                    if (res != 200) { // 200 OK
                                        System.out.println("HTTP error occured: " + res);
                                        return;
                                    }
                                }
                            } else if (messageType.equals("2")) {
                                while (true) {
                                    System.out.println("Enter recipient login: ");
                                    String recLogin = scanner.nextLine();
                                    if (recLogin.isEmpty()) break;
                                    else if (recLogin.equals(yourData.getLogin())) {
                                        System.out.println("not your login, try again.");
                                        continue;
                                    }
                                    User recipient = new User(recLogin, "privateMessage");
                                    int checkingRec = recipient.send(Utils.getURL() + "/regist");
                                    if (checkingRec == 200) {
                                        System.out.println("Message for " + recipient.getLogin() + ": ");
                                        while (true) {
                                            String text = scanner.nextLine();
                                            if (text.isEmpty()) break;

                                            Message m = new Message(yourData.getLogin(), text, recipient.getLogin());
                                            int res = m.send(Utils.getURL() + "/add");

                                            if (res != 200) { // 200 OK
                                                System.out.println("HTTP error occured: " + res);
                                                return;
                                            }
                                        }
                                    } else {
                                        System.out.println("User " + recLogin + " is not registered, try again.");
                                    }
                                }
                            } else {
                                continue;
                            }
                        } else if (option.equals("2")) {
                            URL url = new URL(Utils.getURL() + "/users");
                            HttpURLConnection http = (HttpURLConnection) url.openConnection();
                            http.setRequestMethod("GET");
                            InputStream is = http.getInputStream();
                            try {
                                byte[] buf = requestBodyToArray(is);
                                String strBuf = new String(buf, StandardCharsets.UTF_8);
                                UsersList ul = UsersList.fromJSON(strBuf);
                                ul.show();
                            } finally {
                                is.close();
                            }
                        } else if (option.equals("3")) {
                            while (true) {
                                System.out.println("1-create chat, 2-delete chat, 3-log out chat, 4-log in chat.");
                                option = scanner.nextLine();
                                if (option.isEmpty()) break;
                                if (option.equals("1")) {
                                    System.out.println("Enter a chat name: ");
                                    String chatName = scanner.nextLine();
                                    forLogout = chatName;
                                    if (chatName.isEmpty()) continue;
                                    System.out.println("Enter a number of participants(except you): ");
                                    int number = Integer.parseInt(scanner.nextLine());
                                    for (int i = 0; i <= number; i++) {
                                        if (i == 0) {
                                            yourData.setChatName(chatName);
                                            checking = yourData.send(Utils.getURL() + "/regist");
                                            if (checking == 200) {
                                                gt.setMyChatName(chatName);
                                                continue;
                                            } else {
                                                System.out.println("HTTP error occured: " + checking);
                                                return;
                                            }
                                        }
                                        System.out.println("Enter a login of " + i + " participant:");
                                        String recLogin;
                                        while (true) {
                                            while (true) {
                                                recLogin = scanner.nextLine();
                                                if (!recLogin.equals(yourData.getLogin())) break;
                                                else if (recLogin.isEmpty()) continue;
                                                System.out.println("Not your login: ");
                                            }
                                            User recipient = new User(recLogin, "createChat", chatName);
                                            checking = recipient.send(Utils.getURL() + "/regist");
                                            if (checking == 200) break;
                                            else System.out.println("User " + recLogin + " not registered, try again.");
                                        }
                                    }
                                    System.out.println("The chat " + chatName + " successfully created.");
                                    System.out.println("Enter your message for " + chatName + " participants: ");
                                    while (true) {
                                        String text = scanner.nextLine();
                                        if (text.isEmpty()) break;
                                        Message m = new Message(login, text, chatName);
                                        int res = m.send(Utils.getURL() + "/add");
                                        if (res != 200) { // 200 OK
                                            System.out.println("HTTP error occured: " + res);
                                            return;
                                        }
                                    }
                                } else if (option.equals("2")) {
                                    while (true) {
                                        System.out.println("Enter chat name you want to delete: ");
                                        String chatName = scanner.nextLine();
                                        if (chatName.isEmpty()) break;
                                        URL url = new URL(Utils.getURL() + "/regist?deleteChat=" + chatName);
                                        HttpURLConnection http = (HttpURLConnection) url.openConnection();
                                        http.setRequestMethod("GET");
                                        int res = http.getResponseCode();
                                        if (res == 200) {
                                            System.out.println("The chat " + chatName + " successfully deleted.");
                                            gt.setMyChatName("no chat");
                                            break;
                                        } else {
                                            System.out.println("There is no " + chatName + " chat.");
                                        }
                                    }
                                } else if (option.equals("3")) {
                                    URL url = new URL(Utils.getURL() + "/regist?login=" + yourData.getLogin());
                                    HttpURLConnection http = (HttpURLConnection) url.openConnection();
                                    http.setRequestMethod("GET");
                                    int res = http.getResponseCode();
                                    if (res == 200) {
                                        System.out.println("You've left " + forLogout + " chat.");
                                        gt.setMyChatName("no chat");
                                    } else if (res == 400 ) {
                                        System.out.println("You don't participete in any chat.");
                                    } else {
                                        System.out.println("HTTP error occured: " + res);
                                        return;
                                    }
                                } else if (option.equals("4")) {
                                    while (true) {
                                        System.out.println("Enter a chat you want login: ");
                                        String chatName = scanner.nextLine();
                                        if (chatName.isEmpty()) break;
                                        URL url = new URL(Utils.getURL() + "/regist?chatNameForLogIn=" + chatName +
                                                "&login=" + yourData.getLogin());
                                        HttpURLConnection http = (HttpURLConnection) url.openConnection();
                                        http.setRequestMethod("GET");
                                        int res = http.getResponseCode();
                                        if (res == 200) {
                                            System.out.println("You are in a " + chatName + " chat.");
                                            gt.setMyChatName(chatName);
                                            System.out.println("Enter your message for " + chatName + " participants: ");
                                            while (true) {
                                                String text = scanner.nextLine();
                                                if (text.isEmpty()) break;
                                                Message m = new Message(login, text, chatName);
                                                int response = m.send(Utils.getURL() + "/add");
                                                if (response != 200) { // 200 OK
                                                    System.out.println("HTTP error occured: " + res);
                                                    return;
                                                }
                                            }
                                            break;
                                        } else {
                                            System.out.println("There is no " + chatName + " chat, try again.");
                                        }
                                    }
                                }
                            }
                        } else if (option.equals("4")) {
                            yourData.setPresent("exiting");
                            checking = yourData.send(Utils.getURL() + "/regist");
                            if (checking == 200) {
                                start = false;
                                break;
                            }
                        }
                    }
                } else {
                    System.out.println("Wrong user's data, try again.");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private static byte[] requestBodyToArray(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[10240];
        int r;

        do {
            r = is.read(buf);
            if (r > 0) bos.write(buf, 0, r);
        } while (r != -1);

        return bos.toByteArray();
    }

    private static String checkMyChatName(String login) throws IOException {
        URL url = new URL(Utils.getURL() + "/check?login=" + login);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("GET");
        InputStream is = http.getInputStream();
        try {
            byte[] buf = requestBodyToArray(is);
            String chatName = new String(buf, StandardCharsets.UTF_8);
            return chatName;
        } finally {
            is.close();
        }
    }
}
