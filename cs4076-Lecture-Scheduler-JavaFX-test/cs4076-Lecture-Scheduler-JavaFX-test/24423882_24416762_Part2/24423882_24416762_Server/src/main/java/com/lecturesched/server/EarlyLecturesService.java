package com.lecturesched.server;

import com.lecturesched.model.Lecture;
import com.lecturesched.model.Schedule;
import javafx.concurrent.Task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class EarlyLecturesService extends Task<String> {

    private static final List<String> MORNING_SLOTS = List.of("09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00");

    private final Schedule schedule;

    public EarlyLecturesService(Schedule schedule) {
        this.schedule = schedule;
    }

    @Override
    protected String call() {
        List<Lecture> snapshot = schedule.getSnapshot();
        if (snapshot.isEmpty()) {
            return "OK|EARLY:No lectures scheduled to shift.";
        }

        List<LocalDate> daysToProcess = new ArrayList<>();
        for (Lecture l : snapshot) {
            if (!MORNING_SLOTS.contains(l.getTime()) && !daysToProcess.contains(l.getDate())) {
                daysToProcess.add(l.getDate());
            }
        }

        if (daysToProcess.isEmpty()) {
            return "OK|EARLY:All lectures are already in morning slots.";
        }

        ForkJoinPool pool = new ForkJoinPool();
        EarlyLecturesTask rootTask = new EarlyLecturesTask(snapshot, daysToProcess);
        List<Lecture> shiftedLectures;
        try {
            shiftedLectures = pool.invoke(rootTask);
        } finally {
            pool.shutdown();
        }

        if (shiftedLectures.isEmpty()) {
            return "OK|EARLY:No lectures could be shifted (morning slots already occupied).";
        }

        int count = schedule.applyEarlyShifts(shiftedLectures);
        String updatedSchedule = schedule.getScheduleAsString();
        String prefix = "OK|EARLY:" + count + " lecture(s) shifted to morning slots.|SCHEDULE:";
        return updatedSchedule.isEmpty() ? prefix + "EMPTY" : prefix + updatedSchedule;
    }
}
