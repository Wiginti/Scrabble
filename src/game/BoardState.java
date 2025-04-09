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

    // Si le plateau est carré (ici 15x15), on peut retourner getRows()
    public int getSize() {
        return getRows();
    }

    // Vérifie si le plateau est entièrement vide (aucune lettre posée)
    public boolean isEmpty() {
        for (int i = 0; i < getRows(); i++) {
            for (int j = 0; j < getCols(); j++) {
                String letter = grid[i][j];
                if (letter != null && !letter.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    // Renvoie vrai si la case (row, col) contient une lettre
    public boolean hasLetter(int row, int col) {
        String letter = getLetterAt(row, col);
        return (letter != null && !letter.isEmpty());
    }

    // Renvoie le caractère de la case (row, col). Si la case est vide, retourne le caractère nul.
    public char getLetter(int row, int col) {
        String letter = getLetterAt(row, col);
        return (letter != null && !letter.isEmpty()) ? letter.charAt(0) : '\0';
    }

    // Permet de poser/modifier la lettre sur une case donnée
    public void setLetter(int row, int col, char letter) {
        grid[row][col] = String.valueOf(letter);
    }
}