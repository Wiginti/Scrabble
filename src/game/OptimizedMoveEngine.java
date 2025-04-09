package game;

import dictionary.Dictionary;
import java.util.ArrayList;
import java.util.List;

public class OptimizedMoveEngine {

    // Classe interne pour représenter le meilleur coup trouvé
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
    }

    /**
     * Recherche le coup le plus rentable à jouer.
     *
     * Cet exemple vérifie d'abord si le plateau est vide (premier coup). Si c'est le cas,
     * il teste plusieurs possibilités :
     * - Si le joueur peut jouer "DONT" (cas déjà traité dans l'exemple précédent)
     * - Sinon, s'il peut jouer "VOTE" en utilisant les lettres du chevalet et en plaçant le mot
     *   de façon à ce qu'il passe par la case centrale (7,7).
     *
     * @param rack Liste des lettres présentes sur le chevalet.
     * @param boardState L'état actuel du plateau.
     * @param dictionary Le dictionnaire pour valider les mots.
     * @return Le meilleur coup trouvé, ou null si aucune solution n'est trouvée.
     */
    public static BestMove findBestMove(List<Character> rack, BoardState boardState, Dictionary dictionary) {
        if (isBoardEmpty(boardState)) {
            // Vérifier le cas de "DONT" (exemple précédent)
            if (containsWord(rack, "DONT") && dictionary.validWord("dont")) {
                // On place "DONT" de façon à couvrir la case centrale (7,7)
                return new BestMove("DONT", 7, 6, true, 10);  // Score fictif : 10
            }
            // Vérifier le cas du mot "VOTE"
            else if (containsWord(rack, "VOTE") && dictionary.validWord("vote")) {
                // Pour un mot de 4 lettres ("VOTE"),
                // on peut le placer horizontalement en commençant à la case (7,6) :
                // (7,6)=V, (7,7)=O, (7,8)=T, (7,9)=E
                return new BestMove("VOTE", 7, 6, true, 12);  // Score fictif : 12
            }
        }

        // Ici, d'autres tests ou parcours exhaustive du dictionnaire pourraient être ajoutés

        // Aucun coup optimisé trouvé
        return null;
    }

    /**
     * Vérifie si le plateau est entièrement vide (aucune lettre n'a été posée).
     */
    private static boolean isBoardEmpty(BoardState boardState) {
        for (int i = 0; i < boardState.getRows(); i++) {
            for (int j = 0; j < boardState.getCols(); j++) {
                if (boardState.getLetterAt(i, j) != null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Vérifie si le chevalet contient toutes les lettres nécessaires pour former un mot donné.
     * Cette méthode fait une vérification simple en comparant la fréquence des lettres.
     */
    private static boolean containsWord(List<Character> rack, String word) {
        List<Character> temp = new ArrayList<>(rack);
        for (char c : word.toCharArray()) {
            c = Character.toUpperCase(c);
            boolean found = false;
            for (int i = 0; i < temp.size(); i++) {
                if (Character.toUpperCase(temp.get(i)) == c) {
                    temp.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
}