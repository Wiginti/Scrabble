package game;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import dictionary.Dictionary;

public class Language {

	public static List<String> scrabbleLanguages = Arrays.asList("Afrikaans", "Allemand", "Anglais", "Arabe", "Arménien", "Basque", "Bulgare", "Catalan", "Croate", "Danois", "Espagnol", "Espéranto", "Estonien", "Finnois", "Français", "Gallois", "Grec", "Hébreu", "Hongrois", "Irlandais", "Islandais", "Italien", "Latin", "Letton", "Lituanien", "Malais", "Néerlandais", "Norvégien", "Polonais", "Portugais", "Roumain", "Russe", "Slovaque", "Slovène", "Suédois", "Tchèque", "Turc", "Ukrainien");
	private Dictionary dicLanguage;
	
	public Language(String langue) throws IOException {
		this.dicLanguage = new Dictionary(langue);
	}
	
	public Dictionary getDictionary() {
		return dicLanguage;
	}
	
}


//tets