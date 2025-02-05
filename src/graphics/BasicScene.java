package graphics;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class BasicScene extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        //langue
        final LanguagePane myLanguagePane = new LanguagePane();
        ScrollPane languageScrollPane = new ScrollPane(myLanguagePane);
        languageScrollPane.setFitToWidth(true);
        languageScrollPane.setPannable(true);
        languageScrollPane.getStylesheets().add(getClass().getResource("scrollbar.css").toExternalForm());

        //plateau
        final MyPane scrabbleBoard = new MyPane();
        ScrollPane boardScrollPane = new ScrollPane(scrabbleBoard);
        boardScrollPane.setFitToWidth(true);
        boardScrollPane.setPannable(true);
        boardScrollPane.getStylesheets().add(getClass().getResource("scrollbar.css").toExternalForm());

        TabPane tabPane = new TabPane();
        Tab tabLanguage = new Tab("Choix de la langue", languageScrollPane);
        Tab tabScrabble = new Tab("Plateau de Scrabble", boardScrollPane);
        tabPane.getTabs().addAll(tabLanguage, tabScrabble);

        Scene scene = new Scene(tabPane, 640, 640);
        primaryStage.setTitle("Projet Scrabble");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}