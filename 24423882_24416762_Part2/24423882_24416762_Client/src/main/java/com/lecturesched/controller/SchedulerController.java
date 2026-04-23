package com.lecturesched.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.lecturesched.model.Lecture;
import com.lecturesched.view.SchedulerView;
import com.lecturesched.view.SchedulerView.StatusType;
public class SchedulerController {

    private static final String HOST = "localhost";
    private static final int PORT = 5678;
    private final SchedulerView view;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;
    public SchedulerController(SchedulerView view) {
        this.view = view;
        attachHandlers();
        connectToServer();
    }
    private void attachHandlers() {
        view.getSendBtn().setOnAction(e -> onSend());
        view.getStopBtn().setOnAction(e -> onStop());
        view.getClearBtn().setOnAction(e -> onClear());
    }
    private void connectToServer() {
        try {
            socket = new Socket(HOST, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);
            connected = true;
            view.log("--- Connected to server at " + HOST + ":" + PORT + " ---");
            view.setStatus("Connected", StatusType.OK);
        } catch (IOException e) {
            connected = false;
            view.setStatus("Cannot connect to server", StatusType.ERROR);
            view.log("--- ERROR: Could not connect to server. Is it running on port " + PORT + "? ---");
            view.getSendBtn().setDisable(true);
            view.getStopBtn().setDisable(true);
        }
    }
    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
        connected = false;
    }
    private void onSend() {
        if (!connected) {
            view.showInfo("Not connected",
                    "No connection to server. Press Clear to reconnect.");
            return;
        }
        String action = view.getActionBox().getValue();
        LocalDate date = view.getDatePicker().getValue();
        String time = view.getTimeBox().getValue();
        String room = view.getRoomField().getText().trim();
        String module = view.getModuleField().getText().trim();
        String request = buildRequest(action, date, time, room, module);
        if (request == null) return;
        view.log("CLIENT> " + request);
        String response = sendToServer(request);
        if (response == null) return;
        view.log("SERVER> " + response);
        processResponse(response);
    }
    private void onStop() {
        if (!connected) return;
        String request = "STOP||||";
        view.log("CLIENT> " + request);
        String response = sendToServer(request);
        if (response != null) view.log("SERVER> " + response);
        closeConnection();
        view.getSendBtn().setDisable(true);
        view.setStatus("TERMINATED (STOP sent)", StatusType.TERMINATED);
    }
    private void onClear() {
        view.resetForm();
        view.clearLog();
        view.getSendBtn().setDisable(false);
        view.getStopBtn().setDisable(false);
        closeConnection();
        connectToServer();
    }
    private String buildRequest(String action, LocalDate date, String time, String room, String module) {
        String d = (date == null) ? "" : date.toString();
        return switch (action) {
            case "ADD" -> {
                if (d.isEmpty() || time.isEmpty() || room.isEmpty() || module.isEmpty()) {
                    view.showWarning("Validation", "ADD requires Date, Time Slot, Room and Module.");
                    yield null;
                }
                yield "ADD|" + d + "|" + time + "|" + room + "|" + module;
            }
            case "REMOVE" -> {
                if (d.isEmpty() || time.isEmpty()) {
                    view.showWarning("Validation", "REMOVE requires Date and Time Slot.");
                    yield null;
                }
                yield "REMOVE|" + d + "|" + time + "||";
            }
            case "DISPLAY"          -> "DISPLAY||||";
            case "EARLY LECTURES"   -> "EARLY||||";   // early lectures request
            default                 -> "OTHER||||";   // triggers IncorrectActionException
        };
    }
    private String sendToServer(String request) {
        try {
            out.println(request);
            return in.readLine();
        } catch (IOException e) {
            view.log("--- ERROR: Lost connection to server ---");
            view.setStatus("Connection lost", StatusType.ERROR);
            connected = false;
            view.getSendBtn().setDisable(true);
            return null;
        }
    }
    private void processResponse(String response) {
        if (response == null) {
            view.setStatus("No response from server", StatusType.ERROR);
            return;
        }
        if (response.startsWith("OK|")) {
            String payload = response.substring(3);
            if (payload.startsWith("SCHEDULE:")) {
                parseAndDisplaySchedule(payload.substring(9));
                view.setStatus("Schedule displayed", StatusType.OK);
            } else if (payload.startsWith("EARLY:")) {
                String earlyPayload = payload.substring(6);
                String statusMsg;
                String scheduleData = null;
                int schedIdx = earlyPayload.indexOf("|SCHEDULE:");
                if (schedIdx >= 0) {
                    statusMsg    = earlyPayload.substring(0, schedIdx);
                    scheduleData = earlyPayload.substring(schedIdx + 10);
                } else {
                    statusMsg = earlyPayload;
                }
                view.setStatus(statusMsg, StatusType.OK);
                view.showInfo("Early Lectures Result", statusMsg);
                if (scheduleData != null) {
                    parseAndDisplaySchedule(scheduleData);
                }
            } else {
                view.setStatus(payload, StatusType.OK);
            }
        } else if (response.startsWith("ERROR|")) {
            String msg = response.substring(6);
            view.setStatus(msg, StatusType.ERROR);
            view.showWarning("Server Error", msg);
        } else if (response.startsWith("TERMINATE|")) {
            closeConnection();
            view.getSendBtn().setDisable(true);
            view.setStatus("TERMINATED", StatusType.TERMINATED);
        }
    }
    private void parseAndDisplaySchedule(String data) {
        List<Lecture> lectures = new ArrayList<>();
        if (!data.equals("EMPTY") && !data.isBlank()) {
            for (String entry : data.split(";")) {
                String[] f = entry.split(",", -1);
                if (f.length == 4) {
                    try {
                        lectures.add(new Lecture(
                                LocalDate.parse(f[0].trim()),
                                f[1].trim(), f[2].trim(), f[3].trim()));
                    } catch (Exception ignored) { }
                }
            }
        }
        view.refreshTable(lectures);
    }
}
