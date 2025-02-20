package graphics;

import game.Letter;
import dictionary.Dictionary;
import game.ScoreManager;

import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.*;
import javafx.scene.input.*;

import javafx.scene.Node;
import java.io.IOException;
import java.util.*;

public class MyPane extends Pane {

    private static final int NUM_ROWS = 15;
    private static final int NUM_COLS = 15;
    private static final double CELL_SIZE = 40;

    private static final int CENTER_ROW = 7;
    private static final int CENTER_COL = 7;

    private Dictionary dictionary;

    // Le nombre de joueurs
    private int numberOfPlayers;

    // Un rack par joueur
    private List<HBox> playerRacks = new ArrayList<>();

    // Un ScoreManager par joueur
    private List<ScoreManager> scoreManagers = new ArrayList<>();
    // Un ScorePane par joueur
    private List<ScorePane> scorePanes = new ArrayList<>();

    // Indique quel joueur (index) est en train de jouer
    private int currentPlayerIndex = 0;

    // On garde une référence à l'appli principale pour MAJ du titre
    private BasicScene mainApp;

    // Composants du plateau
    private GridPane grid = new GridPane();
    private Button validateButton;
    private Button resetButton;
    private Button nextPlayerButton;

    // Première pose ?
    private boolean firstWordPlaced = false;

    // placedLettersMap : lettres posées ce tour-ci
    private Map<Label, StackPane> placedLettersMap = new HashMap<>();
    // validatedLetters : lettres validées et donc présentes au plateau
    private List<Label> validatedLetters = new ArrayList<>();

    // [NOUVEAU] On enregistre les mots validés
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

                cellPane.setOnDragDropped(event -> {
                    Dragboard db = event.getDragboard();
                    if (db.hasString()) {
                        String letter = db.getString();
                        Label letterLabel = createDraggableLetter(letter);

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

                        // Mettre à jour la position d’origine
                        StackPane originalTile = placedLettersMap.getOrDefault(draggedLabel, previousTile);
                        placedLettersMap.remove(draggedLabel);
                        placedLettersMap.put(letterLabel, originalTile);

                        // Permettre à la lettre d’être redéplacée
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

        // ScoreManagers + ScorePanes
        for (int i = 0; i < numberOfPlayers; i++) {
            ScoreManager sm = new ScoreManager();
            scoreManagers.add(sm);

            ScorePane sp = new ScorePane(sm);
            scorePanes.add(sp);
        }

        // Création d'un chevalet par joueur
        for (int i = 0; i < numberOfPlayers; i++) {
            HBox rack = new HBox(5);
            rack.setVisible(false); // On n'affiche que celui du joueur courant
            initializeChevalet(rack);
            playerRacks.add(rack);
            this.getChildren().add(rack);
        }

        // Affiche le chevalet du joueur 0
        playerRacks.get(0).setVisible(true);

        // Bouton Annuler
        resetButton = new Button("Annuler");
        resetButton.getStyleClass().add("annuler-btn");
        resetButton.setOnAction(e -> resetBoard());

        // Bouton Valider
        validateButton = new Button("Valider");
        validateButton.getStyleClass().add("valider-btn");
        validateButton.setOnAction(e -> validateBoard());
        validateButton.setDisable(true);

        // Bouton Joueur suivant
        nextPlayerButton = new Button("Joueur suivant");
        nextPlayerButton.setOnAction(e -> switchToNextPlayer());

        // Positionnement
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

        this.getChildren().addAll(grid, resetButton, validateButton, nextPlayerButton);

        // Chargement de style.css si présent
        try {
            this.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {
            // style.css non trouvé
        }
    }

    /**
     * Renvoie la liste de tous les ScorePane (un par joueur),
     * pour que BasicScene puisse les afficher à droite.
     */
    public List<ScorePane> getAllScorePanes() {
        return scorePanes;
    }

    /**
     * Passe la main au joueur suivant, met à jour l'interface,
     * et met à jour le titre de la fenêtre via mainApp.
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
     * Vérifie le placement en tenant compte de l'alignement, de la continuité
     * et de la connexion aux lettres déjà validées sur le plateau.
     * (Chaque mot ajouté doit être relié à un mot déjà présent.)
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
     * Vérifie que toutes les cases de la ligne (ou colonne) entre min et max sont occupées.
     */
    private boolean isLineContinuous(boolean horizontal, int fixed, int min, int max) {
        for (int i = min; i <= max; i++) {
            String letter = horizontal ? getLetterAt(fixed, i) : getLetterAt(i, fixed);
            if (letter == null) return false;
        }
        return true;
    }

    /**
     * Vérifie qu'au moins une des lettres posées est adjacente à une lettre validée déjà présente sur le plateau.
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
     * Calcule le score du coup en tenant compte des valeurs des lettres,
     * des multiplicateurs des cases sur lesquelles les lettres nouvellement posées sont placées,
     * et complète le chevalet du joueur.
     *
     * Seuls les multiplicateurs des cases des lettres posées ce tour-ci (placedLettersMap) sont pris en compte.
     */
    private void validateBoard() {
        String word = getPlacedWord();
        if (word == null || word.length() < 2) {
            showError("Mot invalide : il doit contenir au moins 2 lettres.");
            return;
        }
        if (!dictionary.validWord(word)) {
            showError("Le mot '" + word + "' n'est pas dans le dictionnaire !");
            return;
        }
        showSuccess("Bravo ! Le mot '" + word + "' est valide et a été ajouté au plateau.");
        validatedWords.add(word);

        // Calcul du score
        // La base correspond à la somme des valeurs de toutes les lettres du mot (sans bonus)
        int baseScore = 0;
        for (int i = 0; i < word.length(); i++) {
            char letter = word.charAt(i);
            Integer value = Letter.pointsLetter.get(Character.toUpperCase(letter));
            if (value != null) {
                baseScore += value;
            }
        }
        // Pour les lettres nouvellement posées, on ajoute un bonus
        int bonus = 0;
        int wordMultiplier = 1;
        for (Label letterLabel : placedLettersMap.keySet()) {
            String letterStr = letterLabel.getText();
            if (letterStr != null && !letterStr.isEmpty()) {
                char letter = letterStr.charAt(0);
                Integer value = Letter.pointsLetter.get(Character.toUpperCase(letter));
                StackPane parent = (StackPane) letterLabel.getParent();
                Integer row = GridPane.getRowIndex(parent);
                Integer col = GridPane.getColumnIndex(parent);
                if (row != null && col != null && value != null) {
                    int letterMult = getLetterMultiplier(row, col);
                    bonus += value * (letterMult - 1);
                    wordMultiplier *= getWordMultiplier(row, col);
                }
            }
        }
        int gainedPoints = (baseScore + bonus) * wordMultiplier;

        scoreManagers.get(currentPlayerIndex).addPoints(gainedPoints);
        scorePanes.get(currentPlayerIndex).refreshScore();

        // Les lettres posées deviennent définitivement validées
        for (Label letterLabel : placedLettersMap.keySet()) {
            letterLabel.setOnDragDetected(null);
            validatedLetters.add(letterLabel);
        }
        placedLettersMap.clear();
        firstWordPlaced = true;
        validateButton.setDisable(true);

        // Reconstituer le chevalet du joueur
        refillRack(playerRacks.get(currentPlayerIndex));
    }

    /**
     * Construit le mot à partir des lettres posées en étendant la recherche
     * aux cases adjacentes contenant déjà des lettres validées.
     */
    private String getPlacedWord() {
        List<Integer> rows = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        for (Label label : placedLettersMap.keySet()) {
            StackPane parent = (StackPane) label.getParent();
            Integer row = GridPane.getRowIndex(parent);
            Integer col = GridPane.getColumnIndex(parent);
            if (row != null && col != null) {
                rows.add(row);
                cols.add(col);
            }
        }
        boolean alignedHorizontally = (new HashSet<>(rows).size() == 1);
        boolean alignedVertically   = (new HashSet<>(cols).size() == 1);
        if (alignedHorizontally) {
            int row = rows.get(0);
            int minCol = Collections.min(cols);
            int maxCol = Collections.max(cols);
            int col = minCol;
            while (col > 0 && getLetterAt(row, col - 1) != null) { col--; }
            minCol = col;
            col = maxCol;
            while (col < NUM_COLS - 1 && getLetterAt(row, col + 1) != null) { col++; }
            maxCol = col;
            StringBuilder word = new StringBuilder();
            for (int c = minCol; c <= maxCol; c++) {
                String letter = getLetterAt(row, c);
                if (letter != null) {
                    word.append(letter);
                }
            }
            return word.toString();
        } else if (alignedVertically) {
            int col = cols.get(0);
            int minRow = Collections.min(rows);
            int maxRow = Collections.max(rows);
            int row = minRow;
            while (row > 0 && getLetterAt(row - 1, col) != null) { row--; }
            minRow = row;
            row = maxRow;
            while (row < NUM_ROWS - 1 && getLetterAt(row + 1, col) != null) { row++; }
            maxRow = row;
            StringBuilder word = new StringBuilder();
            for (int r = minRow; r <= maxRow; r++) {
                String letter = getLetterAt(r, col);
                if (letter != null) {
                    word.append(letter);
                }
            }
            return word.toString();
        }
        return null;
    }

    /**
     * Renvoie la lettre présente dans la cellule (row, col) du plateau.
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
     * Annule le placement de ce tour et remet les lettres sur le chevalet d'origine.
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
     * Initialise un chevalet (7 lettres piochées).
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
     * Reconstitue le chevalet en piochant dans le sac pour remplir les cases vides,
     * de sorte que le joueur ait toujours 7 lettres (si disponibles).
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
     * Retourne le multiplicateur de lettre pour la case (row, col)
     * en fonction de sa couleur.
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
     * Retourne le multiplicateur de mot pour la case (row, col)
     * en fonction de sa couleur.
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
        Label letterLabel = new Label(letter);
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
     * Affiche un message d'erreur.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Affiche un message d'information.
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mot validé !");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Retourne la couleur d'arrière-plan pour la case (row, col) du plateau.
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
}