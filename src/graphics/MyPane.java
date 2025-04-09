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

    // Un rack par joueur
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

    // Nouvelle variable pour le bouton optimisé (déclarée ici pour référence si nécessaire)
    private Button optimizedButton;

    // Indique si c'est la première pose
    private boolean firstWordPlaced = false;

    // Lettres posées ce tour-ci
    private Map<Label, StackPane> placedLettersMap = new HashMap<>();
    // Lettres validées (définitivement posées sur le plateau)
    private List<Label> validatedLetters = new ArrayList<>();

    // Mots validés enregistrés
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

                // Gestion du drag & drop : on autorise le déplacement
                cellPane.setOnDragOver(event -> {
                    if (event.getGestureSource() instanceof Label && event.getDragboard().hasString()) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                    event.consume();
                });

                // Modification de l'événement de drop pour gérer le joker
                cellPane.setOnDragDropped(event -> {
                    Dragboard db = event.getDragboard();
                    if (db.hasString()) {
                        String letter = db.getString();
                        Label letterLabel = createDraggableLetter(letter);

                        // Si le jeton est un joker (affiché par un astérisque), on demande à l'utilisateur de choisir la lettre
                        if ("*".equals(letter)) {
                            String chosen = promptForJokerLetter();
                            if (!"*".equals(chosen)) {
                                letterLabel.setText(chosen);
                                // Marquer le label comme provenant d'un joker
                                letterLabel.setUserData("joker");
                            }
                        }

                        if (letterLabel == null || cellPane == null) {
                            event.setDropCompleted(false);
                            return;
                        }

                        // Ajouter la nouvelle lettre sur la case
                        cellPane.getChildren().add(letterLabel);

                        Label draggedLabel = (Label) event.getGestureSource();
                        if (draggedLabel == null) {
                            event.setDropCompleted(false);
                            return;
                        }

                        // Retirer l'ancienne lettre de son parent
                        StackPane previousTile = (StackPane) draggedLabel.getParent();
                        if (previousTile != null) {
                            previousTile.getChildren().remove(draggedLabel);
                        }

                        // Mettre à jour la position d'origine des lettres posées ce tour-ci
                        StackPane originalTile = placedLettersMap.getOrDefault(draggedLabel, previousTile);
                        placedLettersMap.remove(draggedLabel);
                        placedLettersMap.put(letterLabel, originalTile);

                        // Permettre à la lettre d'être redéplacée
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

        // ScoreManagers et ScorePanes (un par joueur)
        for (int i = 0; i < numberOfPlayers; i++) {
            ScoreManager sm = new ScoreManager();
            scoreManagers.add(sm);
            ScorePane sp = new ScorePane(sm);
            scorePanes.add(sp);
        }

        // Création d'un chevalet pour chaque joueur
        for (int i = 0; i < numberOfPlayers; i++) {
            HBox rack = new HBox(5);
            rack.setVisible(false); // Seul le chevalet du joueur courant est visible
            initializeChevalet(rack);
            playerRacks.add(rack);
            this.getChildren().add(rack);
        }

        // Afficher le chevalet du joueur 0
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

        // Bouton "Optimisé" ajouté pour lancer l'algorithme d'optimisation
        optimizedButton = new Button("Optimisé");
        optimizedButton.setOnAction(e -> {
            // Récupérer les lettres du chevalet du joueur courant
            List<Character> rackLetters = getRackLettersForCurrentPlayer();

            // Construire l'état du plateau
            BoardState boardState = buildBoardState();

            // Appeler l'algorithme d'optimisation
            OptimizedMoveEngine.BestMove bestMove = OptimizedMoveEngine.findBestMove(rackLetters, boardState, dictionary);

            if (bestMove != null) {
                // Demander confirmation au joueur via une boîte de dialogue
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Coup optimisé");
                alert.setHeaderText("Mot proposé : " + bestMove.word);
                alert.setContentText("Ce mot rapporte " + bestMove.score +
                        " points.\nVoulez-vous le jouer ?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // Placer automatiquement le mot sur le plateau
                    placeOptimizedMove(bestMove);
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Aucune solution");
                alert.setHeaderText(null);
                alert.setContentText("Aucune solution optimisée n'a été trouvée.");
                alert.showAndWait();
            }
        });

        // Positionnement des composants sur l'interface
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

        // Position du bouton Optimisé
        optimizedButton.setLayoutX(600);
        optimizedButton.setLayoutY(NUM_ROWS * CELL_SIZE + 120 + (numberOfPlayers - 1) * 60);

        this.getChildren().addAll(grid, resetButton, validateButton, nextPlayerButton, optimizedButton);

        // Chargement du fichier CSS pour le style (si présent)
        try {
            this.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {
            // style.css non trouvé
        }
    }

    /**
     * Renvoie la liste de tous les ScorePane (un par joueur).
     */
    public List<ScorePane> getAllScorePanes() {
        return scorePanes;
    }

    /**
     * Permet de passer au joueur suivant et met à jour l'interface.
     */
    private void switchToNextPlayer() {
        validateButton.setDisable(true);
        playerRacks.get(currentPlayerIndex).setVisible(false);
        currentPlayerIndex = (currentPlayerIndex + 1) % numberOfPlayers;
        playerRacks.get(currentPlayerIndex).setVisible(true);
        if (mainApp != null) {
            mainApp.updateWindowTitle(currentPlayerIndex + 1);
        }
    }

    /**
     * Vérifie les règles de placement et active/désactive le bouton de validation.
     */
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

    /**
     * Vérifie qu'une ligne ou colonne est continue (toutes les cases entre min et max sont occupées).
     */
    private boolean isLineContinuous(boolean horizontal, int fixed, int min, int max) {
        for (int i = min; i <= max; i++) {
            String letter = horizontal ? getLetterAt(fixed, i) : getLetterAt(i, fixed);
            if (letter == null) return false;
        }
        return true;
    }

    /**
     * Vérifie qu'au moins une des lettres posées est adjacente à une lettre déjà validée.
     */
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

    /**
     * Vérifie si une lettre validée est présente à la position (row, col).
     */
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

    /**
     * Valide le coup en vérifiant l'ensemble des mots formés (mot principal et mots croisés),
     * en tenant compte des multiplicateurs appliqués aux lettres nouvellement posées.
     * Ce code intègre la gestion du joker.
     */
    private void validateBoard() {
        List<String> formedWords = new ArrayList<>();
        List<Integer> wordsScores = new ArrayList<>();

        if (placedLettersMap.isEmpty()) {
            validateButton.setDisable(true);
            return;
        }

        if (placedLettersMap.size() > 1) {
            Set<Integer> rows = new HashSet<>();
            Set<Integer> cols = new HashSet<>();
            for (Label label : placedLettersMap.keySet()) {
                StackPane parent = (StackPane) label.getParent();
                Integer r = GridPane.getRowIndex(parent);
                Integer c = GridPane.getColumnIndex(parent);
                if (r != null) rows.add(r);
                if (c != null) cols.add(c);
            }
            if (rows.size() == 1) {
                int fixedRow = rows.iterator().next();
                int minCol = Integer.MAX_VALUE;
                for (Label label : placedLettersMap.keySet()) {
                    StackPane parent = (StackPane) label.getParent();
                    int col = GridPane.getColumnIndex(parent);
                    minCol = Math.min(minCol, col);
                }
                int[] horizRange = getHorizontalRange(fixedRow, minCol);
                String mainWord = getWordString(true, fixedRow, horizRange[0], horizRange[1]);
                formedWords.add(mainWord);
                int scoreMain = computeWordScore(true, fixedRow, horizRange[0], horizRange[1]);
                wordsScores.add(scoreMain);
                // Vérifier les mots croisés verticaux pour chaque lettre posée
                for (Label label : placedLettersMap.keySet()) {
                    StackPane parent = (StackPane) label.getParent();
                    int tileRow = GridPane.getRowIndex(parent);
                    int tileCol = GridPane.getColumnIndex(parent);
                    int[] vertRange = getVerticalRange(tileRow, tileCol);
                    if (vertRange[1] - vertRange[0] >= 1) {
                        String crossWord = getWordString(false, tileCol, vertRange[0], vertRange[1]);
                        if (crossWord.length() > 1) {
                            formedWords.add(crossWord);
                            int scoreCross = computeWordScore(false, tileCol, vertRange[0], vertRange[1]);
                            wordsScores.add(scoreCross);
                        }
                    }
                }
            } else if (cols.size() == 1) {
                int fixedCol = cols.iterator().next();
                int minRow = Integer.MAX_VALUE;
                for (Label label : placedLettersMap.keySet()) {
                    StackPane parent = (StackPane) label.getParent();
                    int row = GridPane.getRowIndex(parent);
                    minRow = Math.min(minRow, row);
                }
                int[] vertRange = getVerticalRange(minRow, fixedCol);
                String mainWord = getWordString(false, fixedCol, vertRange[0], vertRange[1]);
                formedWords.add(mainWord);
                int scoreMain = computeWordScore(false, fixedCol, vertRange[0], vertRange[1]);
                wordsScores.add(scoreMain);
                // Vérifier les mots croisés horizontaux pour chaque lettre posée
                for (Label label : placedLettersMap.keySet()) {
                    StackPane parent = (StackPane) label.getParent();
                    int tileRow = GridPane.getRowIndex(parent);
                    int tileCol = GridPane.getColumnIndex(parent);
                    int[] horizRange = getHorizontalRange(tileRow, tileCol);
                    if (horizRange[1] - horizRange[0] >= 1) {
                        String crossWord = getWordString(true, tileRow, horizRange[0], horizRange[1]);
                        if (crossWord.length() > 1) {
                            formedWords.add(crossWord);
                            int scoreCross = computeWordScore(true, tileRow, horizRange[0], horizRange[1]);
                            wordsScores.add(scoreCross);
                        }
                    }
                }
            }
        } else { // Cas d'une seule lettre posée : vérifier dans les deux directions
            Label singleLabel = placedLettersMap.keySet().iterator().next();
            StackPane parent = (StackPane) singleLabel.getParent();
            int row = GridPane.getRowIndex(parent);
            int col = GridPane.getColumnIndex(parent);

            int[] horizRange = getHorizontalRange(row, col);
            String horizWord = getWordString(true, row, horizRange[0], horizRange[1]);
            if (horizWord.length() > 1) {
                formedWords.add(horizWord);
                int scoreHoriz = computeWordScore(true, row, horizRange[0], horizRange[1]);
                wordsScores.add(scoreHoriz);
            }
            int[] vertRange = getVerticalRange(row, col);
            String vertWord = getWordString(false, col, vertRange[0], vertRange[1]);
            if (vertWord.length() > 1) {
                formedWords.add(vertWord);
                int scoreVert = computeWordScore(false, col, vertRange[0], vertRange[1]);
                wordsScores.add(scoreVert);
            }
        }

        // Vérifier que tous les mots formés sont valides (selon le dictionnaire)
        for (String word : formedWords) {
            if (!dictionary.validWord(word)) {
                showError("Le mot '" + word + "' n'est pas dans le dictionnaire !");
                return;
            }
        }

        // Calcul du score total pour le coup
        int totalPoints = 0;
        for (int pts : wordsScores) {
            totalPoints += pts;
        }
        scoreManagers.get(currentPlayerIndex).addPoints(totalPoints);
        scorePanes.get(currentPlayerIndex).refreshScore();

        // Afficher une notification des mots validés et du score obtenu
        StringBuilder msg = new StringBuilder("Bravo !\nLes mots validés :\n");
        for (int i = 0; i < formedWords.size(); i++) {
            msg.append(formedWords.get(i)).append(" (").append(wordsScores.get(i)).append(" points)\n");
        }
        msg.append("Total : ").append(totalPoints).append(" points.");
        showSuccess(msg.toString());

        // Marquer les lettres posées comme définitivement validées
        for (Label letterLabel : placedLettersMap.keySet()) {
            letterLabel.setOnDragDetected(null);
            validatedLetters.add(letterLabel);
        }
        placedLettersMap.clear();
        firstWordPlaced = true;
        validateButton.setDisable(true);

        // Reconstituer le chevalet pour le joueur courant
        refillRack(playerRacks.get(currentPlayerIndex));
    }

    /**
     * Retourne la lettre présente dans la cellule (row, col) du plateau.
     */
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

    /**
     * Annule le placement du coup en cours et remet les lettres sur le chevalet d'origine.
     */
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

    /**
     * Initialise un chevalet (7 lettres piochées) pour un joueur.
     */
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

    /**
     * Reconstitue le chevalet en piochant dans le sac pour remplir les cases vides.
     */
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

    /**
     * Retourne le multiplicateur de lettre pour la case (row, col).
     */
    private int getLetterMultiplier(int row, int col) {
        Color color = getCellColor(row, col);
        if (color.equals(Color.DARKBLUE)) {
            return 3;
        } else if (color.equals(Color.LIGHTBLUE)) {
            return 2;
        }
        return 1;
    }

    /**
     * Retourne le multiplicateur de mot pour la case (row, col).
     */
    private int getWordMultiplier(int row, int col) {
        Color color = getCellColor(row, col);
        if (color.equals(Color.RED)) {
            return 3;
        } else if (color.equals(Color.PINK)) {
            return 2;
        }
        return 1;
    }

    /**
     * Crée un label draggable pour une lettre donnée.
     */
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

    /**
     * Affiche une fenêtre d'erreur.
     */
    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Erreur de validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Affiche une fenêtre d'information.
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Mot validé !");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Retourne la couleur d'arrière-plan de la case (row, col) du plateau.
     */
    private Color getCellColor(int row, int col) {
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

    /**
     * Ouvre une fenêtre de saisie pour demander au joueur la lettre qu'il souhaite attribuer au joker.
     */
    private String promptForJokerLetter() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Choix du Joker");
        dialog.setHeaderText("Lettre du Joker");
        dialog.setContentText("Entrez la lettre que vous souhaitez attribuer au joker :");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().isEmpty()) {
            // On récupère le premier caractère et on le met en majuscule
            return result.get().substring(0, 1).toUpperCase();
        }
        // Si l'utilisateur annule ou ne saisit rien, on conserve l'astérisque
        return "*";
    }

    /**
     * Vérifie si la case (row, col) contient une lettre issue d'un joker.
     */
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

    /**
     * Pour une recherche horizontale, renvoie [colStart, colEnd] de l'étendue du mot sur la ligne.
     */
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

    /**
     * Pour une recherche verticale, renvoie [rowStart, rowEnd] de l'étendue du mot sur la colonne.
     */
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

    /**
     * Construit une chaîne représentant le mot le long d'une ligne (ou colonne).
     *
     * @param horizontal Si vrai, le mot est horizontal (ligne fixe), sinon vertical (colonne fixe).
     * @param fixed      La ligne (ou colonne) fixe.
     * @param start      Le début (colonne ou ligne).
     * @param end        La fin (colonne ou ligne).
     * @return Le mot formé.
     */
    private String getWordString(boolean horizontal, int fixed, int start, int end) {
        StringBuilder word = new StringBuilder();
        for (int pos = start; pos <= end; pos++) {
            String letter;
            if (horizontal) {
                letter = getLetterAt(fixed, pos);
            } else {
                letter = getLetterAt(pos, fixed);
            }
            if (letter != null) {
                word.append(letter);
            }
        }
        return word.toString();
    }

    /**
     * Calcule le score d'un mot situé sur une ligne ou une colonne.
     * Seules les lettres nouvellement posées bénéficient des bonus. Pour un joker, la valeur sera forcée à 0.
     *
     * @param horizontal Si vrai, le mot est horizontal ; sinon vertical.
     * @param fixed      La ligne (ou colonne) fixe.
     * @param start      Début (colonne ou ligne).
     * @param end        Fin (colonne ou ligne).
     * @return Le score calculé pour le mot.
     */
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
            // Si la lettre est une nouvelle pose et provient d'un joker, on fixe sa valeur à 0
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

    /**
     * Vérifie si, à la position (row, col), la lettre a été posée ce tour-ci.
     */
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

    /////////////// MÉTHODES AJOUTÉES POUR LE MODE "OPTIMISÉ" ///////////////

    /**
     * Extrait les lettres du chevalet du joueur courant.
     */
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

    /**
     * Construit un BoardState à partir de l'état actuel du plateau.
     */
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


    /**
     * Place automatiquement le coup optimisé sur le plateau.
     *
     * @param bestMove Le meilleur mouvement trouvé par l'algorithme d'optimisation.
     */
    private void placeOptimizedMove(OptimizedMoveEngine.BestMove bestMove) {
        int row = bestMove.startRow;
        int col = bestMove.startCol;
        boolean horizontal = bestMove.horizontal;

        // Pour chaque caractère du mot, placer la lettre dans la case correspondante
        for (int i = 0; i < bestMove.word.length(); i++) {
            char c = bestMove.word.charAt(i);
            // Parcourir le grid pour trouver le StackPane correspondant à la case (row, col)
            for (Node node : grid.getChildren()) {
                Integer r = GridPane.getRowIndex(node);
                Integer cCol = GridPane.getColumnIndex(node);
                if (r != null && cCol != null && r == row && cCol == col && node instanceof StackPane) {
                    Label letterLabel = createDraggableLetter(String.valueOf(c));
                    ((StackPane) node).getChildren().add(letterLabel);
                    // Marquer la lettre comme définitivement validée
                    letterLabel.setOnDragDetected(null);
                    validatedLetters.add(letterLabel);
                    break;
                }
            }
            if (horizontal) {
                col++;
            } else {
                row++;
            }
        }
        // Mettre à jour le score du joueur courant
        scoreManagers.get(currentPlayerIndex).addPoints(bestMove.score);
        scorePanes.get(currentPlayerIndex).refreshScore();

        // Passer au joueur suivant
        switchToNextPlayer();
    }
    ////////////// FIN DES MÉTHODES AJOUTÉES ///////////////


    ////////////// FIN DES MÉTHODES EXISTANTES ///////////////

}