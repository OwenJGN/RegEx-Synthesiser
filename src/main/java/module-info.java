module com.owenjg.regexsynthesiser {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens com.owenjg.regexsynthesiser.application to javafx.fxml;
    exports com.owenjg.regexsynthesiser.application;
    exports com.owenjg.regexsynthesiser.controller;
    opens com.owenjg.regexsynthesiser.controller to javafx.fxml;
}