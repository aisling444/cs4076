package com.lecturesched.view;

import java.util.List;

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
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SchedulerView {

    private ComboBox<String> actionBox;
    private DatePicker       datePicker;
    private ComboBox<String> timeBox;
    private TextField        roomField;
    private TextField        moduleField;

    private Button sendBtn;
    private Button stopBtn;
    private Button clearBtn;

    private Label    statusLabel;
    private TextArea logArea;

    private TableView<LectureRow> table;

    public void buildAndShow(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setLeft(buildForm());
        root.setCenter(buildTable());
        root.setBottom(buildLog());

        Scene scene = new Scene(root, 1020, 680);
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
        actionBox = new ComboBox<>(FXCollections.observableArrayList(
                "ADD", "REMOVE", "DISPLAY", "OTHER"));
        actionBox.getSelectionModel().selectFirst();
        actionBox.setMaxWidth(Double.MAX_VALUE);

        datePicker = new DatePicker();
        datePicker.setMaxWidth(Double.MAX_VALUE);

        timeBox = new ComboBox<>(FXCollections.observableArrayList(
                "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
                "14:00-15:00", "15:00-16:00", "16:00-17:00", "17:00-18:00"));
        timeBox.getSelectionModel().selectFirst();
        timeBox.setMaxWidth(Double.MAX_VALUE);

        roomField = new TextField();
        roomField.setPromptText("e.g., C105");

        moduleField = new TextField();
        moduleField.setPromptText("e.g., CS6502");

        sendBtn  = new Button("▶  Send Request");
        stopBtn  = new Button("⏹  STOP");
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

        int r = 0;
        form.add(bold("Action:"),    0, r); form.add(actionBox,   1, r++);
        form.add(bold("Date:"),      0, r); form.add(datePicker,  1, r++);
        form.add(bold("Time Slot:"), 0, r); form.add(timeBox,     1, r++);
        form.add(bold("Room:"),      0, r); form.add(roomField,   1, r++);
        form.add(bold("Module:"),    0, r); form.add(moduleField, 1, r++);

        VBox buttons = new VBox(8, sendBtn, stopBtn, clearBtn, statusLabel);
        buttons.setPadding(new Insets(14, 0, 0, 0));

        VBox left = new VBox(10,
                sectionLabel("Request Builder"),
                form,
                new Separator(),
                buttons);
        left.setPadding(new Insets(14));
        left.setPrefWidth(370);
        left.setStyle("-fx-border-color:#cccccc;-fx-border-width:0 1 0 0;");

        // Cosmetic: disable fields not relevant to current action
        actionBox.valueProperty().addListener((obs, oldV, newV) -> applyFieldVisibility(newV));
        applyFieldVisibility(actionBox.getValue());

        return left;
    }

    private void applyFieldVisibility(String action) {
        boolean isAdd    = "ADD".equals(action);
        boolean isRemove = "REMOVE".equals(action);
        boolean needsTime = isAdd || isRemove;

        datePicker.setDisable(!needsTime);
        timeBox.setDisable(!needsTime);
        roomField.setDisable(!isAdd);
        moduleField.setDisable(!isAdd);
    }

    @SuppressWarnings("unchecked")
    private Node buildTable() {
        table = new TableView<>();

        TableColumn<LectureRow, String> cDate   = col("Date",   "date");
        TableColumn<LectureRow, String> cTime   = col("Time",   "time");
        TableColumn<LectureRow, String> cRoom   = col("Room",   "room");
        TableColumn<LectureRow, String> cModule = col("Module", "module");

        table.getColumns().addAll(cDate, cTime, cRoom, cModule);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No lectures scheduled yet."));

        VBox center = new VBox(8, sectionLabel("Course Schedule  (LM051-2026)"), table);
        center.setPadding(new Insets(14));
        VBox.setVgrow(table, Priority.ALWAYS);
        return center;
    }

    private Node buildLog() {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(6);
        logArea.setStyle("-fx-font-family:'Courier New',monospace;-fx-font-size:12px;");

        VBox bottom = new VBox(6,
                sectionLabel("Conversation Log  (Client ↔ Server)"),
                logArea);
        bottom.setPadding(new Insets(10, 14, 10, 14));
        bottom.setStyle("-fx-background-color:#fafafa;"
                + "-fx-border-color:#cccccc;-fx-border-width:1 0 0 0;");
        return bottom;
    }

    public ComboBox<String> getActionBox()   { return actionBox;   }
    public DatePicker       getDatePicker()  { return datePicker;  }
    public ComboBox<String> getTimeBox()     { return timeBox;     }
    public TextField        getRoomField()   { return roomField;   }
    public TextField        getModuleField() { return moduleField; }
    public Button           getSendBtn()     { return sendBtn;     }
    public Button           getStopBtn()     { return stopBtn;     }
    public Button           getClearBtn()    { return clearBtn;    }

    public void log(String message) {
        logArea.appendText(message + System.lineSeparator());
    }

    public void clearLog() {
        logArea.clear();
    }

    public void refreshTable(List<Lecture> lectures) {
        List<LectureRow> rows = lectures.stream().map(LectureRow::from).toList();
        table.setItems(FXCollections.observableArrayList(rows));
    }

    public void setStatus(String text, StatusType type) {
        statusLabel.setText("Status: " + text);
        switch (type) {
            case OK         -> statusLabel.setStyle("-fx-text-fill:#2e7d32;-fx-font-style:italic;");
            case ERROR      -> statusLabel.setStyle("-fx-text-fill:#c62828;-fx-font-style:italic;");
            case TERMINATED -> statusLabel.setStyle("-fx-text-fill:#6a1a4a;-fx-font-style:italic;");
            default         -> statusLabel.setStyle("-fx-font-style:italic;");
        }
    }

    public enum StatusType { READY, OK, ERROR, TERMINATED }

    public void showWarning(String header, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        a.setHeaderText(header);
        a.showAndWait();
    }

    public void showInfo(String header, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setHeaderText(header);
        a.showAndWait();
    }

    public void resetForm() {
        actionBox.getSelectionModel().selectFirst();
        datePicker.setValue(null);
        timeBox.getSelectionModel().selectFirst();
        roomField.clear();
        moduleField.clear();
    }

    private static Label bold(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight:bold;");
        return l;
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#3f51b5;");
        return l;
    }

    private static TableColumn<LectureRow, String> col(String title, String property) {
        TableColumn<LectureRow, String> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        return c;
    }

    public static class LectureRow {
        private final String date;
        private final String time;
        private final String room;
        private final String module;

        public LectureRow(String date, String time, String room, String module) {
            this.date   = date;
            this.time   = time;
            this.room   = room;
            this.module = module;
        }

        public static LectureRow from(Lecture l) {
            return new LectureRow(
                    l.getDate().toString(), l.getTime(), l.getRoom(), l.getModule());
        }

        public String getDate()   { return date;   }
        public String getTime()   { return time;   }
        public String getRoom()   { return room;   }
        public String getModule() { return module; }
    }
}
