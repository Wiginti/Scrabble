package graphics;

import java.util.List;

import game.Language;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class LanguagePane extends VBox {

	public LanguagePane() {
		super();
		setAllLabel(Language.scrabbleLanguages);
		super.setPadding(new Insets(5, 0, 0, 0));
		super.setAlignment(Pos.TOP_CENTER);
		super.setPrefHeight(200);
	}
	
	public void setLabel(Label myLabel) {
		myLabel.addEventFilter(MouseEvent.MOUSE_PRESSED, new LanguageListener());
		myLabel.setMaxWidth(Double.MAX_VALUE);
		myLabel.setAlignment(Pos.CENTER);
	}
	
	public void setAllLabel(List<String> langue) {
        for (String language : langue) {
            Label label = new Label(language);
            setLabel(label);
            super.getChildren().add(label);
        }
	}
	
}
