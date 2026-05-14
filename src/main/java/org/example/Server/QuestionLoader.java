package org.example.Server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.Shared.GameConfig;
import org.example.Shared.Question;

import java.io.*;
import java.lang.reflect.Type;
import java.util.List;

/**
 * QuestionLoader.java
 *
 * Reads questions.json from disk and returns a List<Question>.
 * Called once by Server.java at startup before the game begins.
 *
 * Usage:
 *   List<Question> questions = QuestionLoader.load();
 */
public class QuestionLoader {

    /**
     * Loads questions from the path defined in GameConfig.QUESTIONS_FILE.
     *
     * @return list of Question objects ready to use
     * @throws RuntimeException if the file is missing or malformed
     */
    public static List<Question> load() {
        return load(GameConfig.QUESTIONS_FILE);
    }

    /**
     * Loads questions from a custom path. Useful for testing.
     *
     * @param filePath path to the JSON file
     * @return list of Question objects
     */
    public static List<Question> load(String filePath) {
        System.out.println("[QuestionLoader] Loading questions from: " + filePath);

        try (Reader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Question>>() {}.getType();
            List<Question> questions = gson.fromJson(reader, listType);

            if (questions == null || questions.isEmpty()) {
                throw new RuntimeException("No questions found in: " + filePath);
            }

            System.out.println("[QuestionLoader] Loaded " + questions.size() + " questions.");
            return questions;

        } catch (FileNotFoundException e) {
            throw new RuntimeException("Questions file not found: " + filePath +
                    "\nMake sure it exists at: " + new File(filePath).getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read questions file: " + e.getMessage());
        }
    }
}
