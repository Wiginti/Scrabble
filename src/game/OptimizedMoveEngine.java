package game;

import dictionary.Dictionary;
import graphics.MyPane;

import java.util.ArrayList;
import java.util.List;
import game.Letter; // Importation de la classe Letter

public class OptimizedMoveEngine {

<<<<<<< HEAD
	public static class BestMove {
	    public final String word;
	    public final int startRow;
	    public final int startCol;
	    public final boolean horizontal;
	    public final int score;       // Score total
	    public final int mainScore;   // Score du mot principal
	    public final int crossScore;  // Score des mots croisés
=======
    // Classe interne représentant le meilleur coup trouvé.
    public static class BestMove {
        public final String word;
        public final int startRow;
        public final int startCol;
        public final boolean horizontal;
        public final int score;
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b

	    public BestMove(String word, int startRow, int startCol, boolean horizontal, int score, int mainScore, int crossScore) {
	        this.word = word;
	        this.startRow = startRow;
	        this.startCol = startCol;
	        this.horizontal = horizontal;
	        this.score = score;
	        this.mainScore = mainScore;
	        this.crossScore = crossScore;
	    }

<<<<<<< HEAD
	    @Override
	    public String toString() {
	        return word + " @(" + startRow + "," + startCol + ") " + (horizontal ? "Horizontal" : "Vertical") + " - " + score + " points";
	    }
	}
=======
        @Override
        public String toString() {
            return word + " @(" + startRow + "," + startCol + ") " + (horizontal ? "Horizontal" : "Vertical") + " - " + score + " points";
        }
    }
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b

    public static BestMove findBestMove(List<Character> rack, BoardState boardState, Dictionary dictionary) {
        BestMove bestMove = null;
        int bestScore = 0;
        int size = boardState.getSize();
<<<<<<< HEAD
        List<String> allWords = dictionary.getWords();

        // Parcours de tous les mots du dictionnaire
        for (String word : allWords) {
            int wordLength = word.length();

            for (boolean horizontal : new boolean[]{true, false}) {
=======

        // Récupérer l'ensemble des mots du dictionnaire.
        List<String> allWords = dictionary.getWords();

        // Parcourir tous les mots du dictionnaire.
        for (String word : allWords) {
            int wordLength = word.length();
            // Essayer les deux orientations : horizontale et verticale.
            for (boolean horizontal : new boolean[]{true, false}) {
                // Définir les bornes de départ pour que le mot tienne sur le plateau.
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
                int maxRow = horizontal ? size : size - wordLength + 1;
                int maxCol = horizontal ? size - wordLength + 1 : size;

                for (int startRow = 0; startRow < maxRow; startRow++) {
                    for (int startCol = 0; startCol < maxCol; startCol++) {
<<<<<<< HEAD

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
=======
                        try {
                            // Si le plateau n'est pas vide, le mot doit être connecté à une lettre existante.
                            if (!boardState.isEmpty()) {
                                if (!isConnected(startRow, startCol, horizontal, wordLength, boardState)) {
                                    continue;
                                }
                            } else {
                                // Premier coup : le mot doit couvrir la case centrale.
                                int center = size / 2;
                                if (horizontal) {
                                    if (!(startRow == center && (startCol <= center && center < startCol + wordLength))) {
                                        continue;
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
                                    }
                                } else {
                                    if (!(startCol == center && (startRow <= center && center < startRow + wordLength))) {
                                        continue;
                                    }
                                }
                            }
<<<<<<< HEAD
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
=======

                            // Copie temporaire du chevalet pour simuler la consommation des lettres.
                            List<Character> tempRack = new ArrayList<>(rack);
                            boolean placementValide = true;
                            int currRow = startRow;
                            int currCol = startCol;
                            // Liste stockant les positions des nouvelles lettres à poser.
                            // Chaque élément int[] contient : {row, col, lettre (en int), flag joker (0 ou 1)}.
                            List<int[]> newTiles = new ArrayList<>();

                            // Parcours lettre par lettre du mot.
                            for (int i = 0; i < wordLength; i++) {
                                char letter = Character.toUpperCase(word.charAt(i));
                                if (boardState.hasLetter(currRow, currCol)) {
                                    // La case est occupée : la lettre existante doit correspondre.
                                    char existingLetter = Character.toUpperCase(boardState.getLetter(currRow, currCol));
                                    if (existingLetter != letter) {
                                        placementValide = false;
                                        break;
                                    }
                                } else {
                                    // La case est vide : retirer la lettre du chevalet.
                                    int index = tempRack.indexOf(letter);
                                    if (index == -1) {
                                        // Si la lettre n'est pas présente, essayer un joker ('*').
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
                                // Passer à la case suivante selon l'orientation.
                                if (horizontal) {
                                    currCol++;
                                } else {
                                    currRow++;
                                }
                            }

                            if (!placementValide || newTiles.isEmpty()) {
                                continue;
                            }

                            // Vérifier la validité des mots croisés formés.
                            if (!checkCrossWords(newTiles, boardState, horizontal, dictionary)) {
                                continue;
                            }

                            // Calcul du score (mot principal + mots croisés).
                            int score = computeScore(word, newTiles, boardState, horizontal);
                            // Bonus de 50 points si le chevalet est entièrement utilisé.
                            if (tempRack.isEmpty()) {
                                score += 50;
                            }

                            // Met à jour le meilleur coup si le score est supérieur.
                            if (score > bestScore) {
                                bestScore = score;
                                bestMove = new BestMove(word, startRow, startCol, horizontal, score);
                            }
                        } catch (Exception e) {
                            // Si une erreur survient pour ces coordonnées, on passe à la suivante.
                            e.printStackTrace();
                            continue;
                        }
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
                    }
                }
            }
        }
        return bestMove;
    }

<<<<<<< HEAD
=======
    // Méthode utilitaire pour vérifier que les coordonnées (row, col) sont dans le plateau.
    private static boolean inBounds(BoardState boardState, int row, int col) {
        int size = boardState.getSize();
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    // Vérifie qu'au moins une case du mot touche une lettre existante.
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
    private static boolean isConnected(int startRow, int startCol, boolean horizontal, int wordLength, BoardState boardState) {
        for (int i = 0; i < wordLength; i++) {
            int row = horizontal ? startRow : startRow + i;
            int col = horizontal ? startCol + i : startCol;
<<<<<<< HEAD
            if (boardState.safeHasLetter(row, col)) return true;
            if (horizontal) {
                if (boardState.safeHasLetter(row - 1, col) || boardState.safeHasLetter(row + 1, col)) return true;
            } else {
                if (boardState.safeHasLetter(row, col - 1) || boardState.safeHasLetter(row, col + 1)) return true;
=======
            if (inBounds(boardState, row, col) && boardState.hasLetter(row, col)) {
                return true;
            }
            // Vérifier également les cases adjacentes en toute sécurité.
            if (inBounds(boardState, row - 1, col) && boardState.hasLetter(row - 1, col)) {
                return true;
            }
            if (inBounds(boardState, row + 1, col) && boardState.hasLetter(row + 1, col)) {
                return true;
            }
            if (inBounds(boardState, row, col - 1) && boardState.hasLetter(row, col - 1)) {
                return true;
            }
            if (inBounds(boardState, row, col + 1) && boardState.hasLetter(row, col + 1)) {
                return true;
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
            }
        }
        return false;
    }

<<<<<<< HEAD
=======
    // Vérifie la validité des mots croisés formés par les lettres posées.
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
    private static boolean checkCrossWords(List<int[]> newTiles, BoardState boardState, boolean mainWordHorizontal, Dictionary dictionary) {
        for (int[] tile : newTiles) {
            int row = tile[0];
            int col = tile[1];
            char placedLetter = (char) tile[2];
            StringBuilder wordBuilder = new StringBuilder();

            if (mainWordHorizontal) {
<<<<<<< HEAD
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
=======
                int r = row;
                // Descendre jusqu'au début de la colonne
                while (inBounds(boardState, r - 1, col) && boardState.hasLetter(r - 1, col)) {
                    r--;
                }
                // Construire le mot vertical
                while (inBounds(boardState, r, col) && (boardState.hasLetter(r, col) || r == row)) {
                    if (r == row) {
                        wordBuilder.append(placedLetter);
                    } else {
                        wordBuilder.append(boardState.getLetter(r, col));
                    }
                    r++;
                }
            } else {
                int c = col;
                // Aller au début de la ligne
                while (inBounds(boardState, row, c - 1) && boardState.hasLetter(row, c - 1)) {
                    c--;
                }
                // Construire le mot horizontal
                while (inBounds(boardState, row, c) && (boardState.hasLetter(row, c) || c == col)) {
                    if (c == col) {
                        wordBuilder.append(placedLetter);
                    } else {
                        wordBuilder.append(boardState.getLetter(row, c));
                    }
                    c++;
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
                }
            }

            String crossWord = wordBuilder.toString();
            if (!dictionary.validWord(crossWord)) {
                return false;
            }
        }
        return true;
    }

<<<<<<< HEAD
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
=======
    // Calcule le score du coup.
    private static int computeScore(String word, List<int[]> newTiles, BoardState boardState, boolean horizontal) {
        int totalScore = 0;
        int mainWordScore = 0;
        int wordMultiplier = 1;

        int startRow = newTiles.get(0)[0];
        int startCol = newTiles.get(0)[1];
        int r = startRow;
        int c = startCol;
        for (int i = 0; i < word.length(); i++) {
            char letter = Character.toUpperCase(word.charAt(i));
            // Utilisation de la map statique pointsLetter de la classe Letter.
            int letterValue = Letter.pointsLetter.get(letter);
            int letterMultiplier = 1;
            int cellWordMultiplier = 1;
            if (!boardState.hasLetter(r, c)) {
                letterMultiplier = getLetterMultiplier(r, c);
                cellWordMultiplier = getWordMultiplier(r, c);
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
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

<<<<<<< HEAD
        // Calcul des scores pour les mots croisés
        int crossScoreTotal = 0;
=======
        // Calcul du score des mots croisés.
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
        for (int[] tile : newTiles) {
            int row = tile[0];
            int col = tile[1];
            char letter = (char) tile[2];
            int crossScore = 0;
            int crossWordMultiplier = 1;
            if (horizontal) {
                int rStart = row;
<<<<<<< HEAD
                while (boardState.safeHasLetter(rStart - 1, col)) rStart--;
                int rEnd = row;
                while (boardState.safeHasLetter(rEnd + 1, col)) rEnd++;
                if (rEnd == rStart) continue;
                for (int rPos = rStart; rPos <= rEnd; rPos++) {
                    char ch = (rPos == row) ? letter : boardState.getLetter(rPos, col);
                    int value = Letter.pointsLetter.getOrDefault(Character.toUpperCase(ch), 0);
                    int mult = (rPos == row) ? MyPane.getLetterMultiplier(rPos, col) : 1;
=======
                // Chercher le début du mot vertical
                while (inBounds(boardState, rStart - 1, col) && boardState.hasLetter(rStart - 1, col)) {
                    rStart--;
                }
                int rEnd = row;
                while (inBounds(boardState, rEnd + 1, col) && boardState.hasLetter(rEnd + 1, col)) {
                    rEnd++;
                }
                for (int rPos = rStart; rPos <= rEnd; rPos++) {
                    char letterChar = (rPos == row) ? letter : boardState.getLetter(rPos, col);
                    int value = Letter.pointsLetter.get(Character.toUpperCase(letterChar));
                    int mult = (rPos == row) ? getLetterMultiplier(rPos, col) : 1;
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
                    crossScore += value * mult;
                    if (rPos == row) {
                        crossWordMultiplier *= MyPane.getWordMultiplier(rPos, col);
                    }
                }
<<<<<<< HEAD
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
=======
            } else {
                int cStart = col;
                while (inBounds(boardState, row, cStart - 1) && boardState.hasLetter(row, cStart - 1)) {
                    cStart--;
                }
                int cEnd = col;
                while (inBounds(boardState, row, cEnd + 1) && boardState.hasLetter(row, cEnd + 1)) {
                    cEnd++;
                }
                for (int cPos = cStart; cPos <= cEnd; cPos++) {
                    char letterChar = (cPos == col) ? letter : boardState.getLetter(row, cPos);
                    int value = Letter.pointsLetter.get(Character.toUpperCase(letterChar));
                    int mult = (cPos == col) ? getLetterMultiplier(row, cPos) : 1;
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
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

<<<<<<< HEAD
}
=======
    private static int getLetterMultiplier(int row, int col) {
        // Exemple simplifié : retour 1 (sans bonus).
        return 1;
    }

    private static int getWordMultiplier(int row, int col) {
        // Exemple simplifié : retour 1 (sans bonus).
        return 1;
    }

    /**
     * Applique le coup trouvé en mettant à jour le plateau et le chevalet du joueur.
     * Pour chaque lettre placée sur une case vide, on retire la lettre correspondante du chevalet,
     * on la place sur le plateau (en majuscules) et on refait le chevalet en tirant de nouvelles lettres via Letter.drawLetter().
     *
     * @param bestMove   Le coup à appliquer.
     * @param boardState L'état actuel du plateau (doit implémenter setLetter(int, int, char)).
     * @param rack       Le chevalet du joueur.
     */
    public static void applyMove(BestMove bestMove, BoardState boardState, List<Character> rack) {
        int row = bestMove.startRow;
        int col = bestMove.startCol;
        for (int i = 0; i < bestMove.word.length(); i++) {
            char letter = Character.toUpperCase(bestMove.word.charAt(i));
            // Si la case est vide, la lettre est issue du chevalet.
            if (!boardState.hasLetter(row, col)) {
                // Retire la lettre du chevalet, ou un joker ('*') si la lettre demandée n'y est pas.
                if (rack.contains(letter)) {
                    rack.remove((Character) letter);
                } else if (rack.contains('*')) {
                    rack.remove((Character) '*');
                }
                // Place la lettre sur le plateau en majuscules.
                boardState.setLetter(row, col, letter);
            }
            if (bestMove.horizontal) {
                col++;
            } else {
                row++;
            }
        }

        // Reconstituer le chevalet jusqu'à 7 lettres en tirant depuis le sac via Letter.drawLetter().
        while (rack.size() < 7) {
            Character newLetter = Letter.drawLetter();
            if (newLetter == null) { // Le sac est vide.
                break;
            }
            rack.add(newLetter);
        }
    }
}
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
