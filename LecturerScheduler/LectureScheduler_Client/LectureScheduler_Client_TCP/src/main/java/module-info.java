module com.lecturesched {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.lecturesched.app        to javafx.graphics;
    opens com.lecturesched.view       to javafx.base, javafx.controls;
    opens com.lecturesched.controller to javafx.fxml;
    opens com.lecturesched.model      to javafx.base;
}
