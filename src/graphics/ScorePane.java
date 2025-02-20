package graphics;

import game.ScoreManager;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

public class ScorePane extends VBox {
    private final ScoreManager scoreManager;
    private final Label scoreLabel;

    public ScorePane(ScoreManager scoreManager) {
        this.scoreManager = scoreManager;

        // Label d’affichage du score
        scoreLabel = new Label("Score : 0");
        scoreLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        this.setSpacing(10);      // un petit espace vertical
        this.getChildren().add(scoreLabel);
    }

    /**
     * Méthode pour rafraîchir l’affichage du score
     * en fonction des points stockés dans ScoreManager.
     */
    public void refreshScore() {
        int currentScore = scoreManager.getScore();
        scoreLabel.setText("Score : " + currentScore);
    }
}