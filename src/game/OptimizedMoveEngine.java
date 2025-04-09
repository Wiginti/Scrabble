package game;

import dictionary.Dictionary;
import graphics.MyPane;

import java.util.ArrayList;
import java.util.List;

public class OptimizedMoveEngine {

	public static class BestMove {
	    public final String word;
	    public final int startRow;
	    public final int startCol;
	    public final boolean horizontal;
	    public final int score;       // Score total
	    public final int mainScore;   // Score du mot principal
	    public final int crossScore;  // Score des mots croisés

	    public BestMove(String word, int startRow, int startCol, boolean horizontal, int score, int mainScore, int crossScore) {
	        this.word = word;
	        this.startRow = startRow;
	        this.startCol = startCol;
	        this.horizontal = horizontal;
	        this.score = score;
	        this.mainScore = mainScore;
	        this.crossScore = crossScore;
	    }

	    @Override
	    public String toString() {
	        return word + " @(" + startRow + "," + startCol + ") " + (horizontal ? "Horizontal" : "Vertical") + " - " + score + " points";
	    }
	}

    public static BestMove findBestMove(List<Character> rack, BoardState boardState, Dictionary dictionary) {
        BestMove bestMove = null;
        int bestScore = 0;
        int size = boardState.getSize();
        List<String> allWords = dictionary.getWords();

        // Parcours de tous les mots du dictionnaire
        for (String word : allWords) {
            int wordLength = word.length();

            for (boolean horizontal : new boolean[]{true, false}) {
                int maxRow = horizontal ? size : size - wordLength + 1;
                int maxCol = horizontal ? size - wordLength + 1 : size;

                for (int startRow = 0; startRow < maxRow; startRow++) {
                    for (int startCol = 0; startCol < maxCol; startCol++) {

                        if (!boardState.isEmpty()) {
                            if (!isConnected(startRow, startCol, horizontal, wordLength, boardState)) {
                                continue;
                            }
                        } else {
                            int center = size / 2;
                            if (horizontal) {
                                if (!(startRow == center && (startCol <= center && center < startCol + wordLength))) {
                                    continue;
                                }
                            } else {
                                if (!(startCol == center && (startRow <= center && center < startRow + wordLength))) {
                                    continue;
                                }
                            }
                        }

                        List<Character> tempRack = new ArrayList<>(rack);
                        boolean placementValide = true;
                        int currRow = startRow;
                        int currCol = startCol;
                        List<int[]> newTiles = new ArrayList<>();

                        // Vérification de chaque lettre du mot candidate
                        for (int i = 0; i < wordLength; i++) {
                            char letter = Character.toUpperCase(word.charAt(i));

                            if (boardState.safeHasLetter(currRow, currCol)) {
                                char existingLetter = Character.toUpperCase(boardState.getLetter(currRow, currCol));
                                if (existingLetter != letter) {
                                    placementValide = false;
                                    break;
                                }
                            } else {
                                int index = tempRack.indexOf(letter);
                                if (index == -1) {
                                    int jokerIndex = tempRack.indexOf('*');
                                    if (jokerIndex == -1) {
                                        placementValide = false;
                                        break;
                                    } else {
                                        tempRack.remove(jokerIndex);
                                        newTiles.add(new int[]{currRow, currCol, letter, 1});
                                    }
                                } else {
                                    tempRack.remove(index);
                                    newTiles.add(new int[]{currRow, currCol, letter, 0});
                                }
                            }
                            if (horizontal) currCol++;
                            else currRow++;
                        }

                        // Nécessite qu'au moins une lettre soit posée du chevalet
                        if (!placementValide || newTiles.isEmpty()) {
                            continue;
                        }

                        if (!checkCrossWords(newTiles, boardState, horizontal, dictionary)) {
                            continue;
                        }

                        // Simulation du plateau final en copiant le plateau actuel et en y ajoutant les nouvelles lettres
                        String[][] simGrid = new String[size][size];
                        for (int r = 0; r < size; r++) {
                            for (int c = 0; c < size; c++) {
                                char letter = boardState.getLetter(r, c);
                                simGrid[r][c] = (letter == '\0') ? null : String.valueOf(letter);
                            }
                        }

                        for (int[] tile : newTiles) {
                            int tileRow = tile[0];
                            int tileCol = tile[1];
                            char placedLetter = (char) tile[2];
                            simGrid[tileRow][tileCol] = String.valueOf(placedLetter);
                        }
                        BoardState simulatedState = new BoardState(simGrid);

                        // Récupérer le mot complet formé sur le plateau virtuel
                        String fullWord = getFullMainWord(startRow, startCol, word, horizontal, simulatedState);

                        // Si le mot complet n'est pas valide, on passe à la suite
                        if (!dictionary.validWord(fullWord)) {
                            continue;
                        }

                        int[] scores = computeScore(word, newTiles, boardState, horizontal);
                        int score = scores[0];       // Score total
                        int mainScore = scores[1];   // Score du mot principal
                        int crossScore = scores[2];
                        if (tempRack.isEmpty()) score += 50;

                        if (score > bestScore) {
                            bestScore = score;
                            bestMove = new BestMove(word, startRow, startCol, horizontal, score, mainScore, crossScore);
                        }
                    }
                }
            }
        }
        return bestMove;
    }

    private static boolean isConnected(int startRow, int startCol, boolean horizontal, int wordLength, BoardState boardState) {
        for (int i = 0; i < wordLength; i++) {
            int row = horizontal ? startRow : startRow + i;
            int col = horizontal ? startCol + i : startCol;
            if (boardState.safeHasLetter(row, col)) return true;
            if (horizontal) {
                if (boardState.safeHasLetter(row - 1, col) || boardState.safeHasLetter(row + 1, col)) return true;
            } else {
                if (boardState.safeHasLetter(row, col - 1) || boardState.safeHasLetter(row, col + 1)) return true;
            }
        }
        return false;
    }

    private static boolean checkCrossWords(List<int[]> newTiles, BoardState boardState, boolean mainWordHorizontal, Dictionary dictionary) {
        for (int[] tile : newTiles) {
            int row = tile[0];
            int col = tile[1];
            char placedLetter = (char) tile[2];
            StringBuilder wordBuilder = new StringBuilder();

            if (mainWordHorizontal) {
                int rStart = row;
                while (boardState.safeHasLetter(rStart - 1, col)) rStart--;
                int rEnd = row;
                while (boardState.safeHasLetter(rEnd + 1, col)) rEnd++;

                if (rEnd == rStart) continue;

                for (int r = rStart; r <= rEnd; r++) {
                    wordBuilder.append((r == row) ? placedLetter : boardState.getLetter(r, col));
                }
            } else {
                int cStart = col;
                while (boardState.safeHasLetter(row, cStart - 1)) cStart--;
                int cEnd = col;
                while (boardState.safeHasLetter(row, cEnd + 1)) cEnd++;

                if (cEnd == cStart) continue;

                for (int c = cStart; c <= cEnd; c++) {
                    wordBuilder.append((c == col) ? placedLetter : boardState.getLetter(row, c));
                }
            }

            String crossWord = wordBuilder.toString();
            if (!dictionary.validWord(crossWord)) {
                return false;
            }
        }
        return true;
    }

    private static int[] computeScore(String word, List<int[]> newTiles, BoardState boardState, boolean horizontal) {
        int mainScore = 0;
        int wordMultiplier = 1;
        int r = newTiles.get(0)[0];
        int c = newTiles.get(0)[1];
        // Calcul du mot principal
        for (int i = 0; i < word.length(); i++) {
            char letter = Character.toUpperCase(word.charAt(i));
            int letterValue = Letter.pointsLetter.getOrDefault(letter, 0);
            int letterMultiplier = 1;
            int cellWordMultiplier = 1;
            if (!boardState.safeHasLetter(r, c)) {
                letterMultiplier = MyPane.getLetterMultiplier(r, c);
                cellWordMultiplier = MyPane.getWordMultiplier(r, c);
            }
            mainScore += letterValue * letterMultiplier;
            wordMultiplier *= cellWordMultiplier;
            if (horizontal) {
                c++;
            } else {
                r++;
            }
        }
        int mainTotal = mainScore * wordMultiplier;

        // Calcul des scores pour les mots croisés
        int crossScoreTotal = 0;
        for (int[] tile : newTiles) {
            int row = tile[0];
            int col = tile[1];
            char letter = (char) tile[2];
            int crossScore = 0;
            int crossWordMultiplier = 1;
            if (horizontal) {
                int rStart = row;
                while (boardState.safeHasLetter(rStart - 1, col)) rStart--;
                int rEnd = row;
                while (boardState.safeHasLetter(rEnd + 1, col)) rEnd++;
                if (rEnd == rStart) continue;
                for (int rPos = rStart; rPos <= rEnd; rPos++) {
                    char ch = (rPos == row) ? letter : boardState.getLetter(rPos, col);
                    int value = Letter.pointsLetter.getOrDefault(Character.toUpperCase(ch), 0);
                    int mult = (rPos == row) ? MyPane.getLetterMultiplier(rPos, col) : 1;
                    crossScore += value * mult;
                    if (rPos == row) {
                        crossWordMultiplier *= MyPane.getWordMultiplier(rPos, col);
                    }
                }
            } else { // cas vertical
                int cStart = col;
                while (boardState.safeHasLetter(row, cStart - 1)) cStart--;
                int cEnd = col;
                while (boardState.safeHasLetter(row, cEnd + 1)) cEnd++;
                if (cEnd == cStart) continue;
                for (int cPos = cStart; cPos <= cEnd; cPos++) {
                    char ch = (cPos == col) ? letter : boardState.getLetter(row, cPos);
                    int value = Letter.pointsLetter.getOrDefault(Character.toUpperCase(ch), 0);
                    int mult = (cPos == col) ? MyPane.getLetterMultiplier(row, cPos) : 1;
                    crossScore += value * mult;
                    if (cPos == col) {
                        crossWordMultiplier *= MyPane.getWordMultiplier(row, cPos);
                    }
                }
            }
            crossScoreTotal += crossScore * crossWordMultiplier;
        }
        int totalScore = mainTotal + crossScoreTotal;
        return new int[]{totalScore, mainTotal, crossScoreTotal};
    }


    // Cette méthode simule l'extension du mot principal sur le plateau final
    private static String getFullMainWord(int startRow, int startCol, String word, boolean horizontal, BoardState boardState) {
        int row = startRow;
        int col = startCol;

        // Recule jusqu'au début réel du mot
        if (horizontal) {
            while (col > 0 && boardState.safeHasLetter(row, col - 1)) {
                col--;
            }
        } else {
            while (row > 0 && boardState.safeHasLetter(row - 1, col)) {
                row--;
            }
        }

        StringBuilder fullWord = new StringBuilder();

        // Avance dans la direction principale tant que les cases contiguës sont remplies
        while (row < boardState.getRows() && col < boardState.getCols() && boardState.safeHasLetter(row, col)) {
            fullWord.append(boardState.getLetter(row, col));
            if (horizontal) {
                col++;
            } else {
                row++;
            }
        }

        return fullWord.toString();
    }

}
