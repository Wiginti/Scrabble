package graphics;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Fenêtre de menu permettant de choisir le nombre de joueurs,
 * puis de lancer la partie Scrabble.
 */
public class MenuScene {

    private final Stage menuStage;       // La fenêtre (Stage) du menu
    private final BasicScene mainApp;    // Référence à l'application principale

    public MenuScene(BasicScene mainApp) {
        this.mainApp = mainApp;
        this.menuStage = new Stage();
        initializeMenu();
    }

    private void initializeMenu() {
        // Choix du nombre de joueurs
        Label label = new Label("Choisissez le nombre de joueurs (2 à 4) :");
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(2, 3, 4);
        comboBox.setValue(2); // Valeur par défaut

        Button startButton = new Button("Démarrer la partie");
        startButton.setOnAction(e -> {
            int nbPlayers = comboBox.getValue();
            menuStage.close();           // Ferme la fenêtre de menu
            mainApp.openGame(nbPlayers); // Lance la partie avec nbPlayers
        });

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.getChildren().addAll(label, comboBox, startButton);

        Scene scene = new Scene(root, 300, 150);
        menuStage.setTitle("Menu Scrabble");
        menuStage.setScene(scene);
    }

    /**
     * Affiche la fenêtre de menu.
     */
    public void show() {
        menuStage.show();
    }
}