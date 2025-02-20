package graphics;

import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.*;
import javafx.scene.input.*;
import game.Letter;
import dictionary.Dictionary;
import java.io.IOException;
import java.util.*;

public class MyPane extends Pane {

    private static final int NUM_ROWS = 15;
    private static final int NUM_COLS = 15;
    private static final double CELL_SIZE = 40;
    private static final int CHEVALET_SIZE = 7;
    
    private static final int CENTER_ROW = 7;
    private static final int CENTER_COL = 7;
    
    private boolean firstWordPlaced = false;

    private Map<Label, StackPane> placedLettersMap = new HashMap<>();
    private List<Label> validatedLetters = new ArrayList<>();
    private HBox chevalet = new HBox(5);
    private GridPane grid = new GridPane();
    @SuppressWarnings("unused")
	private Letter letterBag = Letter.getInstance();
    private Button validateButton;
    private Dictionary dictionary;

    public MyPane() {
        super();
        try {
            dictionary = new Dictionary("french"); // Chargement du dictionnaire français
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du dictionnaire.");
            return;
        }

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

                        // Vérification de validité
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

                        // Récupérer l'ancienne case où était la lettre
                        StackPane previousTile = (StackPane) draggedLabel.getParent();
                        if (previousTile != null) {
                            previousTile.getChildren().remove(draggedLabel); // Supprime bien l’ancienne lettre
                        }

                        // Mettre à jour la position d’origine
                        StackPane originalTile = placedLettersMap.getOrDefault(draggedLabel, previousTile);
                        placedLettersMap.remove(draggedLabel); // Supprime l’ancienne référence
                        placedLettersMap.put(letterLabel, originalTile); // Stocke la nouvelle référence

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

        chevalet.setTranslateY(NUM_ROWS * CELL_SIZE + 60);
        chevalet.setTranslateX(155);
        initializeChevalet();

        Button resetButton = new Button("Annuler");
        resetButton.getStyleClass().add("annuler-btn");
        resetButton.setTranslateY(NUM_ROWS * CELL_SIZE + 120);
        resetButton.setTranslateX(180);
        resetButton.setOnAction(e -> resetBoard());

        validateButton = new Button("Valider");
        validateButton.getStyleClass().add("valider-btn");
        validateButton.setTranslateY(NUM_ROWS * CELL_SIZE + 120);
        validateButton.setTranslateX(320);
        validateButton.setOnAction(e -> validateBoard());
        validateButton.setDisable(true);

        this.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        this.getChildren().addAll(grid, chevalet, resetButton, validateButton);
    }
    
    /**
     * Vérifie si les lettres sont alignées, collées et passent par la case centrale si nécessaire.
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

        boolean alignedHorizontally = new HashSet<>(rows).size() == 1;
        boolean alignedVertically = new HashSet<>(cols).size() == 1;

        // Vérifie que les lettres sont bien collées sans espace
        boolean isContinuous = isPlacementContinuous(rows, cols, alignedHorizontally, alignedVertically);

        if (!firstWordPlaced && !containsCenter) {
            validateButton.setDisable(true);
            return;
        }

        if (!(alignedHorizontally || alignedVertically) || !isContinuous) {
            validateButton.setDisable(true);
        } else {
            validateButton.setDisable(false);
        }
    }

    /**
     * Vérifie que les lettres placées sont collées sans espace.
     */
    private boolean isPlacementContinuous(List<Integer> rows, List<Integer> cols, boolean alignedHorizontally, boolean alignedVertically) {
        if (alignedHorizontally) {
            Collections.sort(cols);
            for (int i = 0; i < cols.size() - 1; i++) {
                if (cols.get(i) + 1 != cols.get(i + 1)) {
                    return false; // Il y a un trou entre les lettres
                }
            }
        } else if (alignedVertically) {
            Collections.sort(rows);
            for (int i = 0; i < rows.size() - 1; i++) {
                if (rows.get(i) + 1 != rows.get(i + 1)) {
                    return false; // Il y a un trou entre les lettres
                }
            }
        }
        return true;
    }

    /**
     * Vérifie si les lettres sont bien placées et si le mot existe dans le dictionnaire.
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

        // 🔥 Le mot est valide, afficher un message de confirmation
        showSuccess("Bravo ! Le mot '" + word + "' est valide et a été ajouté au plateau.");

        for (Label letterLabel : placedLettersMap.keySet()) {
            letterLabel.setOnDragDetected(null); // Désactiver le déplacement
            validatedLetters.add(letterLabel);
        }
        placedLettersMap.clear();
        firstWordPlaced = true;
        validateButton.setDisable(true);
    }
    
    /**
     * Affiche une boîte de dialogue de succès.
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mot validé !");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



    /**
     * Récupère le mot formé par les lettres placées.
     */
    private String getPlacedWord() {
        List<Integer> rows = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        TreeMap<Integer, String> lettersByRow = new TreeMap<>();
        TreeMap<Integer, String> lettersByCol = new TreeMap<>();

        for (Label label : placedLettersMap.keySet()) {
            StackPane parent = (StackPane) label.getParent();
            Integer row = GridPane.getRowIndex(parent);
            Integer col = GridPane.getColumnIndex(parent);
            if (row == null || col == null) continue;

            rows.add(row);
            cols.add(col);

            if (!lettersByRow.containsKey(col)) {
                lettersByRow.put(col, label.getText());
            } else {
                lettersByRow.put(col, lettersByRow.get(col) + label.getText());
            }

            if (!lettersByCol.containsKey(row)) {
                lettersByCol.put(row, label.getText());
            } else {
                lettersByCol.put(row, lettersByCol.get(row) + label.getText());
            }
        }

        boolean alignedHorizontally = new HashSet<>(rows).size() == 1;
        boolean alignedVertically = new HashSet<>(cols).size() == 1;

        if (alignedHorizontally) {
            return String.join("", lettersByRow.values());
        } else if (alignedVertically) {
            return String.join("", lettersByCol.values());
        }
        return null;
    }

    /**
     * Affiche une alerte en cas d'erreur.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void resetBoard() {
        List<Label> lettersToRemove = new ArrayList<>(placedLettersMap.keySet());

        for (Label letterLabel : lettersToRemove) {
            if (letterLabel == null || letterLabel.getParent() == null) {
                continue; // Évite les erreurs si la lettre est null
            }

            StackPane originalTile = placedLettersMap.get(letterLabel);

            if (originalTile == null) {
                continue; // Empêche toute tentative de remise d'un `null`
            }

            // Vérifier si la lettre est encore présente sur le plateau avant de la supprimer
            StackPane currentTile = (StackPane) letterLabel.getParent();
            if (currentTile != null) {
                currentTile.getChildren().remove(letterLabel);
            }

            // Ajouter la lettre à sa position d'origine seulement si elle a un texte valide
            if (letterLabel.getText() != null) {
                Label restoredLetter = createDraggableLetter(letterLabel.getText());
                originalTile.getChildren().add(restoredLetter);
            }
        }

        placedLettersMap.clear();
        validateButton.setDisable(true);
    }


    private void initializeChevalet() {
        chevalet.getChildren().clear();
        for (int i = 0; i < CHEVALET_SIZE; i++) {
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

            chevalet.getChildren().add(tilePane);
        }
    }

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
