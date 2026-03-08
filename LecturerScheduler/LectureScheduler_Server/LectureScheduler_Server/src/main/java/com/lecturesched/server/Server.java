package com.lecturesched.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;

import com.lecturesched.exception.IncorrectActionException;
import com.lecturesched.model.Lecture;
import com.lecturesched.model.Schedule;

public class Server {

    private static final int    PORT   = 5678;
    private static final String COURSE = "LM051-2026";

    private static ServerSocket servSock;
    private static int clientConnections = 0;

    private static final Schedule schedule = new Schedule(COURSE);

    public static void main(String[] args) {
        System.out.println("=== Lecture Scheduler Server ===");
        System.out.println("Course  : " + COURSE);
        System.out.println("Port    : " + PORT);
        System.out.println("Opening port...\n");

        try {
            servSock = new ServerSocket(PORT);
        } catch (IOException e) {
            System.out.println("Unable to attach to port " + PORT + "!");
            System.exit(1);
        }

        // Accept clients indefinitely
        while (true) {
            handleClient();
        }
    }

    private static void handleClient() {
        Socket link = null;
        try {
            link = servSock.accept();
            clientConnections++;
            System.out.println("Client " + clientConnections + " connected from "
                    + link.getInetAddress() + "\n");

            BufferedReader in  = new BufferedReader(
                    new InputStreamReader(link.getInputStream()));
            PrintWriter    out = new PrintWriter(link.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("RECV: " + message);
                String response = processRequest(message);
                System.out.println("SEND: " + response + "\n");
                out.println(response);

                if (response.startsWith("TERMINATE")) {
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (link != null) {
                    System.out.println("* Closing connection with client "
                            + clientConnections + " *\n");
                    link.close();
                }
            } catch (IOException e) {
                System.out.println("Unable to close connection!");
                System.exit(1);
            }
        }
    }

    private static String processRequest(String message) {
        String[] parts = message.split("\\|", -1);
        String action = parts[0].trim().toUpperCase();

        try {
            switch (action) {

                case "STOP":
                    return "TERMINATE|Server confirms termination.";

                case "ADD": {
                    if (parts.length < 5) throw new IncorrectActionException(
                            "ADD requires 5 fields: ADD|DATE|TIME|ROOM|MODULE");

                    LocalDate date   = LocalDate.parse(parts[1].trim());
                    String    time   = parts[2].trim();
                    String    room   = parts[3].trim();
                    String    module = parts[4].trim();

                    // Module cap check
                    String capError = schedule.checkModuleCap(module);
                    if (capError != null) return "ERROR|" + capError;

                    // Clash check
                    String clash = schedule.checkClash(date, time, room);
                    if (clash != null) return "ERROR|Clash: " + clash;

                    schedule.addLecture(date, time, room, module);
                    return "OK|Added: " + module + " on " + date
                            + " at " + time + " in room " + room;
                }

                case "REMOVE": {
                    if (parts.length < 3) throw new IncorrectActionException(
                            "REMOVE requires: REMOVE|DATE|TIME");

                    LocalDate date = LocalDate.parse(parts[1].trim());
                    String    time = parts[2].trim();

                    Lecture removed = schedule.removeLecture(date, time);
                    if (removed == null) {
                        return "ERROR|No lecture found at " + date + " " + time + ".";
                    }
                    return "OK|Removed: " + removed.getModule()
                            + " on " + date + " at " + time
                            + " in room " + removed.getRoom();
                }

                case "DISPLAY": {
                    String data = schedule.getScheduleAsString();
                    if (data.isEmpty()) {
                        return "OK|SCHEDULE:EMPTY";
                    }
                    return "OK|SCHEDULE:" + data;
                }

                default:
                    throw new IncorrectActionException(
                            "Unknown action '" + parts[0] + "'. "
                            + "Valid actions: ADD, REMOVE, DISPLAY, STOP.");
            }

        } catch (IncorrectActionException e) {
            System.out.println("[IncorrectActionException] " + e.getMessage());
            return "ERROR|IncorrectActionException: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR|Server exception: " + e.getMessage();
        }
    }
}
