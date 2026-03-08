package com.mycompany.server;

import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.util.*;

//waits for the client to connect, then handles messages in a loop
public class Server {
    private static ServerSocket servSock;
    private static final int PORT = 5678;
    private static int clientConnections = 0;
    private static final String COURSE = "LM121";

    //schedule is stored here
    private static Schedule schedule = new Schedule(COURSE);

    public static void main(String[] args) {
        System.out.println("Opening port...\n");
        try {
            servSock = new ServerSocket(PORT); //create the server socket
        } catch (IOException e) {
            System.out.println("Unable to attach to port!");
            System.exit(1);
        }
        do {
            run();
        } while (true);
    }

    private static void run() {
        Socket link = null; //prepare a socket for the client
        try {
            link = servSock.accept(); //wait for client to connect
            clientConnections++;
            System.out.println("Client " + clientConnections + " connected.\n");
            BufferedReader in  = new BufferedReader(new InputStreamReader(link.getInputStream())); //open reader
            PrintWriter    out = new PrintWriter(link.getOutputStream(), true); //open writer

            //keep reading messages until the client disconnects or sends STOP
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received from client: " + message);
                String response = processRequest(message); //what to reply
                System.out.println("Sending to client:    " + response);
                out.println(response); //send the reply
                if (response.startsWith("TERMINATE")) {
                    break; //terminate ends the loop
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                System.out.println("\n* Closing connection... *");
                link.close(); //close the connection
            } catch (IOException e) {
                System.out.println("Unable to disconnect!");
                System.exit(1);
            }
        }
    }
    //reads the action from the message and decides what to reply
    private static String processRequest(String message) {
        String[] parts = message.split("\\|", -1);
        String action = parts[0].trim().toUpperCase();
        try {
            switch (action) {
                case "STOP":
                    return "TERMINATE|Connection closed";
                case "ADD":
                    LocalDate addDate = LocalDate.parse(parts[1].trim());
                    String addTime   = parts[2].trim();
                    String addRoom   = parts[3].trim();
                    String addModule = parts[4].trim();
                    String clash = schedule.checkClash(addDate, addTime, addRoom);
                    if (clash != null) {
                        return "ERROR|Clash: " + clash;
                    }
                    schedule.addLecture(addDate, addTime, addRoom, addModule);
                    return "OK|Lecture added: " + addModule + " " + addDate + " " + addTime + " " + addRoom;
                case "REMOVE":
                    LocalDate remDate = LocalDate.parse(parts[1].trim());
                    String remTime = parts[2].trim();
                    Lecture removed = schedule.removeLecture(remDate, remTime);
                    if (removed == null) {
                        return "ERROR|No lecture found at: " + remDate + " " + remTime;
                    }
                    return "OK|Removed: " + removed.module + " " + remDate + " " + remTime + " " + removed.room;
                case "DISPLAY":
                    String scheduleStr = schedule.getScheduleAsString();
                    if (scheduleStr.isEmpty()) {
                        return "OK|SCHEDULE:empty";
                    }
                    return "OK|SCHEDULE:" + scheduleStr;
                default:
                    //custom exception for an unrecognised action
                    throw new IncorrectActionException("Unknown action: '" + action + "' Valid actions: ADD, REMOVE, DISPLAY, STOP.");
            }
        } catch (IncorrectActionException e) {
            System.out.println("[IncorrectActionException] " + e.getMessage());
            return "ERROR|IncorrectActionException: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR|Server exception: " + e.getMessage();
        }
    }
    static class IncorrectActionException extends Exception {
        public IncorrectActionException(String message) {
            super(message);
        }
    }
    static class Lecture {
        LocalDate date;
        String time, room, module;

        Lecture(LocalDate date, String time, String room, String module) {
            this.date = date; this.time = time; this.room = room; this.module = module;
        }

        String timeKey() { return date + "|" + time; }
    }
    //stores all lectures using HashMaps
    static class Schedule {
        private final String courseCode;
        private final Map<String, Lecture> timeSlots = new HashMap<>(); //key = date|time
        private final Map<String, String>  roomSlots = new HashMap<>(); // key = date|time|room
        Schedule(String courseCode) { this.courseCode = courseCode; }
        //returns a clash message, or null if no clash
        String checkClash(LocalDate date, String time, String room) {
            if (timeSlots.containsKey(date + "|" + time)) {
                return courseCode + " already has " + timeSlots.get(date + "|" + time).module + " at this time.";
            }
            if (roomSlots.containsKey(date + "|" + time + "|" + room)) {
                return "Room " + room + " is already booked at this time.";
            }
            return null;
        }
        void addLecture(LocalDate date, String time, String room, String module) {
            Lecture lec = new Lecture(date, time, room, module);
            timeSlots.put(lec.timeKey(), lec);
            roomSlots.put(date + "|" + time + "|" + room, module);
        }
        Lecture removeLecture(LocalDate date, String time) {
            Lecture lec = timeSlots.remove(date + "|" + time);
            if (lec != null) roomSlots.remove(date + "|" + time + "|" + lec.room);
            return lec;
        }
        String getScheduleAsString() {
            List<Lecture> sorted = new ArrayList<>(timeSlots.values());
            sorted.sort(Comparator.comparing((Lecture l) -> l.date).thenComparing(l -> l.time));
            StringBuilder sb = new StringBuilder();
            for (Lecture l : sorted) {
                if (sb.length() > 0) sb.append("~");
                sb.append(l.module).append(",").append(l.date).append(",").append(l.time).append(",").append(l.room);
            }
            return sb.toString();
        }
    }
}
