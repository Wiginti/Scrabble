package graphics;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class MyPane extends Pane {

	private static final int NUM_ROWS = 15;
	private static final int NUM_COLS = 15;
	private static final double CELL_SIZE = 40;

	public MyPane() {
		super();
		GridPane grid = new GridPane();
		grid.setHgap(1); // Espacement horizontal entre les cases
		grid.setVgap(1); // Espacement vertical entre les cases

		// Création de la grille de 15 x 15
		for (int row = 0; row < NUM_ROWS; row++) {
			for (int col = 0; col < NUM_COLS; col++) {
				// Chaque case est un StackPane, afin de pouvoir y superposer une lettre (Label)
				StackPane cellPane = new StackPane();
				cellPane.setPrefSize(CELL_SIZE, CELL_SIZE);

				Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
				cell.setStroke(Color.BLACK);
				cell.setFill(getCellColor(row, col));

				cellPane.getChildren().add(cell);

				// Gestionnaire d'événements pour accepter le drag over
				cellPane.setOnDragOver(event -> {
					Dragboard db = event.getDragboard();
					if (event.getGestureSource() != cellPane && db.hasString()) {
						event.acceptTransferModes(TransferMode.MOVE);
					}
					event.consume();
				});

				// Gestionnaire d'événement pour le drop d'une tuile lettre
				cellPane.setOnDragDropped(event -> {
					Dragboard db = event.getDragboard();
					boolean success = false;
					if (db.hasString()) {
						String letter = db.getString();
						// Création d'un Label pour afficher la lettre déposée
						Label letterLabel = new Label(letter);
						letterLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
						// On ajoute le label sur le cellPane (au-dessus du Rectangle)
						cellPane.getChildren().add(letterLabel);
						success = true;
					}
					event.setDropCompleted(success);
					event.consume();
				});

				grid.add(cellPane, col, row);
			}
		}
		this.getChildren().add(grid);
	}

	/**
	 * Retourne la couleur de la case en fonction de sa position sur le plateau.
	 */
	private Color getCellColor(int row, int col) {
		// Triple mot (TWS) – cases rouges
		if ((row == 0 && (col == 0 || col == 7 || col == 14)) ||
				(row == 7 && (col == 0 || col == 14)) ||
				(row == 14 && (col == 0 || col == 7 || col == 14))) {
			return Color.RED;
		}
		// Double mot (DWS) – cases roses
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
		// Triple lettre (TLS) – cases bleu foncé
		if ((row == 1 && (col == 5 || col == 9)) ||
				(row == 5 && (col == 1 || col == 5 || col == 9 || col == 13)) ||
				(row == 9 && (col == 1 || col == 5 || col == 9 || col == 13)) ||
				(row == 13 && (col == 5 || col == 9))) {
			return Color.DARKBLUE;
		}
		// Double lettre (DLS) – cases bleu clair
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
		// Sinon, case normale
		return Color.BEIGE;
	}
}