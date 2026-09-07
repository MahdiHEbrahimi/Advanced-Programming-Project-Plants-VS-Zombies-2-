package com.PVZ.database;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.PVZ.model.user.User;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class UserDatabase {

    private static final Path DATA_DIR = findProjectRoot().resolve("core/src/main/resources/data").normalize();

    private static Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle"))) {
                return current;
            }
            current = current.getParent();
        }

        // Running as a standalone JAR:
        // use the directory containing the JAR / current working directory.
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    }

    private static final Path USERS_DIR = DATA_DIR.resolve("users");
    private static final Path INDEX_FILE = DATA_DIR.resolve("users_index.json");

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    public static void init() throws Exception {
        Files.createDirectories(USERS_DIR);
        if (!Files.exists(INDEX_FILE)) {
            Files.writeString(INDEX_FILE, "[]");
        }
    }

    public static List<String> loadIndex() throws Exception {
        if (!Files.exists(INDEX_FILE)) return new ArrayList<>();
        String json = Files.readString(INDEX_FILE);
        return MAPPER.readValue(json, new TypeReference<List<String>>() {});
    }

    public static void addToIndex(String username) throws Exception {
        List<String> list = loadIndex();
        if (!list.contains(username)) {
            list.add(username);
            Files.writeString(INDEX_FILE, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(list));
        }
    }

    public static void save(String username, User user) throws Exception {
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(user);
        Files.writeString(USERS_DIR.resolve(username + ".dat"), json);
    }

    public static User load(String username) throws Exception {
        Path file = USERS_DIR.resolve(username + ".dat");
        if (!Files.exists(file)) return null;
        String json = Files.readString(file);
        return MAPPER.readValue(json, User.class);
    }

    public static boolean exists(String username) throws Exception {
        return loadIndex().contains(username);
    }

    public static void delete(String username) throws Exception {
        Files.deleteIfExists(USERS_DIR.resolve(username + ".dat"));
        List<String> list = loadIndex();
        list.remove(username);
        Files.writeString(INDEX_FILE, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(list));
    }
}
