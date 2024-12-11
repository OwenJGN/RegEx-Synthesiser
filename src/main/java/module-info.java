module com.owenjg.regexsynthesiser {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    opens com.owenjg.regexsynthesiser to javafx.fxml;
    exports com.owenjg.regexsynthesiser;
    exports com.owenjg.regexsynthesiser.controller;
    opens com.owenjg.regexsynthesiser.controller to javafx.fxml;
}