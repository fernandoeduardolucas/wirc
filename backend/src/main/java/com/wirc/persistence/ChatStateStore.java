package com.wirc.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Component
public class ChatStateStore {
    private static final TypeReference<List<RoomSessionSnapshot>> SNAPSHOT_LIST = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final Path stateFile;

    public ChatStateStore(ObjectMapper objectMapper, @Value("${wirc.state-file}") String stateFile) {
        this.objectMapper = objectMapper;
        this.stateFile = Path.of(stateFile);
    }

    public Optional<List<RoomSessionSnapshot>> load() {
        if (Files.notExists(stateFile)) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(stateFile.toFile(), SNAPSHOT_LIST));
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler o ficheiro de estado: " + stateFile, exception);
        }
    }

    public void save(List<RoomSessionSnapshot> snapshots) {
        try {
            Path parent = stateFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(stateFile.toFile(), snapshots);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gravar o ficheiro de estado: " + stateFile, exception);
        }
    }
}
