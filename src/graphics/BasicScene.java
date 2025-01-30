package graphics;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

public class BasicScene extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
    	
    	final LanguagePane myLanguagePane = new LanguagePane();
    	ScrollPane scrollPane = new ScrollPane(myLanguagePane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.getStylesheets().add(getClass().getResource("scrollbar.css").toExternalForm());
		Scene scene = new Scene(scrollPane, 160, 600);
		
    	
        primaryStage.setTitle("Choix de la langue");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}