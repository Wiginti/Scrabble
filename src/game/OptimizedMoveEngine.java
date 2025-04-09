package game;

import dictionary.Dictionary;
import java.util.ArrayList;
import java.util.List;

public class OptimizedMoveEngine {

    // Classe interne représentant le meilleur coup trouvé
    public static class BestMove {
        public final String word;
        public final int startRow;
        public final int startCol;
        public final boolean horizontal;
        public final int score;

        public BestMove(String word, int startRow, int startCol, boolean horizontal, int score) {
            this.word = word;
            this.startRow = startRow;
            this.startCol = startCol;
            this.horizontal = horizontal;
            this.score = score;
        }

        @Override
        public String toString(){
            return word + " @(" + startRow + "," + startCol + ") " + (horizontal ? "Horizontal" : "Vertical") + " - " + score + " points";
        }
    }

    /**
     * Parcourt l'intégralité du dictionnaire pour trouver le meilleur coup possible.
     *
     * @param rack       Liste des lettres du chevalet du joueur.
     * @param boardState L'état actuel du plateau.
     * @param dictionary Le dictionnaire utilisé pour la validation des mots.
     * @return Le coup optimisé, ou null si aucun coup n'est trouvé.
     */
    public static BestMove findBestMove(List<Character> rack, BoardState boardState, Dictionary dictionary) {
        BestMove bestMove = null;
        int bestScore = 0;
        int size = boardState.getSize();

        // Récupérer l'ensemble des mots du dictionnaire
        List<String> allWords = dictionary.getWords();

        // Parcourir tous les mots du dictionnaire
        for (String word : allWords) {
            int wordLength = word.length();
            // Essayer les deux orientations : horizontale et verticale
            for (boolean horizontal : new boolean[]{true, false}) {
                // Définir les bornes de départ de manière à ce que le mot tienne dans le plateau
                int maxRow = horizontal ? size : size - wordLength + 1;
                int maxCol = horizontal ? size - wordLength + 1 : size;
                for (int startRow = 0; startRow < maxRow; startRow++) {
                    for (int startCol = 0; startCol < maxCol; startCol++) {

                        // Pour un plateau non vide, le nouveau mot doit être connecté à une lettre existante.
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
                                }
                            } else {
                                if (!(startCol == center && (startRow <= center && center < startRow + wordLength))) {
                                    continue;
                                }
                            }
                        }

                        // Copie temporaire du chevalet pour simuler la consommation de lettres.
                        List<Character> tempRack = new ArrayList<>(rack);
                        boolean placementValide = true;
                        int currRow = startRow;
                        int currCol = startCol;
                        // Liste pour stocker les positions des nouvelles lettres posées.
                        // Chaque élément int[] contient : {row, col, lettre (en int), flag joker (0 ou 1)}.
                        List<int[]> newTiles = new ArrayList<>();

                        // Parcours du mot lettre par lettre.
                        for (int i = 0; i < wordLength; i++) {
                            char letter = Character.toUpperCase(word.charAt(i));
                            if (boardState.hasLetter(currRow, currCol)) {
                                // La case est déjà occupée : la lettre existante doit correspondre.
                                char existingLetter = Character.toUpperCase(boardState.getLetter(currRow, currCol));
                                if (existingLetter != letter) {
                                    placementValide = false;
                                    break;
                                }
                                // Sinon, on ne consomme pas de lettre du chevalet.
                            } else {
                                // La case est vide : tenter de consommer la lettre depuis le chevalet.
                                int index = tempRack.indexOf(letter);
                                if (index == -1) {
                                    // Pas de lettre trouvée dans le chevalet : essayer un joker représenté par '*'.
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
                        } // Fin de la boucle sur les lettres.

                        if (!placementValide) {
                            continue;
                        }

                        // Vérification ajoutée : si aucune nouvelle lettre n'est posée, on ignore ce coup.
                        if (newTiles.isEmpty()) {
                            continue;
                        }

                        // Vérifier la validité des mots croisés formés par les nouvelles lettres posées.
                        if (!checkCrossWords(newTiles, boardState, horizontal, dictionary)) {
                            continue;
                        }

                        // Calculer le score du coup (mot principal + mots croisés).
                        int score = computeScore(word, newTiles, boardState, horizontal);
                        // Bonus de 50 points si toutes les lettres du chevalet ont été utilisées.
                        if (tempRack.isEmpty()) {
                            score += 50;
                        }

                        // Mettre à jour le meilleur coup si ce score est supérieur au meilleur trouvé.
                        if (score > bestScore) {
                            bestScore = score;
                            bestMove = new BestMove(word, startRow, startCol, horizontal, score);
                        }
                    }
                }
            }
        }
        return bestMove;
    }

    // Vérifie qu'au moins une case du mot touche une lettre existante (pour un plateau non vide).
    private static boolean isConnected(int startRow, int startCol, boolean horizontal, int wordLength, BoardState boardState) {
        for (int i = 0; i < wordLength; i++) {
            int row = horizontal ? startRow : startRow + i;
            int col = horizontal ? startCol + i : startCol;
            if (boardState.hasLetter(row, col)) {
                return true;
            }
            // Vérifie également les cases adjacentes pour établir la connexion.
            if (horizontal) {
                if ((row > 0 && boardState.hasLetter(row - 1, col)) ||
                        (row < boardState.getSize() - 1 && boardState.hasLetter(row + 1, col))) {
                    return true;
                }
            } else {
                if ((col > 0 && boardState.hasLetter(row, col - 1)) ||
                        (col < boardState.getSize() - 1 && boardState.hasLetter(row, col + 1))) {
                    return true;
                }
            }
        }
        return false;
    }

    // Vérifie la validité des mots croisés formés par chaque lettre nouvellement posée.
    private static boolean checkCrossWords(List<int[]> newTiles, BoardState boardState, boolean mainWordHorizontal, Dictionary dictionary) {
        for (int[] tile : newTiles) {
            int row = tile[0];
            int col = tile[1];
            char placedLetter = (char) tile[2];
            StringBuilder wordBuilder = new StringBuilder();

            if (mainWordHorizontal) {
                // Constituer le mot vertical passant par (row, col).
                int r = row;
                while (r > 0 && boardState.hasLetter(r - 1, col)) {
                    r--;
                }
                while (r < boardState.getRows() && (boardState.hasLetter(r, col) || r == row)) {
                    if (r == row) {
                        wordBuilder.append(placedLetter);
                    } else {
                        wordBuilder.append(boardState.getLetter(r, col));
                    }
                    r++;
                }
            } else {
                // Constituer le mot horizontal passant par (row, col).
                int c = col;
                while (c > 0 && boardState.hasLetter(row, c - 1)) {
                    c--;
                }
                while (c < boardState.getCols() && (boardState.hasLetter(row, c) || c == col)) {
                    if (c == col) {
                        wordBuilder.append(placedLetter);
                    } else {
                        wordBuilder.append(boardState.getLetter(row, c));
                    }
                    c++;
                }
            }
            String crossWord = wordBuilder.toString();
            if (crossWord.length() > 1 && !dictionary.validWord(crossWord)) {
                return false;
            }
        }
        return true;
    }

    // Calcule le score du coup en combinant le mot principal et les mots croisés.
    private static int computeScore(String word, List<int[]> newTiles, BoardState boardState, boolean horizontal) {
        int totalScore = 0;
        int mainWordScore = 0;
        int wordMultiplier = 1;

        // Récupère les coordonnées de départ du mot principal à partir de newTiles.
        // (newTiles n'est pas vide grâce à la vérification faite dans findBestMove)
        int startRow = newTiles.get(0)[0];
        int startCol = newTiles.get(0)[1];
        int r = startRow;
        int c = startCol;
        for (int i = 0; i < word.length(); i++) {
            char letter = Character.toUpperCase(word.charAt(i));
            // Récupère la valeur en points de la lettre via la table de la classe Letter.
            int letterValue = Letter.pointsLetter.get(Character.toUpperCase(letter));
            int letterMultiplier = 1;
            int cellWordMultiplier = 1;
            // Appliquer les bonus uniquement pour les lettres posées ce tour-ci.
            if (!boardState.hasLetter(r, c)) {
                letterMultiplier = getLetterMultiplier(r, c);
                cellWordMultiplier = getWordMultiplier(r, c);
            }
            mainWordScore += letterValue * letterMultiplier;
            wordMultiplier *= cellWordMultiplier;
            if (horizontal) {
                c++;
            } else {
                r++;
            }
        }
        mainWordScore *= wordMultiplier;
        totalScore += mainWordScore;

        // Calculer le score des mots croisés pour chaque nouvelle lettre posée.
        for (int[] tile : newTiles) {
            int row = tile[0];
            int col = tile[1];
            char letter = (char) tile[2];
            int crossScore = 0;
            int crossWordMultiplier = 1;
            StringBuilder crossWordBuilder = new StringBuilder();
            if (horizontal) {
                // Mot vertical formé par la lettre posée.
                int rStart = row;
                while (rStart > 0 && boardState.hasLetter(rStart - 1, col)) rStart--;
                int rEnd = row;
                while (rEnd < boardState.getRows() - 1 && boardState.hasLetter(rEnd + 1, col)) rEnd++;
                for (int rPos = rStart; rPos <= rEnd; rPos++) {
                    char letterChar;
                    if (rPos == row) {
                        letterChar = letter;
                    } else {
                        letterChar = boardState.getLetter(rPos, col);
                    }
                    int value = Letter.pointsLetter.get(Character.toUpperCase(letterChar));
                    int mult = (rPos == row) ? getLetterMultiplier(rPos, col) : 1;
                    crossScore += value * mult;
                    if (rPos == row) {
                        crossWordMultiplier *= getWordMultiplier(rPos, col);
                    }
                    crossWordBuilder.append(letterChar);
                }
            } else {
                // Mot horizontal formé par la lettre posée.
                int cStart = col;
                while (cStart > 0 && boardState.hasLetter(row, cStart - 1)) cStart--;
                int cEnd = col;
                while (cEnd < boardState.getCols() - 1 && boardState.hasLetter(row, cEnd + 1)) cEnd++;
                for (int cPos = cStart; cPos <= cEnd; cPos++) {
                    char letterChar;
                    if (cPos == col) {
                        letterChar = letter;
                    } else {
                        letterChar = boardState.getLetter(row, cPos);
                    }
                    int value = Letter.pointsLetter.get(Character.toUpperCase(letterChar));
                    int mult = (cPos == col) ? getLetterMultiplier(row, cPos) : 1;
                    crossScore += value * mult;
                    if (cPos == col) {
                        crossWordMultiplier *= getWordMultiplier(row, cPos);
                    }
                    crossWordBuilder.append(letterChar);
                }
            }
            if (crossWordBuilder.length() > 1) {
                totalScore += crossScore * crossWordMultiplier;
            }
        }
        return totalScore;
    }

    // Méthodes d'exemple pour récupérer les multiplicateurs de lettres et de mots.
    // Adaptez-les selon votre logique (par exemple en fonction de la couleur de la case dans MyPane).
    private static int getLetterMultiplier(int row, int col) {
        // Exemple : retourne 3 pour "lettre triple", sinon 1.
        return 1;
    }

    private static int getWordMultiplier(int row, int col) {
        // Exemple : retourne 3 pour "mot triple", sinon 1.
        return 1;
    }
}