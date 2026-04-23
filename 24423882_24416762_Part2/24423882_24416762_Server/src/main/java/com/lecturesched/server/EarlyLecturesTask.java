package com.lecturesched.server;

import com.lecturesched.model.Lecture;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class EarlyLecturesTask extends RecursiveTask<List<Lecture>> {
    private static final List<String> MORNING_SLOTS = Arrays.asList(
            "09:00-10:00",
            "10:00-11:00",
            "11:00-12:00",
            "12:00-13:00"
    );
    private final List<Lecture> snapshot;
    private final List<LocalDate> days;
    public EarlyLecturesTask(List<Lecture> snapshot, List<LocalDate> days) {
        this.snapshot = snapshot;
        this.days = days;
    }
    @Override
    protected List<Lecture> compute() {
        if (days.size() == 1) {
            return shiftDay(days.get(0));
        }
        int mid = days.size() / 2;
        EarlyLecturesTask leftTask  = new EarlyLecturesTask(snapshot, days.subList(0, mid));
        EarlyLecturesTask rightTask = new EarlyLecturesTask(snapshot, days.subList(mid, days.size()));
        leftTask.fork();
        List<Lecture> rightResult = rightTask.compute();
        List<Lecture> leftResult = leftTask.join();
        List<Lecture> merged = new ArrayList<>(leftResult);
        merged.addAll(rightResult);
        return merged;
    }
    private List<Lecture> shiftDay(LocalDate day) {
        List<Lecture> toShift = new ArrayList<>();
        for (Lecture l : snapshot) {
            if (l.getDate().equals(day) && !MORNING_SLOTS.contains(l.getTime())) {
                toShift.add(l);
            }
        }
        List<String> occupiedTimes = new ArrayList<>();
        for (Lecture l : snapshot) {
            if (l.getDate().equals(day)) {
                occupiedTimes.add(l.getTime());
            }
        }
        List<Lecture> shifted   = new ArrayList<>();
        List<String>  usedSlots = new ArrayList<>(occupiedTimes);
        for (Lecture original : toShift) {
            String targetSlot = null;
            for (String candidate : MORNING_SLOTS) {
                if (usedSlots.contains(candidate)) continue;

                boolean roomFree = snapshot.stream().noneMatch(l ->
                        l.getDate().equals(day)
                        && l.getTime().equals(candidate)
                        && l.getRoom().equals(original.getRoom()));
                if (roomFree) {
                    targetSlot = candidate;
                    break;
                }
            }
            if (targetSlot != null) {
                shifted.add(new Lecture(day, targetSlot, original.getRoom(), original.getModule()));
                usedSlots.add(targetSlot);          // new slot is now taken
                usedSlots.remove(original.getTime()); // old slot is freed
            }
        }
        System.out.println("[EarlyLecturesTask] Day " + day
                + " processed by thread " + Thread.currentThread().getName()
                + " — " + shifted.size() + " lecture(s) shifted.");
        return shifted;
    }
}
