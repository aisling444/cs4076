package org.openjfx.lab3;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.*;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
     

public class App extends Application {
  private boolean connected = true;
        private final FakeServer server = new FakeServer();
        
    @Override
    public void start(Stage primaryStage) {
        
        Label status = new Label("Connected");
     
        ComboBox<String> actionBox = new ComboBox<>(FXCollections.observableArrayList("Add", "Remove", "Display", "Other"));
        ComboBox<String> timeBox = new ComboBox<>(FXCollections.observableArrayList("09:00-10:00","10:00-11:00","11:00-12:00", "12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00", "17:00-18:00"));
        
        TextField roomField = new TextField();
        TextField moduleField = new TextField();
        DatePicker datePicker = new DatePicker();
        
        Button submit = new Button ("Send Request");
        Button clear = new Button ("Clear");
        Button stop = new Button ("Stop");
        
       
        

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setPadding(new Insets(10));

        form.addRow(0, new Label("Action"), actionBox);
        form.addRow(1, new Label("Date"), datePicker);
        form.addRow(2, new Label("Time"), timeBox);
        form.addRow(3, new Label("Room"), roomField);
        form.addRow(4, new Label("Module"), moduleField);
        form.addRow(5, submit, clear, stop);
        
        TextArea log = new TextArea();
        log.setEditable(false);
        log.setWrapText(true);
        log.setPrefWidth(360);
        
        TableView<ScheduleEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ScheduleEntry, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<ScheduleEntry, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));

        TableColumn<ScheduleEntry, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("room"));

        TableColumn<ScheduleEntry, String> moduleCol = new TableColumn<>("Module");
        moduleCol.setCellValueFactory(new PropertyValueFactory<>("module"));

        table.getColumns().addAll(dateCol, timeCol, roomCol, moduleCol);
        
        BorderPane root = new BorderPane();
        root.setTop(status);
        root.setLeft(form);
        root.setCenter(table);
        root.setRight(log);
        
        BorderPane.setMargin(status, new Insets(10));
        BorderPane.setMargin(log, new Insets(10));
        BorderPane.setMargin(table, new Insets(10));
        
        submit.setOnAction(e -> {
            if (!connected) {
                logLine(log, "SERVER <- ERROR|DISCONNECTED");
                return;
            }
            
            String action = actionBox.getValue();
            LocalDate date = datePicker.getValue();
            String time = timeBox.getValue();
            String room = roomField.getText().trim();
            String module = moduleField.getText().trim();
            String validationError = validate(action, date, time, room, module);
            if (validationError != null) {
                logLine(log, "CLIENT -> " + safeAction(action) + "|" + (date == null ? "" : date) + "|" + (time == null ? "" : time) + "|" + room + "|" + module);
                logLine(log, "SERVER <- ERROR|" + validationError);
                return;
            }
            
        String request = buildRequest(action, date, time, room, module);
        logLine(log, "CLIENT -> " + request);
        
        String response = server.handle(request);
        logLine(log, "SERVER <- " + response);
        
        if ("Display".equals(action) || "Add".equals(action) || "Remove".equals(action)){
            ObservableList<ScheduleEntry> serverData = server.getSchedule();
            table.setItems(serverData);
        }
        });
       
        
        
        actionBox.setValue("Display");
        timeBox.setValue("09:00-10:00");
        
        clear.setOnAction(e -> log.clear());
        stop.setOnAction(e -> {
            connected = false;
            submit.setDisable(true);
            status.setText("Disconnected");
            logLine(log, "CLIENT -> Disconnected");
            logLine(log, "SERVER <- OK|Disconnected");

        });
        
        
   


        
        Scene scene = new Scene (root, 980, 450);
        primaryStage.setScene(scene);
        primaryStage.setTitle("User Form");
        primaryStage.show();  
    }

    private String safeAction(String action){
        return action == null ? "" : action;
    }
    
    private void logLine(TextArea log, String msg) { 
        log.appendText(msg + "\n");        
    }
    
    private String buildRequest(String action, LocalDate date, String time, String room, String module){
        if (action == null) return "ERROR||||";
        if (action.equals("Display") || action.equals("Other")) {
            return action + "||||";
        }
  
        String dateStr = (date == null) ? "" : date.toString();
        String timeStr = (time == null) ? "" : time;
        String roomStr = (room == null) ? "" : room;
        String moduleStr = (module == null) ? "" : module;
        
        return action + "|" + dateStr + "|" + timeStr + "|" + roomStr + "|" + moduleStr;

    }
    
    private String validate(String action, LocalDate date, String time, String room, String module) {
        if (action == null || action.isBlank()) return "NO_ACTION";

        if ("Display".equals(action) || "Other".equals(action)) {
            return null; 
        }

        if ("ADD".equals(action)) {
            if (date == null) return "MISSING_DATE";
            if (time == null || time.isBlank()) return "MISSING_TIME";
            if (room == null || room.isBlank()) return "MISSING_ROOM";
            if (module == null || module.isBlank()) return "MISSING_MODULE";
            return null;
        }

        if ("Remove".equals(action)) {
            if (date == null) return "MISSING_DATE";
            if (time == null || time.isBlank()) return "MISSING_TIME";
            return null;
        }

        return "UNKNOWN_ACTION";
    }

    

    public static void main(String[] args) {
        launch(args);
    }
}