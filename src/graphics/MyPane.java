package graphics;

import game.Letter;
import dictionary.Dictionary;
import game.ScoreManager;
import game.BoardState;
import game.OptimizedMoveEngine;

import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;

import java.io.IOException;
import java.util.*;

public class MyPane extends Pane {

    private static final int NUM_ROWS = 15;
    private static final int NUM_COLS = 15;
    private static final double CELL_SIZE = 40;

    private static final int CENTER_ROW = 7;
    private static final int CENTER_COL = 7;

    private Dictionary dictionary;
    private int numberOfPlayers;

    // Un chevalet par joueur
    private List<HBox> playerRacks = new ArrayList<>();

    // Un ScoreManager par joueur
    private List<ScoreManager> scoreManagers = new ArrayList<>();
    // Un ScorePane par joueur
    private List<ScorePane> scorePanes = new ArrayList<>();

    // Indique quel joueur (index) est en train de jouer
    private int currentPlayerIndex = 0;

    // Référence à l'application principale
    private BasicScene mainApp;

    // Composants du plateau
    private GridPane grid = new GridPane();
    private Button validateButton;
    private Button resetButton;
    private Button nextPlayerButton;

    // Bouton "Optimisé"
    private Button optimizedButton;

    // Indique si c'est la première pose
    private boolean firstWordPlaced = false;

    // Lettres posées ce tour-ci
    private Map<Label, StackPane> placedLettersMap = new HashMap<>();
    // Lettres validées (définitivement posées sur le plateau)
    private List<Label> validatedLetters = new ArrayList<>();

    // Mots validés enregistrés
    @SuppressWarnings("unused")
    private List<String> validatedWords = new ArrayList<>();

    public MyPane(int numberOfPlayers, BasicScene mainApp) {
        super();
        this.numberOfPlayers = numberOfPlayers;
        this.mainApp = mainApp;

        // Chargement du dictionnaire
        try {
            dictionary = new Dictionary("french");
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du dictionnaire.");
            return;
        }

        // Initialisation de la grille
        grid.setHgap(1);
        grid.setVgap(1);

        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                StackPane cellPane = new StackPane();
                cellPane.setPrefSize(CELL_SIZE, CELL_SIZE);

                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
                cell.setStroke(Color.BLACK);
                cell.setFill(getCellColor(row, col));
                cellPane.getChildren().add(cell);

                // Gestion du drag & drop
                cellPane.setOnDragOver(event -> {
                    if (event.getGestureSource() instanceof Label && event.getDragboard().hasString()) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                    event.consume();
                });

                // Gestion du drop avec gestion du joker
                cellPane.setOnDragDropped(event -> {
                    Dragboard db = event.getDragboard();
                    if (db.hasString()) {
                        String letter = db.getString();
                        Label letterLabel = createDraggableLetter(letter);

                        if ("*".equals(letter)) {
                            String chosen = promptForJokerLetter();
                            if (!"*".equals(chosen)) {
                                letterLabel.setText(chosen);
                                letterLabel.setUserData("joker");
                            }
                        }

                        if (letterLabel == null || cellPane == null) {
                            event.setDropCompleted(false);
                            return;
                        }

                        cellPane.getChildren().add(letterLabel);

                        Label draggedLabel = (Label) event.getGestureSource();
                        if (draggedLabel == null) {
                            event.setDropCompleted(false);
                            return;
                        }

                        StackPane previousTile = (StackPane) draggedLabel.getParent();
                        if (previousTile != null) {
                            previousTile.getChildren().remove(draggedLabel);
                        }

                        StackPane originalTile = placedLettersMap.getOrDefault(draggedLabel, previousTile);
                        placedLettersMap.remove(draggedLabel);
                        placedLettersMap.put(letterLabel, originalTile);

                        letterLabel.setOnDragDetected(event2 -> {
                            Dragboard db2 = letterLabel.startDragAndDrop(TransferMode.MOVE);
                            ClipboardContent content = new ClipboardContent();
                            content.putString(letterLabel.getText());
                            db2.setContent(content);
                            event2.consume();
                        });

                        checkPlacementRules();
                        event.setDropCompleted(true);
                    } else {
                        event.setDropCompleted(false);
                    }
                    event.consume();
                });

                grid.add(cellPane, col, row);
            }
        }

        // Initialisation des ScoreManagers et ScorePanes
        for (int i = 0; i < numberOfPlayers; i++) {
            ScoreManager sm = new ScoreManager();
            scoreManagers.add(sm);
            ScorePane sp = new ScorePane(sm);
            scorePanes.add(sp);
        }

        // Création d'un chevalet pour chaque joueur
        for (int i = 0; i < numberOfPlayers; i++) {
            HBox rack = new HBox(5);
            rack.setVisible(false);
            initializeChevalet(rack);
            playerRacks.add(rack);
            this.getChildren().add(rack);
        }

        // Affichage du chevalet du premier joueur
        playerRacks.get(0).setVisible(true);

        // Bouton "Annuler"
        resetButton = new Button("Annuler");
        resetButton.getStyleClass().add("annuler-btn");
        resetButton.setOnAction(e -> resetBoard());

        // Bouton "Valider"
        validateButton = new Button("Valider");
        validateButton.getStyleClass().add("valider-btn");
        validateButton.setOnAction(e -> validateBoard());
        validateButton.setDisable(true);

        // Bouton "Joueur suivant"
        nextPlayerButton = new Button("Joueur suivant");
        nextPlayerButton.setOnAction(e -> switchToNextPlayer());

        // Bouton "Optimisé" pour lancer l'algorithme d'optimisation
        optimizedButton = new Button("Optimisé");
        optimizedButton.setOnAction(e -> {
            List<Character> rackLetters = getRackLettersForCurrentPlayer();
            BoardState boardState = buildBoardState();
            OptimizedMoveEngine.BestMove bestMove = OptimizedMoveEngine.findBestMove(rackLetters, boardState, dictionary);

            if (bestMove != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Coup optimisé");
                alert.setHeaderText("Mot proposé : " + bestMove.word);
                
                StringBuilder alertContent = new StringBuilder();
                alertContent.append("Ce coup rapporte un total de ")
                            .append(bestMove.score)
                            .append(" points.\n")
                            .append("Dont :\n")
                            .append("  - Mot principal : ")
                            .append(bestMove.mainScore)
                            .append(" points\n")
                            .append("  - Mots croisés   : ")
                            .append(bestMove.crossScore)
                            .append(" points\n")
                            .append("Voulez-vous le jouer ?");
                            
                alert.setContentText(alertContent.toString());
                
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    placeOptimizedMove(bestMove);
                }
            }

        });

        // Positionnement des composants
        grid.setLayoutX(0);
        grid.setLayoutY(0);
        for (int i = 0; i < numberOfPlayers; i++) {
            HBox rack = playerRacks.get(i);
            rack.setLayoutX(155);
            rack.setLayoutY(NUM_ROWS * CELL_SIZE + 60 + (i * 60));
        }
        resetButton.setLayoutX(180);
        resetButton.setLayoutY(NUM_ROWS * CELL_SIZE + 120 + (numberOfPlayers - 1) * 60);
        validateButton.setLayoutX(320);
        validateButton.setLayoutY(NUM_ROWS * CELL_SIZE + 120 + (numberOfPlayers - 1) * 60);
        nextPlayerButton.setLayoutX(460);
        nextPlayerButton.setLayoutY(NUM_ROWS * CELL_SIZE + 120 + (numberOfPlayers - 1) * 60);
        optimizedButton.setLayoutX(600);
        optimizedButton.setLayoutY(NUM_ROWS * CELL_SIZE + 120 + (numberOfPlayers - 1) * 60);

        this.getChildren().addAll(grid, resetButton, validateButton, nextPlayerButton, optimizedButton);

        // Chargement du CSS si présent
        try {
            this.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {
            // style.css introuvable
        }
    }

    public List<ScorePane> getAllScorePanes() {
        return scorePanes;
    }

    private void switchToNextPlayer() {
        validateButton.setDisable(true);
        playerRacks.get(currentPlayerIndex).setVisible(false);
        currentPlayerIndex = (currentPlayerIndex + 1) % numberOfPlayers;
        playerRacks.get(currentPlayerIndex).setVisible(true);
        if (mainApp != null) {
            mainApp.updateWindowTitle(currentPlayerIndex + 1);
        }
    }

    private void checkPlacementRules() {
        if (placedLettersMap.isEmpty()) {
            validateButton.setDisable(true);
            return;
        }

        List<Integer> rows = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        boolean containsCenter = false;

        for (Label label : placedLettersMap.keySet()) {
            StackPane parent = (StackPane) label.getParent();
            Integer row = GridPane.getRowIndex(parent);
            Integer col = GridPane.getColumnIndex(parent);
            if (row == null || col == null) continue;
            rows.add(row);
            cols.add(col);
            if (row == CENTER_ROW && col == CENTER_COL) {
                containsCenter = true;
            }
        }

        boolean alignedHorizontally = (new HashSet<>(rows).size() == 1);
        boolean alignedVertically   = (new HashSet<>(cols).size() == 1);

        boolean isContinuous = false;
        if (alignedHorizontally) {
            int row = rows.get(0);
            int minCol = Collections.min(cols);
            int maxCol = Collections.max(cols);
            isContinuous = isLineContinuous(true, row, minCol, maxCol);
        } else if (alignedVertically) {
            int col = cols.get(0);
            int minRow = Collections.min(rows);
            int maxRow = Collections.max(rows);
            isContinuous = isLineContinuous(false, col, minRow, maxRow);
        }

        if (!firstWordPlaced && !containsCenter) {
            validateButton.setDisable(true);
            return;
        }

        if (!(alignedHorizontally || alignedVertically) || !isContinuous) {
            validateButton.setDisable(true);
            return;
        }

        if (firstWordPlaced) {
            if (!isConnectedToValidated()) {
                validateButton.setDisable(true);
                return;
            }
        }

        validateButton.setDisable(false);
    }

    private boolean isLineContinuous(boolean horizontal, int fixed, int min, int max) {
        for (int i = min; i <= max; i++) {
            String letter = horizontal ? getLetterAt(fixed, i) : getLetterAt(i, fixed);
            if (letter == null) return false;
        }
        return true;
    }

    private boolean isConnectedToValidated() {
        for (Label label : placedLettersMap.keySet()) {
            StackPane parent = (StackPane) label.getParent();
            Integer row = GridPane.getRowIndex(parent);
            Integer col = GridPane.getColumnIndex(parent);
            if (isValidatedLetterAt(row - 1, col) ||
                    isValidatedLetterAt(row + 1, col) ||
                    isValidatedLetterAt(row, col - 1) ||
                    isValidatedLetterAt(row, col + 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidatedLetterAt(int row, int col) {
        for (Label validatedLabel : validatedLetters) {
            Node parent = validatedLabel.getParent();
            if (parent != null) {
                Integer r = GridPane.getRowIndex(parent);
                Integer c = GridPane.getColumnIndex(parent);
                if (r != null && c != null && r == row && c == col) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateBoard() {
        List<String> formedWords = new ArrayList<>();
        List<Integer> wordsScores = new ArrayList<>();

        if (placedLettersMap.isEmpty()) {
            validateButton.setDisable(true);
            return;
        }

        // Déterminer l'orientation du placement
        Set<Integer> rows = new HashSet<>();
        Set<Integer> cols = new HashSet<>();
        for (Label label : placedLettersMap.keySet()) {
            StackPane parent = (StackPane) label.getParent();
            Integer row = GridPane.getRowIndex(parent);
            Integer col = GridPane.getColumnIndex(parent);
            if (row != null) rows.add(row);
            if (col != null) cols.add(col);
        }
        boolean horizontalPlacement = (rows.size() == 1);
        boolean verticalPlacement = (cols.size() == 1);

        // Calcul du mot principal et son score
        String mainWord = "";
        int mainScore = 0;
        if (horizontalPlacement) {
            int fixedRow = rows.iterator().next();
            int minCol = Integer.MAX_VALUE;
            for (Label label : placedLettersMap.keySet()) {
                StackPane parent = (StackPane) label.getParent();
                int col = GridPane.getColumnIndex(parent);
                minCol = Math.min(minCol, col);
            }
            int[] horizRange = getHorizontalRange(fixedRow, minCol);
            mainWord = getWordString(true, fixedRow, horizRange[0], horizRange[1]);
            mainScore = computeWordScore(true, fixedRow, horizRange[0], horizRange[1]);
        } else if (verticalPlacement) {
            int fixedCol = cols.iterator().next();
            int minRow = Integer.MAX_VALUE;
            for (Label label : placedLettersMap.keySet()) {
                StackPane parent = (StackPane) label.getParent();
                int row = GridPane.getRowIndex(parent);
                minRow = Math.min(minRow, row);
            }
            int[] vertRange = getVerticalRange(minRow, fixedCol);
            mainWord = getWordString(false, fixedCol, vertRange[0], vertRange[1]);
            mainScore = computeWordScore(false, fixedCol, vertRange[0], vertRange[1]);
        } else {
            // Cas d'un unique jeton ou placement irrégulier :
            StackPane parent = (StackPane) placedLettersMap.keySet().iterator().next().getParent();
            int r = GridPane.getRowIndex(parent);
            int c = GridPane.getColumnIndex(parent);
            int[] horizRange = getHorizontalRange(r, c);
            String horizWord = getWordString(true, r, horizRange[0], horizRange[1]);
            int horizScore = (horizWord.length() > 1) ? computeWordScore(true, r, horizRange[0], horizRange[1]) : 0;
            int[] vertRange = getVerticalRange(r, c);
            String vertWord = getWordString(false, c, vertRange[0], vertRange[1]);
            int vertScore = (vertWord.length() > 1) ? computeWordScore(false, c, vertRange[0], vertRange[1]) : 0;
            // Ici, on additionne les scores obtenus dans les deux directions
            mainWord = horizWord.length() >= vertWord.length() ? horizWord : vertWord;
            mainScore = horizScore + vertScore;
        }
        formedWords.add(mainWord);
        wordsScores.add(mainScore);

        // Calcul des mots croisés. Selon l'orientation du placement, pour chaque nouvelle lettre on
        // va chercher le mot croisé dans la direction perpendiculaire.
        Set<String> crossWordsComputed = new HashSet<>();
        int crossTotalScore = 0;
        for (Label label : placedLettersMap.keySet()) {
            StackPane parent = (StackPane) label.getParent();
            int r = GridPane.getRowIndex(parent);
            int c = GridPane.getColumnIndex(parent);
            if (horizontalPlacement) { // vérification verticale
                int[] vertRange = getVerticalRange(r, c);
                String crossWord = getWordString(false, c, vertRange[0], vertRange[1]);
                if (crossWord.length() > 1 && !crossWordsComputed.contains(crossWord)) {
                    int crossScore = computeWordScore(false, c, vertRange[0], vertRange[1]);
                    formedWords.add(crossWord);
                    wordsScores.add(crossScore);
                    crossWordsComputed.add(crossWord);
                    crossTotalScore += crossScore;
                }
            } else if (verticalPlacement) { // vérification horizontale
                int[] horizRange = getHorizontalRange(r, c);
                String crossWord = getWordString(true, r, horizRange[0], horizRange[1]);
                if (crossWord.length() > 1 && !crossWordsComputed.contains(crossWord)) {
                    int crossScore = computeWordScore(true, r, horizRange[0], horizRange[1]);
                    formedWords.add(crossWord);
                    wordsScores.add(crossScore);
                    crossWordsComputed.add(crossWord);
                    crossTotalScore += crossScore;
                }
            } else {
                // Pour un seul jeton, on a déjà traité les deux directions.
            }
        }

        int totalPoints = mainScore + crossTotalScore;

        // Vérification de la validité de tous les mots formés
        for (String word : formedWords) {
            if (!dictionary.validWord(word)) {
                showError("Le mot '" + word + "' n'est pas dans le dictionnaire !");
                return;
            }
        }

        // Mise à jour du score et affichage du message
        scoreManagers.get(currentPlayerIndex).addPoints(totalPoints);
        scorePanes.get(currentPlayerIndex).refreshScore();

        StringBuilder msg = new StringBuilder("Bravo !\nLes mots validés :\n");
        for (int i = 0; i < formedWords.size(); i++) {
            msg.append(formedWords.get(i))
               .append(" (")
               .append(wordsScores.get(i))
               .append(" points)\n");
        }
        msg.append("Total : ").append(totalPoints).append(" points.");
        showSuccess(msg.toString());

        for (Label letterLabel : placedLettersMap.keySet()) {
            letterLabel.setOnDragDetected(null);
            validatedLetters.add(letterLabel);
        }
        placedLettersMap.clear();
        firstWordPlaced = true;
        validateButton.setDisable(true);
        refillRack(playerRacks.get(currentPlayerIndex));
    }


    private String getLetterAt(int row, int col) {
        for (Node node : grid.getChildren()) {
            Integer r = GridPane.getRowIndex(node);
            Integer c = GridPane.getColumnIndex(node);
            if (r != null && c != null && r == row && c == col) {
                if (node instanceof StackPane) {
                    for (Node child : ((StackPane) node).getChildren()) {
                        if (child instanceof Label) {
                            return ((Label) child).getText();
                        }
                    }
                }
            }
        }
        return null;
    }

    private void resetBoard() {
        List<Label> lettersToRemove = new ArrayList<>(placedLettersMap.keySet());
        for (Label letterLabel : lettersToRemove) {
            StackPane originalTile = placedLettersMap.get(letterLabel);
            if (originalTile == null) continue;
            StackPane currentTile = (StackPane) letterLabel.getParent();
            if (currentTile != null) {
                currentTile.getChildren().remove(letterLabel);
            }
            if (letterLabel.getText() != null) {
                Label restoredLetter = createDraggableLetter(letterLabel.getText());
                originalTile.getChildren().add(restoredLetter);
            }
        }
        placedLettersMap.clear();
        validateButton.setDisable(true);
    }

    private void initializeChevalet(HBox rack) {
        rack.getChildren().clear();
        for (int i = 0; i < 7; i++) {
            StackPane tilePane = new StackPane();
            tilePane.setPrefSize(CELL_SIZE, CELL_SIZE);
            Rectangle tile = new Rectangle(CELL_SIZE, CELL_SIZE);
            tile.setStroke(Color.BLACK);
            tile.setFill(Color.BURLYWOOD);
            tilePane.getChildren().add(tile);
            Character drawnLetter = Letter.drawLetter();
            if (drawnLetter != null) {
                Label letterLabel = createDraggableLetter(drawnLetter.toString());
                tilePane.getChildren().add(letterLabel);
            }
            rack.getChildren().add(tilePane);
        }
    }

    private void refillRack(HBox rack) {
        for (Node node : rack.getChildren()) {
            if (node instanceof StackPane) {
                StackPane tilePane = (StackPane) node;
                boolean hasLetter = false;
                for (Node child : tilePane.getChildren()) {
                    if (child instanceof Label) {
                        hasLetter = true;
                        break;
                    }
                }
                if (!hasLetter) {
                    Character drawn = Letter.drawLetter();
                    if (drawn != null) {
                        Label letterLabel = createDraggableLetter(drawn.toString());
                        tilePane.getChildren().add(letterLabel);
                    }
                }
            }
        }
    }

    public static int getLetterMultiplier(int row, int col) {
        Color color = getCellColor(row, col);
        if (color.equals(Color.DARKBLUE)) {
            return 3;
        } else if (color.equals(Color.LIGHTBLUE)) {
            return 2;
        }
        return 1;
    }

    public static int getWordMultiplier(int row, int col) {
        Color color = getCellColor(row, col);
        if (color.equals(Color.RED)) {
            return 3;
        } else if (color.equals(Color.PINK)) {
            return 2;
        }
        return 1;
    }

    private Label createDraggableLetter(String letter) {
        // Forcer l'affichage de la lettre en majuscule
        Label letterLabel = new Label(letter.toUpperCase());
        letterLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
        letterLabel.setOnDragDetected(event -> {
            Dragboard db = letterLabel.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(letterLabel.getText());
            db.setContent(content);
            event.consume();
        });
        return letterLabel;
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Erreur de validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mot validé !");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static Color getCellColor(int row, int col) {
        if ((row == 0 && (col == 0 || col == 7 || col == 14)) ||
                (row == 7 && (col == 0 || col == 14)) ||
                (row == 14 && (col == 0 || col == 7 || col == 14))) {
            return Color.RED;
        }
        if ((row == 1 && (col == 1 || col == 13)) ||
                (row == 2 && (col == 2 || col == 12)) ||
                (row == 3 && (col == 3 || col == 11)) ||
                (row == 4 && (col == 4 || col == 10)) ||
                (row == 7 && col == 7) ||
                (row == 10 && (col == 4 || col == 10)) ||
                (row == 11 && (col == 3 || col == 11)) ||
                (row == 12 && (col == 2 || col == 12)) ||
                (row == 13 && (col == 1 || col == 13))) {
            return Color.PINK;
        }
        if ((row == 1 && (col == 5 || col == 9)) ||
                (row == 5 && (col == 1 || col == 5 || col == 9 || col == 13)) ||
                (row == 9 && (col == 1 || col == 5 || col == 9 || col == 13)) ||
                (row == 13 && (col == 5 || col == 9))) {
            return Color.DARKBLUE;
        }
        if ((row == 0 && (col == 3 || col == 11)) ||
                (row == 2 && (col == 6 || col == 8)) ||
                (row == 3 && (col == 0 || col == 7 || col == 14)) ||
                (row == 6 && (col == 2 || col == 6 || col == 8 || col == 12)) ||
                (row == 7 && (col == 3 || col == 11)) ||
                (row == 8 && (col == 2 || col == 6 || col == 8 || col == 12)) ||
                (row == 11 && (col == 0 || col == 7 || col == 14)) ||
                (row == 12 && (col == 6 || col == 8)) ||
                (row == 14 && (col == 3 || col == 11))) {
            return Color.LIGHTBLUE;
        }
        return Color.BEIGE;
    }

    private String promptForJokerLetter() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Choix du Joker");
        dialog.setHeaderText("Lettre du Joker");
        dialog.setContentText("Entrez la lettre que vous souhaitez attribuer au joker :");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().isEmpty()) {
            return result.get().substring(0, 1).toUpperCase();
        }
        return "*";
    }

    private boolean isJokerTileAt(int row, int col) {
        for (Node node : grid.getChildren()) {
            Integer r = GridPane.getRowIndex(node);
            Integer c = GridPane.getColumnIndex(node);
            if (r != null && c != null && r == row && c == col) {
                if (node instanceof StackPane) {
                    for (Node child : ((StackPane) node).getChildren()) {
                        if (child instanceof Label) {
                            Label label = (Label) child;
                            if ("joker".equals(label.getUserData())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private int[] getHorizontalRange(int row, int col) {
        int start = col;
        while (start > 0 && getLetterAt(row, start - 1) != null) {
            start--;
        }
        int end = col;
        while (end < NUM_COLS - 1 && getLetterAt(row, end + 1) != null) {
            end++;
        }
        return new int[]{start, end};
    }

    private int[] getVerticalRange(int row, int col) {
        int start = row;
        while (start > 0 && getLetterAt(start - 1, col) != null) {
            start--;
        }
        int end = row;
        while (end < NUM_ROWS - 1 && getLetterAt(end + 1, col) != null) {
            end++;
        }
        return new int[]{start, end};
    }

    private String getWordString(boolean horizontal, int fixed, int start, int end) {
        StringBuilder word = new StringBuilder();
        for (int pos = start; pos <= end; pos++) {
            String letter = horizontal ? getLetterAt(fixed, pos) : getLetterAt(pos, fixed);
            if (letter != null) {
                word.append(letter);
            }
        }
        return word.toString();
    }

    private int computeWordScore(boolean horizontal, int fixed, int start, int end) {
        int score = 0;
        int wordMultiplier = 1;
        for (int pos = start; pos <= end; pos++) {
            int row, col;
            if (horizontal) {
                row = fixed;
                col = pos;
            } else {
                row = pos;
                col = fixed;
            }
            String letterStr = getLetterAt(row, col);
            if (letterStr == null || letterStr.isEmpty()) continue;
            char letter = letterStr.charAt(0);
            int letterValue = Letter.pointsLetter.get(Character.toUpperCase(letter));
            if (isNewTileAt(row, col) && isJokerTileAt(row, col)) {
                letterValue = 0;
            }
            int letterMult = 1;
            int cellWordMult = 1;
            if (isNewTileAt(row, col)) {
                letterMult = getLetterMultiplier(row, col);
                cellWordMult = getWordMultiplier(row, col);
            }
            score += letterValue * letterMult;
            wordMultiplier *= cellWordMult;
        }
        return score * wordMultiplier;
    }

    private boolean isNewTileAt(int row, int col) {
        for (Label label : placedLettersMap.keySet()) {
            StackPane parent = (StackPane) label.getParent();
            Integer r = GridPane.getRowIndex(parent);
            Integer c = GridPane.getColumnIndex(parent);
            if (r != null && c != null && r == row && c == col) {
                return true;
            }
        }
        return false;
    }

    private List<Character> getRackLettersForCurrentPlayer() {
        List<Character> letters = new ArrayList<>();
        HBox currentRack = playerRacks.get(currentPlayerIndex);
        for (Node node : currentRack.getChildren()) {
            if (node instanceof StackPane) {
                for (Node child : ((StackPane) node).getChildren()) {
                    if (child instanceof Label) {
                        String letter = ((Label) child).getText();
                        if (letter != null && !letter.isEmpty()) {
                            letters.add(letter.charAt(0));
                        }
                    }
                }
            }
        }
        return letters;
    }

    private BoardState buildBoardState() {
        String[][] gridState = new String[NUM_ROWS][NUM_COLS];
        for (Node node : grid.getChildren()) {
            Integer row = GridPane.getRowIndex(node);
            Integer col = GridPane.getColumnIndex(node);
            if (row != null && col != null && node instanceof StackPane) {
                for (Node child : ((StackPane) node).getChildren()) {
                    if (child instanceof Label) {
                        gridState[row][col] = ((Label) child).getText();
                    }
                }
            }
        }
        return new BoardState(gridState);
    }

<<<<<<< HEAD
    // Méthode de placement d'un coup optimisé
=======

    /**
     * Place automatiquement le coup optimisé sur le plateau.
     *
     * @param bestMove Le meilleur mouvement trouvé par l'algorithme d'optimisation.
     */
>>>>>>> 442b3cfef8a8039bc2f15842e6c6e440d5434a5b
    private void placeOptimizedMove(OptimizedMoveEngine.BestMove bestMove) {
        // Capture l'état du plateau AVANT le placement
        BoardState boardStateBefore = buildBoardState();
        int row = bestMove.startRow;
        int col = bestMove.startCol;
        boolean horizontal = bestMove.horizontal;

        // Placement automatique sur le plateau
        for (int i = 0; i < bestMove.word.length(); i++) {
        	char c = Character.toUpperCase(bestMove.word.charAt(i));
            // Si la case était déjà remplie dans boardStateBefore, on passe à la cellule suivante
            if (boardStateBefore.safeHasLetter(row, col)) {
                if (horizontal)
                    col++;
                else
                    row++;
                continue;
            }
            // Ajout de la lettre sur le plateau
            for (Node node : grid.getChildren()) {
                Integer r = GridPane.getRowIndex(node);
                Integer cCol = GridPane.getColumnIndex(node);
                if (r != null && cCol != null && r == row && cCol == col && node instanceof StackPane) {
                    Label letterLabel = createDraggableLetter(String.valueOf(c));
                    ((StackPane) node).getChildren().add(letterLabel);
                    letterLabel.setOnDragDetected(null);
                    validatedLetters.add(letterLabel);
                    break;
                }
            }
            if (horizontal)
                col++;
            else
                row++;
        }

        // Utilise l'état du plateau AVANT le placement pour retirer les lettres du chevalet
        removeOptimizedMoveLettersFromRack(bestMove, boardStateBefore);
        refillRack(playerRacks.get(currentPlayerIndex));

        // Mise à jour du score et passage au joueur suivant
        scoreManagers.get(currentPlayerIndex).addPoints(bestMove.score);
        scorePanes.get(currentPlayerIndex).refreshScore();
        refillRack(playerRacks.get(currentPlayerIndex));
        switchToNextPlayer();
    }

    // Retire les lettres utilisées pour le coup optimisé en se basant sur l'état du plateau AVANT placement
    private void removeOptimizedMoveLettersFromRack(OptimizedMoveEngine.BestMove move, BoardState boardStateBefore) {
        HBox rack = playerRacks.get(currentPlayerIndex);

        List<Label> rackLetters = new ArrayList<>();
        // Récupère toutes les étiquettes présentes dans chaque StackPane du chevalet.
        for (Node node : rack.getChildren()) {
            if (node instanceof StackPane) {
                for (Node child : ((StackPane) node).getChildren()) {
                    if (child instanceof Label) {
                        rackLetters.add((Label) child);
                    }
                }
            }
        }

        int row = move.startRow;
        int col = move.startCol;

        // Pour chaque lettre du mot optimisé, si la case était vide avant placement,
        // cela signifie qu'elle a été jouée à partir du chevalet.
        for (int i = 0; i < move.word.length(); i++) {
            char letter = Character.toUpperCase(move.word.charAt(i));

            if (!boardStateBefore.safeHasLetter(row, col)) {
                boolean removed = false;
                // Cherche la lettre correspondante dans le chevalet.
                for (Label rackLabel : rackLetters) {
                    if (rackLabel.getText().equalsIgnoreCase(String.valueOf(letter))) {
                        // On vérifie que le label a bien un parent avant de le retirer.
                        if (rackLabel.getParent() != null && rackLabel.getParent() instanceof StackPane) {
                            ((StackPane) rackLabel.getParent()).getChildren().remove(rackLabel);
                            removed = true;
                            break;
                        }
                    }
                }
                // Si la lettre n'a pas été trouvée, on tente de retirer un joker.
                if (!removed) {
                    for (Label rackLabel : rackLetters) {
                        if (rackLabel.getText().equals("*")) {
                            if (rackLabel.getParent() != null && rackLabel.getParent() instanceof StackPane) {
                                ((StackPane) rackLabel.getParent()).getChildren().remove(rackLabel);
                                break;
                            }
                        }
                    }
                }
            }
            if (move.horizontal)
                col++;
            else
                row++;
        }
    }

}
