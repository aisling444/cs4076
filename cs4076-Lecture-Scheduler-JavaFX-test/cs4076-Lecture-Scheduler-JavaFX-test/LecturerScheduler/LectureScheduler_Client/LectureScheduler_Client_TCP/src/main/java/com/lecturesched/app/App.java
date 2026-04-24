package com.lecturesched.app;

import com.lecturesched.controller.SchedulerController;
import com.lecturesched.view.SchedulerView;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        SchedulerView view = new SchedulerView();
        view.buildAndShow(stage);

        new SchedulerController(view);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
