package dictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

public class Dictionary {
    private HashSet<String> words;

    public Dictionary(String language) throws IOException {
        words = new HashSet<>();
        loadDictionary(language + "Dictionary.txt");
    }

    private void loadDictionary(String languageFileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream(languageFileName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line.trim().toLowerCase());
            }
        }
    }

    public boolean validWord(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        return words.contains(word.toLowerCase());
    }

    // Méthode ajoutée pour récupérer tous les mots du dictionnaire
    public List<String> getWords() {
        return new ArrayList<>(words);
    }
}