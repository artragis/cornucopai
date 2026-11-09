package com.cornucopai.game.card;

import com.cornucopai.game.card.model.Card;
import com.cornucopai.game.card.model.CardFamily;
import com.cornucopai.game.card.model.CardSet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Scans the Cornucopia repo ({@code <CORNUCOPIA_HOME>/source/}) and exposes the
 * card catalog: one set per edition, latest version only (R-013), languages from
 * the file suffixes, families parsed from the {@code en} file (R-014). The catalog
 * is scanned once and cached (R-019). Also resolves the per-set language with an
 * {@code en} fallback within the latest version (R-015) and reads real cards for
 * selected families (FR-007).
 */
@ApplicationScoped
public class CardCatalogService {

    private static final Logger LOG = Logger.getLogger(CardCatalogService.class);
    private static final Pattern FILE_PATTERN =
            Pattern.compile("^(.+)-cards-([0-9][0-9.]*)-([a-zA-Z_]+)\\.yaml$");

    private final Path sourceDir;
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    private volatile CardCatalog cachedCatalog;
    private volatile Map<String, Map<String, Map<String, Path>>> cachedIndex;

    @Inject
    public CardCatalogService(@ConfigProperty(name = "cornucopai.cornucopia-home", defaultValue = "") String home) {
        this(home == null || home.isBlank() ? null : Path.of(home));
    }

    /** Test-friendly constructor; {@code home} is the Cornucopia repo root. */
    public CardCatalogService(Path home) {
        this.sourceDir = home == null ? null : home.resolve("source");
    }

    public CardCatalog catalog() {
        if (cachedCatalog == null) {
            cachedCatalog = scan();
        }
        return cachedCatalog;
    }

    public List<String> availableLanguages() {
        return catalog().availableLanguages();
    }

    public Optional<CardSet> set(String edition) {
        return catalog().sets().stream().filter(s -> s.id().equals(edition)).findFirst();
    }

    /** Lowercase-normalized resolved language for a set: chosen lang if available, else {@code en} (R-015). */
    public Optional<String> resolveLanguage(String edition, String language) {
        Optional<CardSet> set = set(edition);
        if (set.isEmpty()) {
            return Optional.empty();
        }
        String lang = language == null ? "en" : language.toLowerCase();
        List<String> languages = set.get().languages();
        if (languages.contains(lang)) {
            return Optional.of(lang);
        }
        if (languages.contains("en")) {
            return Optional.of("en");
        }
        return Optional.of(languages.get(0));
    }

    public boolean isKnownFamily(String edition, String familyId) {
        return set(edition)
                .map(s -> s.families().stream().anyMatch(f -> f.id().equals(familyId)))
                .orElse(false);
    }

    /**
     * Real cards of the given families for a set, read from the file in the
     * resolved language (FR-007). Cards carry id + description + resolved language.
     */
    public List<Card> cardsFor(String edition, Set<String> familyIds, String resolvedLanguage) {
        Optional<CardSet> set = set(edition);
        if (set.isEmpty()) {
            return List.of();
        }
        Path file = index().getOrDefault(edition, Map.of())
                .getOrDefault(set.get().version(), Map.of())
                .get(resolvedLanguage);
        if (file == null) {
            // fallback to en file of the same version
            file = index().getOrDefault(edition, Map.of())
                    .getOrDefault(set.get().version(), Map.of())
                    .get("en");
        }
        if (file == null) {
            return List.of();
        }
        CardSetFile parsed = parse(file);
        String lang = resolvedLanguage;
        List<Card> cards = new ArrayList<>();
        if (parsed.getSuits() == null) {
            return cards;
        }
        for (CardSetFile.SuitEntry suit : parsed.getSuits()) {
            if (!familyIds.contains(suit.getId()) || suit.getCards() == null) {
                continue;
            }
            for (CardSetFile.CardEntry c : suit.getCards()) {
                cards.add(new Card(c.getId(), c.getValue(), edition, suit.getId(), suit.getName(),
                        lang, c.getDesc(), c.getUrl(), c.getMisc()));
            }
        }
        return cards;
    }

    private CardCatalog scan() {
        if (sourceDir == null || !Files.isDirectory(sourceDir)) {
            LOG.warnf("Cornucopia repo not configured or missing (source dir=%s) — empty card catalog", sourceDir);
            return new CardCatalog(List.of(), List.of());
        }
        Map<String, Map<String, Map<String, Path>>> index = new TreeMap<>();
        try (Stream<Path> files = Files.list(sourceDir)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Matcher m = FILE_PATTERN.matcher(file.getFileName().toString());
                if (!m.matches()) {
                    continue;
                }
                String edition = m.group(1);
                String version = m.group(2);
                String lang = m.group(3).toLowerCase();
                JsonNode meta = readMeta(file);
                if (meta != null) {
                    if (meta.hasNonNull("edition")) edition = meta.get("edition").asText();
                    if (meta.hasNonNull("version")) version = meta.get("version").asText();
                    if (meta.hasNonNull("language")) lang = meta.get("language").asText().toLowerCase();
                }
                index.computeIfAbsent(edition, k -> new TreeMap<>())
                        .computeIfAbsent(version, k -> new TreeMap<>())
                        .put(lang, file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        cachedIndex = index;

        List<CardSet> sets = new ArrayList<>();
        for (Map.Entry<String, Map<String, Map<String, Path>>> e : index.entrySet()) {
            String edition = e.getKey();
            Map<String, Map<String, Path>> byVersion = e.getValue();
            String latestVersion = byVersion.keySet().stream()
                    .max(CardCatalogService::compareVersions).orElse(null);
            if (latestVersion == null) {
                continue;
            }
            Map<String, Path> byLang = byVersion.get(latestVersion);
            List<String> languages = new ArrayList<>(new TreeSet<>(byLang.keySet()));
            Path structureFile = byLang.getOrDefault("en", byLang.values().iterator().next());
            CardSetFile parsed = parse(structureFile);
            List<CardFamily> families = new ArrayList<>();
            int total = 0;
            if (parsed.getSuits() != null) {
                for (CardSetFile.SuitEntry suit : parsed.getSuits()) {
                    int count = suit.getCards() == null ? 0 : suit.getCards().size();
                    families.add(new CardFamily(suit.getId(), suit.getName(), edition, count));
                    total += count;
                }
            }
            sets.add(new CardSet(edition, latestVersion, languages, families, total));
        }
        sets.sort(Comparator.comparing(CardSet::id));
        Set<String> available = new TreeSet<>();
        for (CardSet s : sets) {
            available.addAll(s.languages());
        }
        return new CardCatalog(List.copyOf(available), List.copyOf(sets));
    }

    private Map<String, Map<String, Map<String, Path>>> index() {
        catalog(); // ensure scanned
        return cachedIndex;
    }

    private JsonNode readMeta(Path file) {
        try {
            JsonNode root = yaml.readTree(file.toFile());
            return root == null ? null : root.path("meta");
        } catch (IOException e) {
            LOG.warnf("Unreadable card file %s: %s", file, e.getMessage());
            return null;
        }
    }

    private CardSetFile parse(Path file) {
        try {
            return yaml.readValue(file.toFile(), CardSetFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot parse card file " + file, e);
        }
    }

    /** Numeric dotted-version compare (e.g. 3.0 > 2.2, 10.0 > 9.0). */
    static int compareVersions(String a, String b) {
        String[] as = a.split("\\.");
        String[] bs = b.split("\\.");
        int n = Math.max(as.length, bs.length);
        for (int i = 0; i < n; i++) {
            int x = i < as.length ? parseSegment(as[i]) : 0;
            int y = i < bs.length ? parseSegment(bs[i]) : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }

    private static int parseSegment(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
