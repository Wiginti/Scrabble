package graphics;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.net.URL;

import game.Letter;

public class BasicScene extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
    	
    	//Initialisation de Letter
    	Letter.getInstance();
    	
        // 2. Création du plateau de Scrabble
        final MyPane scrabbleBoard = new MyPane();
        ScrollPane boardScrollPane = new ScrollPane(scrabbleBoard);
        boardScrollPane.setFitToWidth(true);
        boardScrollPane.setPannable(true);

        // 3. Création de la rack de lettres (affichage des tuiles pour le joueur)
        HBox letterRack = new HBox(5); // espacement de 5 pixels entre les tuiles
        // Générer 7 tuiles initiales
        /*for (int i = 0; i < 7; i++) {
            LetterTile tile = createTile(letterRack);
            letterRack.getChildren().add(tile);
        }*/

        // 4. Composition du plateau avec le plateau et la rack
        BorderPane boardWithRack = new BorderPane();
        boardWithRack.setCenter(boardScrollPane);
        boardWithRack.setBottom(letterRack);

        // 5. Application de la feuille de style (si disponible)
        URL cssUrl = getClass().getResource("scrollbar.css");
        if (cssUrl != null) {
            boardScrollPane.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Stylesheet scrollbar.css introuvable.");
        }

        // 7. Création de la scène et affichage de la fenêtre
        Scene scene = new Scene(scrabbleBoard, 800, 800);
        primaryStage.setTitle("Scrabble – Choix et Plateau");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}