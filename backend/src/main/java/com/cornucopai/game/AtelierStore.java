package com.cornucopai.game;

import com.cornucopai.game.model.Atelier;
import com.cornucopai.game.model.Leg;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Filesystem persistence of ateliers (research R-002, R-008): one folder
 * {@code atelier{yyyyMMdd-HHmmss}} per game containing {@code atelier.json}
 * and one {@code leg-{NNN}-{playerSlug}.json} per played card.
 */
@ApplicationScoped
public class AtelierStore {

    private static final Logger LOG = Logger.getLogger(AtelierStore.class);
    private static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path rootDir;
    private final ObjectMapper mapper;

    @Inject
    public AtelierStore(@ConfigProperty(name = "cornucopai.ateliers.root-dir", defaultValue = "ateliers") String rootDir,
                        ObjectMapper mapper) {
        this(Path.of(rootDir), mapper);
    }

    /** Test-friendly constructor. */
    public AtelierStore(Path rootDir, ObjectMapper mapper) {
        this.rootDir = rootDir;
        this.mapper = mapper;
    }

    /** {@code atelier{yyyyMMdd-HHmmss}} — dashes, never colons (FR-004). */
    public String newAtelierId() {
        return "atelier" + LocalDateTime.now().format(ID_FORMAT);
    }

    /**
     * Creates the atelier folder and returns the id actually used. If the
     * timestamp-based id already exists (two games in the same second), a
     * numeric suffix is appended (spec edge case).
     */
    public String createAtelierDir(String baseId) {
        try {
            Files.createDirectories(rootDir);
            String id = baseId;
            int suffix = 2;
            while (Files.exists(rootDir.resolve(id))) {
                id = baseId + "-" + suffix++;
            }
            Files.createDirectories(rootDir.resolve(id));
            LOG.infof("Created atelier dir %s", id);
            return id;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void saveAtelier(String atelierId, Atelier atelier) {
        writeJson(rootDir.resolve(atelierId).resolve("atelier.json"), atelier);
    }

    /** Writes (or overwrites) the leg file — first PENDING, later updated in place. */
    public void saveLeg(String atelierId, int turnIndex, String playerName, Leg leg) {
        String fileName = String.format("leg-%03d-%s.json", turnIndex, slug(playerName));
        writeJson(rootDir.resolve(atelierId).resolve(fileName), leg);
    }

    public Optional<Atelier> loadAtelier(String atelierId) {
        Path file = rootDir.resolve(atelierId).resolve("atelier.json");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readValue(file.toFile(), Atelier.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** All leg files of an atelier, ordered by turn index (filename order). */
    public List<Leg> loadLegs(String atelierId) {
        Path dir = rootDir.resolve(atelierId);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().startsWith("leg-"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .map(this::readLeg)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** All ateliers, newest first. */
    public List<Atelier> listAteliers() {
        if (!Files.isDirectory(rootDir)) {
            return List.of();
        }
        try (Stream<Path> dirs = Files.list(rootDir)) {
            return dirs.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .map(this::loadAtelier)
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(Atelier::createdAt).reversed()
                            .thenComparing(Comparator.comparing(Atelier::id).reversed()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** {@code Joueur-1} → {@code joueur-1}. */
    public static String slug(String playerName) {
        return playerName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private Leg readLeg(Path file) {
        try {
            return mapper.readValue(file.toFile(), Leg.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeJson(Path file, Object value) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
