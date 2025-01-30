package game;

import java.util.HashMap;

public class Letter {

	public static HashMap<Character, Integer> defaultLetters;
	public static HashMap<Character, Integer> pointsLetter;
	public static HashMap<Character, Integer> bagLetters;
	
	public void removeLetter(Character c) {
		int amount = bagLetters.get(c);
		if (amount > 0) {
			bagLetters.put(c, amount-1);
		} else {
			System.out.println("Erreur removeLetter : La lettre " + c + " n'est plus disponible dans le sac");
		}
		
	}
	
	public void addLetter(Character c) {
		int amount = bagLetters.get(c);
		int maxNbLetter = defaultLetters.get(c);
		if (amount < maxNbLetter) {
			bagLetters.put(c, amount+1);
		} else {
			System.out.println("Erreur addLetter : La lettre " + c + " est déjà en trop gros nombre dans le sac");
		}
	}
	
}
