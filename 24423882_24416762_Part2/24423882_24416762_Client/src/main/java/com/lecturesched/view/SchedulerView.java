package com.lecturesched.view;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lecturesched.model.Lecture;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SchedulerView {

    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri"};
    private static final String[] TIME_SLOTS = {"09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
        "14:00-15:00", "15:00-16:00", "16:00-17:00", "17:00-18:00"};
    private static final String[] MODULE_COLORS = {"#3f51b5", "#e91e63", "#009688", "#ff5722", "#8bc34a"};

    private ComboBox<String> actionBox;
    private DatePicker datePicker;
    private ComboBox<String> timeBox;
    private TextField roomField;
    private TextField moduleField;
    private Button sendBtn;
    private Button stopBtn;
    private Button clearBtn;
    private Label statusLabel;
    private TextArea logArea;
    private GridPane timetableGrid;
    private final Map<String, StackPane> cells = new HashMap<>();
    private final Map<String, Integer> moduleColorMap = new HashMap<>();
    private int colorCounter = 0;

    public void buildAndShow(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setLeft(buildForm());
        root.setCenter(buildTimetable());
        root.setBottom(buildLog());
        Scene scene = new Scene(root, 1100, 720);
        stage.setTitle("Lecture Scheduler – LM051-2026");
        stage.setScene(scene);
        stage.show();
    }

    private Node buildHeader() {
        Label title = new Label("Lecture Scheduler Client  |  Course: LM051-2026");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        HBox header = new HBox(title);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #e8eaf6;");
        return header;
    }

    private Node buildForm() {
        actionBox = new ComboBox<>(FXCollections.observableArrayList("ADD", "REMOVE", "DISPLAY", "EARLY LECTURES", "OTHER"));
        actionBox.getSelectionModel().selectFirst();
        actionBox.setMaxWidth(Double.MAX_VALUE);

        datePicker = new DatePicker();
        datePicker.setMaxWidth(Double.MAX_VALUE);

        timeBox = new ComboBox<>(FXCollections.observableArrayList(TIME_SLOTS));
        timeBox.getSelectionModel().selectFirst();
        timeBox.setMaxWidth(Double.MAX_VALUE);

        roomField = new TextField();
        roomField.setPromptText("e.g., C105");
        moduleField = new TextField();
        moduleField.setPromptText("e.g., CS6502");

        sendBtn = new Button("▶  Send Request");
        stopBtn = new Button("⏹  STOP");
        clearBtn = new Button("✕  Clear");
        sendBtn.setMaxWidth(Double.MAX_VALUE);
        stopBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        sendBtn.setStyle("-fx-background-color:#3f51b5;-fx-text-fill:white;-fx-font-weight:bold;");
        stopBtn.setStyle("-fx-background-color:#e53935;-fx-text-fill:white;-fx-font-weight:bold;");
        clearBtn.setStyle("-fx-background-color:#757575;-fx-text-fill:white;");

        statusLabel = new Label("Status: Ready");
        statusLabel.setStyle("-fx-font-style: italic;");

        GridPane form = new GridPane();
        form.setPadding(new Insets(10));
        form.setHgap(10);
        form.setVgap(10);
        int row = 0;
        form.add(boldLabel("Action:"), 0, row); 
        form.add(actionBox, 1, row++);
        form.add(boldLabel("Date:"), 0, row); 
        form.add(datePicker, 1, row++);
        form.add(boldLabel("Time Slot:"), 0, row); 
        form.add(timeBox, 1, row++);
        form.add(boldLabel("Room:"), 0, row); 
        form.add(roomField, 1, row++);
        form.add(boldLabel("Module:"), 0, row); 
        form.add(moduleField, 1, row++);

        VBox buttons = new VBox(8, sendBtn, stopBtn, clearBtn, statusLabel);
        buttons.setPadding(new Insets(14, 0, 0, 0));

        VBox left = new VBox(10, sectionLabel("Request Builder"), form, new Separator(), buttons);
        left.setPadding(new Insets(14));
        left.setPrefWidth(370);
        left.setStyle("-fx-border-color:#cccccc;-fx-border-width:0 1 0 0;");

        actionBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFieldVisibility(newVal));
        updateFieldVisibility(actionBox.getValue());
        return left;
    }

    private void updateFieldVisibility(String action) {
        boolean isAdd = "ADD".equals(action);
        boolean isRemove = "REMOVE".equals(action);
        boolean needsDate = isAdd || isRemove;
        datePicker.setDisable(!needsDate);
        timeBox.setDisable(!needsDate);
        roomField.setDisable(!isAdd);
        moduleField.setDisable(!isAdd);
    }

    private Node buildTimetable() {
        timetableGrid = new GridPane();
        timetableGrid.setHgap(2);
        timetableGrid.setVgap(2);
        timetableGrid.setPadding(new Insets(4));

        timetableGrid.getColumnConstraints().add(new ColumnConstraints(110));
        for (int d = 0; d < DAYS.length; d++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setMinWidth(120);
            col.setHgrow(Priority.ALWAYS);
            timetableGrid.getColumnConstraints().add(col);
        }

        timetableGrid.getRowConstraints().add(new RowConstraints(32));
        for (int t = 0; t < TIME_SLOTS.length; t++) {
            timetableGrid.getRowConstraints().add(new RowConstraints(56));
        }

        Label corner = new Label("Time \\ Day");
        corner.setStyle("-fx-font-weight:bold;-fx-font-size:11px;");
        corner.setPadding(new Insets(4));
        timetableGrid.add(corner, 0, 0);

        for (int d = 0; d < DAYS.length; d++) {
            Label dayHeader = new Label(DAYS[d]);
            dayHeader.setMaxWidth(Double.MAX_VALUE);
            dayHeader.setAlignment(Pos.CENTER);
            dayHeader.setStyle("-fx-font-weight:bold;-fx-font-size:12px;" + "-fx-background-color:#3f51b5;-fx-text-fill:white;-fx-padding:4;");
            timetableGrid.add(dayHeader, d + 1, 0);
        }

        for (int t = 0; t < TIME_SLOTS.length; t++) {
            Label timeLabel = new Label(TIME_SLOTS[t]);
            timeLabel.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:4;");
            timeLabel.setWrapText(true);
            timetableGrid.add(timeLabel, 0, t + 1);

            for (int d = 0; d < DAYS.length; d++) {
                StackPane cell = blankCell();
                cells.put(DAYS[d] + "|" + TIME_SLOTS[t], cell);
                timetableGrid.add(cell, d + 1, t + 1);
            }
        }

        ScrollPane scroll = new ScrollPane(timetableGrid);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        VBox center = new VBox(8, sectionLabel("Weekly Course Schedule – LM051-2026"), scroll);
        center.setPadding(new Insets(14));
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return center;
    }

    private StackPane blankCell() {
        StackPane cell = new StackPane();
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setMaxHeight(Double.MAX_VALUE);
        cell.setStyle("-fx-background-color:#f5f5f5;-fx-border-color:#e0e0e0;-fx-border-width:1;");
        return cell;
    }

    private Node buildLog() {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(6);
        logArea.setStyle("-fx-font-family:'Courier New',monospace;-fx-font-size:12px;");
        VBox bottom = new VBox(6, sectionLabel("Conversation Log"), logArea);
        bottom.setPadding(new Insets(10, 14, 10, 14));
        bottom.setStyle("-fx-background-color:#fafafa;" + "-fx-border-color:#cccccc;-fx-border-width:1 0 0 0;");
        return bottom;
    }

    public void refreshTimetable(List<Lecture> lectures) {
        cells.values().forEach(cell -> {
            cell.getChildren().clear();
            cell.setStyle("-fx-background-color:#f5f5f5;-fx-border-color:#e0e0e0;-fx-border-width:1;");
        });

        for (Lecture lec : lectures) {
            DayOfWeek dow = lec.getDate().getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;

            String dayName = dow.name().substring(0, 1) + dow.name().substring(1, 3).toLowerCase();
            StackPane cell = cells.get(dayName + "|" + lec.getTime());
            if (cell == null) continue;

            String color = colorForModule(lec.getModule());
            cell.setStyle("-fx-background-color:" + color + ";-fx-border-color:#cccccc;-fx-border-width:1;");

            Label moduleLabel = new Label(lec.getModule());
            moduleLabel.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:11px;");
            moduleLabel.setWrapText(true);
            moduleLabel.setAlignment(Pos.CENTER);

            Label roomLabel = new Label("Room: " + lec.getRoom());
            roomLabel.setStyle("-fx-text-fill:white;-fx-font-size:10px;");

            VBox content = new VBox(2, moduleLabel, roomLabel);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(4));
            cell.getChildren().setAll(content);
        }
    }

    private String colorForModule(String module) {
        if (!moduleColorMap.containsKey(module)) {
            moduleColorMap.put(module, colorCounter % MODULE_COLORS.length);
            colorCounter++;
        }
        return MODULE_COLORS[moduleColorMap.get(module)];
    }

    public ComboBox<String> getActionBox() { return actionBox; }
    public DatePicker getDatePicker() { return datePicker; }
    public ComboBox<String> getTimeBox() { return timeBox; }
    public TextField getRoomField() { return roomField; }
    public TextField getModuleField() { return moduleField; }
    public Button getSendBtn() { return sendBtn; }
    public Button getStopBtn() { return stopBtn; }
    public Button getClearBtn() { return clearBtn; }

    public void log(String message) { logArea.appendText(message + System.lineSeparator()); }
    public void clearLog() { logArea.clear(); }

    public void setStatus(String text, StatusType type) {
        statusLabel.setText("Status: " + text);
        switch (type) {
            case OK -> statusLabel.setStyle("-fx-text-fill:#2e7d32;-fx-font-style:italic;");
            case ERROR -> statusLabel.setStyle("-fx-text-fill:#c62828;-fx-font-style:italic;");
            case TERMINATED -> statusLabel.setStyle("-fx-text-fill:#6a1a4a;-fx-font-style:italic;");
            default -> statusLabel.setStyle("-fx-font-style:italic;");
        }
    }

    public enum StatusType { READY, OK, ERROR, TERMINATED }

    public void showWarning(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    public void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    public void resetForm() {
        actionBox.getSelectionModel().selectFirst();
        datePicker.setValue(null);
        timeBox.getSelectionModel().selectFirst();
        roomField.clear();
        moduleField.clear();
    }

    private static Label boldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight:bold;");
        return label;
    }

    private static Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#3f51b5;");
        return label;
    }
}
