package graphics;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.net.URL;

public class BasicScene extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Création du panneau de langue
        final LanguagePane myLanguagePane = new LanguagePane();
        ScrollPane languageScrollPane = new ScrollPane(myLanguagePane);
        languageScrollPane.setFitToWidth(true);
        languageScrollPane.setPannable(true);

        // 2. Création du plateau de Scrabble
        final MyPane scrabbleBoard = new MyPane();
        ScrollPane boardScrollPane = new ScrollPane(scrabbleBoard);
        boardScrollPane.setFitToWidth(true);
        boardScrollPane.setPannable(true);

        // 3. Création de la rack de lettres (affichage des tuiles pour le joueur)
        HBox letterRack = new HBox(5); // espacement de 5 pixels entre les tuiles
        // Générer 7 tuiles initiales
        for (int i = 0; i < 7; i++) {
            LetterTile tile = createTile(letterRack);
            letterRack.getChildren().add(tile);
        }

        // 4. Composition du plateau avec le plateau et la rack
        BorderPane boardWithRack = new BorderPane();
        boardWithRack.setCenter(boardScrollPane);
        boardWithRack.setBottom(letterRack);

        // 5. Application de la feuille de style (si disponible)
        URL cssUrl = getClass().getResource("scrollbar.css");
        if (cssUrl != null) {
            languageScrollPane.getStylesheets().add(cssUrl.toExternalForm());
            boardScrollPane.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Stylesheet scrollbar.css introuvable.");
        }

        // 6. Création d'un TabPane pour permettre de basculer entre le choix de langue et le plateau
        TabPane tabPane = new TabPane();

        Tab tabLanguage = new Tab("Choix de la langue", languageScrollPane);
        tabLanguage.setClosable(false);

        Tab tabBoard = new Tab("Plateau de Scrabble", boardWithRack);
        tabBoard.setClosable(false);

        tabPane.getTabs().addAll(tabLanguage, tabBoard);

        // 7. Création de la scène et affichage de la fenêtre
        Scene scene = new Scene(tabPane, 800, 800);
        primaryStage.setTitle("Scrabble – Choix et Plateau");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Crée une nouvelle LetterTile avec un événement onDragDone.
     * Si le drop est complété, la tuile est retirée de la rack et remplacée par une nouvelle.
     *
     * @param letterRack Le conteneur HBox de la rack de lettres.
     * @return La nouvelle LetterTile créée.
     */
    private LetterTile createTile(HBox letterRack) {
        char letter = LetterGenerator.getRandomLetter();
        LetterTile tile = new LetterTile(letter);
        tile.setOnDragDone(event -> {
            if (event.isDropCompleted()) {
                // Retire la tuile déposée de la rack
                letterRack.getChildren().remove(tile);
                // Ajoute une nouvelle tuile pour maintenir 7 lettres
                letterRack.getChildren().add(createTile(letterRack));
            }
            event.consume();
        });
        return tile;
    }

    public static void main(String[] args) {
        launch(args);
    }
}