package graphics;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import game.Letter;
import java.net.URL;
import java.util.List;

public class BasicScene extends Application {

    private Stage primaryStage;  // Pour garder une référence à la fenêtre principale
    private int numberOfPlayers; // Nombre de joueurs sélectionné

    // On stocke le MyPane pour pouvoir actualiser le titre si besoin
    private MyPane scrabbleBoard;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        // On affiche d'abord le menu
        MenuScene menu = new MenuScene(this);
        menu.show();
    }

    /**
     * Méthode appelée par le menu pour lancer la partie avec "nbPlayers" joueurs.
     */
    public void openGame(int nbPlayers) {
        this.numberOfPlayers = nbPlayers;

        // Initialisation du sac de lettres
        Letter.getInstance();

        // On crée un MyPane (le plateau) en lui passant le nombre de joueurs
        scrabbleBoard = new MyPane(numberOfPlayers, this);

        // On met ce MyPane dans un ScrollPane
        ScrollPane boardScrollPane = new ScrollPane(scrabbleBoard);
        boardScrollPane.setFitToWidth(true);
        boardScrollPane.setPannable(true);

        // Application éventuelle de scrollbar.css
        URL cssUrl = getClass().getResource("scrollbar.css");
        if (cssUrl != null) {
            boardScrollPane.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Stylesheet scrollbar.css introuvable.");
        }

        // Récupérer tous les ScorePane (un par joueur)
        List<ScorePane> scorePanes = scrabbleBoard.getAllScorePanes();
        HBox scoresLayout = new HBox(20);
        scoresLayout.getChildren().addAll(scorePanes);

        // On assemble : plateau au centre, scores à droite
        BorderPane root = new BorderPane();
        root.setCenter(boardScrollPane);
        root.setRight(scoresLayout);

        // Création de la scène et affichage de la fenêtre
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);

        // Mise à jour du titre (ex: "Scrabble – Joueur 1 sur 2 joueurs")
        updateWindowTitle(1);

        primaryStage.show();
    }

    /**
     * Méthode pour actualiser le titre de la fenêtre
     * en fonction du joueur courant (index base 1).
     */
    public void updateWindowTitle(int currentPlayerIndex) {
        // Ex : "Scrabble (4 joueurs) – Tour de Joueur 2"
        String title = "Scrabble (" + numberOfPlayers + " joueurs) – Tour de Joueur " + currentPlayerIndex;
        primaryStage.setTitle(title);
    }

    public static void main(String[] args) {
        launch(args);
    }
}