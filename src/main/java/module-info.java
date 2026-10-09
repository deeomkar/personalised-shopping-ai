module com.myshop {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.xerial.sqlitejdbc;
    requires jbcrypt;
    requires java.net.http;
    requires java.desktop;
    requires com.google.genai;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires org.glavo.webp;

    exports com.myshop;
    opens com.myshop.controller to javafx.fxml;
}
