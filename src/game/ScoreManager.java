package game;

public class ScoreManager {
    private int score;

    public ScoreManager() {
        this.score = 0;
    }

    /**
     * Ajoute des points au score actuel.
     */
    public void addPoints(int points) {
        this.score += points;
    }

    /**
     * Retourne le score actuel.
     */
    public int getScore() {
        return this.score;
    }
}