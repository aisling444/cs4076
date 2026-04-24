package com.lecturesched.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Schedule {

    public static final int MAX_MODULES = 5;

    private final String courseCode;
    private final Map<String, Lecture> timeSlots = new HashMap<>();
    private final Map<String, String>  roomSlots = new HashMap<>();

    public Schedule(String courseCode) {
        this.courseCode = courseCode;
    }

    public synchronized String checkClash(LocalDate date, String time, String room) {
        if (timeSlots.containsKey(date + "|" + time)) {
            return courseCode + " already has '" + timeSlots.get(date + "|" + time).getModule() + "' scheduled at " + date + " " + time + ".";
        }
        if (roomSlots.containsKey(date + "|" + time + "|" + room)) {
            return "Room " + room + " is already booked at " + date + " " + time + ".";
        }
        return null;
    }

    public synchronized String checkModuleCap(String module) {
        long distinct = timeSlots.values().stream().map(Lecture::getModule).distinct().count();
        boolean known = timeSlots.values().stream().anyMatch(l -> l.getModule().equalsIgnoreCase(module));
        if (!known && distinct >= MAX_MODULES) {
            return "Maximum of " + MAX_MODULES + " modules reached for " + courseCode + ".";
        }
        return null;
    }

    public synchronized void addLecture(LocalDate date, String time, String room, String module) {
        Lecture lec = new Lecture(date, time, room, module);
        timeSlots.put(lec.timeKey(), lec);
        roomSlots.put(lec.roomKey(), module);
    }

    public synchronized Lecture removeLecture(LocalDate date, String time) {
        Lecture lec = timeSlots.remove(date + "|" + time);
        if (lec != null) {
            roomSlots.remove(lec.roomKey());
        }
        return lec;
    }

    public synchronized String getScheduleAsString() {
        List<Lecture> sorted = new ArrayList<>(timeSlots.values());
        sorted.sort(Comparator.comparing(Lecture::getDate).thenComparing(Lecture::getTime));
        StringBuilder sb = new StringBuilder();
        for (Lecture l : sorted) {
            sb.append(l.getDate()).append(",")
              .append(l.getTime()).append(",")
              .append(l.getRoom()).append(",")
              .append(l.getModule()).append(";");
        }
        return sb.toString();
    }

    public synchronized List<Lecture> getSnapshot() {
        return new ArrayList<>(timeSlots.values());
    }

    public synchronized int applyEarlyShifts(List<Lecture> shiftedLectures) {
        int count = 0;
        for (Lecture shifted : shiftedLectures) {
            Lecture original = null;
            for (Lecture live : new ArrayList<>(timeSlots.values())) {
                if (live.getDate().equals(shifted.getDate())
                        && live.getModule().equals(shifted.getModule())
                        && live.getRoom().equals(shifted.getRoom())) {
                    original = live;
                    break;
                }
            }
            if (original != null) {
                timeSlots.remove(original.timeKey());
                roomSlots.remove(original.roomKey());
                timeSlots.put(shifted.timeKey(), shifted);
                roomSlots.put(shifted.roomKey(), shifted.getModule());
                count++;
            }
        }
        return count;
    }

    public String getCourseCode() { return courseCode; }
}
