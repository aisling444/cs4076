package org.openjfx.lab3;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class FakeServer {

   private final Map<String, ScheduleEntry> scheduleMap = new LinkedHashMap<>();

   

    public String handle(String request) {
        String[] parts = request.split("\\|", -1);
        if (parts.length != 5) {
            return "ERROR|BAD_FORMAT";
        }

        String action = parts[0];
        String dateStr = parts[1];
        String time = parts[2];
        String room = parts[3];
        String module = parts[4];

        switch (action) {
            case "Display":
                return "OK|Display";

            case "Other":
                return "OK|OTHER_NOT_IMPLEMENTED";

            case "Add": {
                LocalDate date = parseDate(dateStr);
                if (date == null || time.isBlank() || room.isBlank() || module.isBlank()) {
                    return "ERROR|MISSING_FIELDS";
                }

                String key = key(date, time);
                if (scheduleMap.containsKey(key)) {
                    return "ERROR|CLASH";
                }

                scheduleMap.put(key, new ScheduleEntry(date, time, room, module));
                return "OK|ADDED";
            }

            case "Removed": {
                LocalDate date = parseDate(dateStr);
                if (date == null || time.isBlank()) {
                    return "ERROR|MISSING_FIELDS";
                }

                String key = key(date, time);
                if (!scheduleMap.containsKey(key)) {
                    return "ERROR|NOT_FOUND";
                }

                scheduleMap.remove(key);
                return "OK|REMOVED";
            }

            default:
                return "ERROR|UNKNOWN_ACTION";
        }
    }

    public ObservableList<ScheduleEntry> getSchedule() {
        return FXCollections.observableArrayList(scheduleMap.values());
    }

    private String key(LocalDate date, String time) {
        return date.toString() + "|" + time;
    }

    private LocalDate parseDate(String s) {
        try {
            if (s == null || s.isBlank()) return null;
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}