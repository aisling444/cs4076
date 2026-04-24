package com.lecturesched.server;

import com.lecturesched.exception.IncorrectActionException;
import com.lecturesched.model.Lecture;
import com.lecturesched.model.Schedule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final Schedule schedule;
    private final int clientId;

    public ClientHandler(Socket clientSocket, Schedule schedule, int clientId) {
        this.clientSocket = clientSocket;
        this.schedule = schedule;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        System.out.println("[Client " + clientId + "] Connected from " + clientSocket.getRemoteSocketAddress());
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out   = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[Client " + clientId + "] RECV: " + line);
                String response = processRequest(line);
                System.out.println("[Client " + clientId + "] SEND: " + response + "\n");
                out.println(response);
                if (response.startsWith("TERMINATE")) break;
            }
        } catch (IOException e) {
            System.out.println("[Client " + clientId + "] Connection error: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {}
            System.out.println("[Client " + clientId + "] Disconnected.");
        }
    }

    private String processRequest(String message) {
        String[] parts = message.split("\\|", -1);
        String action = parts[0].trim().toUpperCase();
        try {
            return switch (action) {
                case "STOP" -> "TERMINATE|Server confirms termination.";
                case "ADD" -> handleAdd(parts);
                case "REMOVE" -> handleRemove(parts);
                case "DISPLAY" -> handleDisplay();
                case "EARLY" -> handleEarlyLectures();
                case "EXPORT" -> handleExport();
                default -> throw new IncorrectActionException("Unknown action '" + parts[0] + "'. Valid: ADD, REMOVE, DISPLAY, EARLY, STOP.");
            };
        } catch (IncorrectActionException e) {
            System.out.println("[Client " + clientId + "] IncorrectActionException: " + e.getMessage());
            return "ERROR|IncorrectActionException: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR|Server exception: " + e.getMessage();
        }
    }

    private String handleAdd(String[] parts) throws IncorrectActionException {
        if (parts.length < 5)
            throw new IncorrectActionException("ADD requires: ADD|DATE|TIME|ROOM|MODULE");
        LocalDate date = LocalDate.parse(parts[1].trim());
        String time = parts[2].trim();
        String room = parts[3].trim();
        String module = parts[4].trim();
        String capErr = schedule.checkModuleCap(module);
        if (capErr != null) return "ERROR|" + capErr;
        String clash = schedule.checkClash(date, time, room);
        if (clash != null) return "ERROR|Clash: " + clash;
        schedule.addLecture(date, time, room, module);
        return "OK|Added: " + module + " on " + date + " at " + time + " in room " + room;
    }

    private String handleRemove(String[] parts) throws IncorrectActionException {
        if (parts.length < 3)
            throw new IncorrectActionException("REMOVE requires: REMOVE|DATE|TIME");
        LocalDate date = LocalDate.parse(parts[1].trim());
        String time = parts[2].trim();
        Lecture removed = schedule.removeLecture(date, time);
        if (removed == null)
            return "ERROR|No lecture found at " + date + " " + time + ".";
        return "OK|Removed: " + removed.getModule() + " on " + date + " at " + time + " in room " + removed.getRoom() + ". Slot " + time + " on " + date + " is now free.";
    }

    private String handleDisplay() {
        String data = schedule.getScheduleAsString();
        return data.isEmpty() ? "OK|SCHEDULE:EMPTY" : "OK|SCHEDULE:" + data;
    }
    private String handleExport() {
        String data = schedule.getScheduleAsString();
        return data.isEmpty() ? "OK|EXPORT:EMPTY" : "OK|EXPORT" + data;
    }
    private String handleEarlyLectures() {
        EarlyLecturesService task = new EarlyLecturesService(schedule);
        Thread worker = new Thread(task, "EarlyLecturesWorker-client" + clientId);
        worker.setDaemon(true);
        worker.start();
        try {
            return task.get();
        } catch (Exception e) {
            return "ERROR|Early lectures failed: " + e.getMessage();
        }
    }
}
