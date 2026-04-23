package com.lecturesched.server;

import com.lecturesched.model.Schedule;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static final int PORT = 5678;
    private static final String COURSE = "LM123";
    public static void main(String[] args) {
        System.out.println("=== Lecture Scheduler Server ===");
        System.out.println("Course: " + COURSE);
        System.out.println("Port: " + PORT);
        System.out.println("Waiting for connections...\n");
        Schedule schedule = new Schedule(COURSE);
        AtomicInteger clientIdCounter = new AtomicInteger(0);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int id = clientIdCounter.incrementAndGet();
                ClientHandler handler = new ClientHandler(clientSocket, schedule, id);
                Thread thread = new Thread(handler, "ClientThread-" + id);
                thread.setDaemon(true);  // thread dies automatically when main exits
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Server fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
