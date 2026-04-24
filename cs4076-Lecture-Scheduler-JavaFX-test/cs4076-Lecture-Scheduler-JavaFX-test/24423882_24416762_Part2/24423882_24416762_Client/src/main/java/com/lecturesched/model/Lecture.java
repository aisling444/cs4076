package com.lecturesched.model;

import java.time.LocalDate;

public class Lecture {
    private final LocalDate date;
    private final String time;
    private final String room;
    private final String module;
    public Lecture(LocalDate date, String time, String room, String module) {
        this.date = date;
        this.time = time;
        this.room = room;
        this.module = module;
    }
    public LocalDate getDate() { return date; }
    public String getTime() { return time; }
    public String getRoom() { return room; }
    public String getModule() { return module; }
    public String slotKey() {
        return date + "|" + time;
    }

    @Override
    public String toString() {
        return date + " " + time + " Room " + room + " (" + module + ")";
    }
}
