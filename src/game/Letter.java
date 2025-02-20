package game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Letter {

	private static Letter instance;
	private static final Object lock = new Object(); // verrou pour les threads
	public static HashMap<Character, Integer> defaultLetters = new HashMap<>();
	public static HashMap<Character, Integer> pointsLetter = new HashMap<>();
	public static HashMap<Character, Integer> bagLetters = new HashMap<>();

	private Letter(){
		// Initialisation des lettres et des points
		// Distribution de lettres selon le Scrabble français officiel (102 lettres)
		initializeLetters();
	}

	public static Letter getInstance() {
		if(instance == null) {
			synchronized (lock) { // Empêche plusieurs threads de créer une instance simultanément
				if (instance == null) { // Vérification finale
					instance = new Letter();
				}
			}
			return instance;
		} else {
			return instance;
		}
	}

	private void initializeLetters() {
		// Voyelles
		defaultLetters.put('A', 9);  pointsLetter.put('A', 1);  bagLetters.put('A', 9);
		defaultLetters.put('E', 15); pointsLetter.put('E', 1);  bagLetters.put('E', 15);
		defaultLetters.put('I', 8);  pointsLetter.put('I', 1);  bagLetters.put('I', 8);
		defaultLetters.put('O', 6);  pointsLetter.put('O', 1);  bagLetters.put('O', 6);
		defaultLetters.put('U', 6);  pointsLetter.put('U', 1);  bagLetters.put('U', 6);
		defaultLetters.put('Y', 1);  pointsLetter.put('Y', 10); bagLetters.put('Y', 1);

		// Consonnes courantes
		defaultLetters.put('B', 2);  pointsLetter.put('B', 3);  bagLetters.put('B', 2);
		defaultLetters.put('C', 2);  pointsLetter.put('C', 3);  bagLetters.put('C', 2);
		defaultLetters.put('D', 3);  pointsLetter.put('D', 2);  bagLetters.put('D', 3);
		defaultLetters.put('F', 2);  pointsLetter.put('F', 4);  bagLetters.put('F', 2);
		defaultLetters.put('G', 2);  pointsLetter.put('G', 2);  bagLetters.put('G', 2);
		defaultLetters.put('H', 2);  pointsLetter.put('H', 4);  bagLetters.put('H', 2);
		defaultLetters.put('J', 1);  pointsLetter.put('J', 8);  bagLetters.put('J', 1);
		defaultLetters.put('K', 1);  pointsLetter.put('K', 10); bagLetters.put('K', 1);
		defaultLetters.put('L', 5);  pointsLetter.put('L', 1);  bagLetters.put('L', 5);
		defaultLetters.put('M', 3);  pointsLetter.put('M', 2);  bagLetters.put('M', 3);
		defaultLetters.put('N', 6);  pointsLetter.put('N', 1);  bagLetters.put('N', 6);
		defaultLetters.put('P', 2);  pointsLetter.put('P', 3);  bagLetters.put('P', 2);
		defaultLetters.put('Q', 1);  pointsLetter.put('Q', 8);  bagLetters.put('Q', 1);
		defaultLetters.put('R', 6);  pointsLetter.put('R', 1);  bagLetters.put('R', 6);
		defaultLetters.put('S', 6);  pointsLetter.put('S', 1);  bagLetters.put('S', 6);
		defaultLetters.put('T', 6);  pointsLetter.put('T', 1);  bagLetters.put('T', 6);
		defaultLetters.put('V', 2);  pointsLetter.put('V', 4);  bagLetters.put('V', 2);
		defaultLetters.put('W', 1);  pointsLetter.put('W', 10); bagLetters.put('W', 1);
		defaultLetters.put('X', 1);  pointsLetter.put('X', 10); bagLetters.put('X', 1);
		defaultLetters.put('Z', 1);  pointsLetter.put('Z', 10); bagLetters.put('Z', 1);

		// Joker (case blanche)
		defaultLetters.put('*', 2);  pointsLetter.put('*', 0);  bagLetters.put('*', 2);
	}

	public static void removeLetter(Character c) {
		int amount = bagLetters.get(c);
		if (amount > 0) {
			bagLetters.put(c, amount-1);
		} else {
			System.out.println("Erreur removeLetter : La lettre " + c + " n'est plus disponible dans le sac");
		}
	}

	public static void addLetter(Character c) {
		int amount = bagLetters.get(c);
		int maxNbLetter = defaultLetters.get(c);
		if (amount < maxNbLetter) {
			bagLetters.put(c, amount+1);
		} else {
			System.out.println("Erreur addLetter : La lettre " + c + " est déjà en trop gros nombre dans le sac");
		}
	}

	public static Character drawLetter() {
		if (bagLetters.isEmpty()) {
			System.out.println("Erreur drawLetter : Le sac est vide");
			return null;
		}

		// Créer une liste pondérée en fonction des quantités de chaque lettre
		List<Character> letterPool = new ArrayList<>();
		for (Map.Entry<Character, Integer> entry : bagLetters.entrySet()) {
			char letter = entry.getKey();
			int count = entry.getValue();
			// Ajouter la lettre dans la liste autant de fois qu'elle est présente dans le sac
			for (int i = 0; i < count; i++) {
				letterPool.add(letter);
			}
		}

		if (letterPool.isEmpty()) {
			System.out.println("Erreur drawLetter : Plus de lettres disponibles");
			return null;
		}

		// Tirage au sort d'une lettre
		Random rand = new Random();
		Character drawnLetter = letterPool.get(rand.nextInt(letterPool.size()));

		// Retirer la lettre du sac
		removeLetter(drawnLetter);

		return drawnLetter;
	}
}