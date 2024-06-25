module com.github.idelstak.mdtopdf {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.boxicons;
    requires org.kordamp.ikonli.octicons;
    requires flexmark;
    requires layout;
    requires html2pdf;
    requires org.slf4j;

    opens com.github.idelstak.mdtopdf;

    exports com.github.idelstak.mdtopdf;
}