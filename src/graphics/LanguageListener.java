package graphics;

import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

public class LanguageListener implements EventHandler<MouseEvent> {
	
	private String bgColorClicked = "-fx-background-color: lightgrey";
	
	public LanguageListener() {
		
	}

	@Override
	public void handle(MouseEvent event) {
		// TODO Auto-generated method stub
        if (event.getSource() instanceof Label) {
            Label label = (Label) event.getSource();
            
            // Change la couleur de fond du label
            label.setStyle(bgColorClicked);
            System.out.println("yo");
        }
		
	}

}
