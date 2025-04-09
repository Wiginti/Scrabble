package game;

public class BoardState {
    private final String[][] grid;

    public BoardState(String[][] grid) {
        // Clonage de la grille pour éviter toute modification externe
        this.grid = new String[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            this.grid[i] = grid[i].clone();
        }
    }

    public String getLetterAt(int row, int col) {
        return grid[row][col];
    }

    public int getRows() {
        return grid.length;
    }

    public int getCols() {
        return grid[0].length;
    }

    // Vous pouvez ajouter d'autres méthodes d'accès ou de mise à jour si nécessaire
}