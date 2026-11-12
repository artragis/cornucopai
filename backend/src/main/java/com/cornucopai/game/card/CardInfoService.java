package com.cornucopai.game.card;

import com.cornucopai.game.card.model.CardInfo;
import com.cornucopai.game.card.model.CardMappings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Aggregates everything interesting about one Cornucopia card from its id alone (spec 004
 * FR-005..FR-009, research R-024):
 *
 * <ol>
 *   <li>the cards YAML {@code <home>/source/*-cards-*-en.yaml} — latest version per edition
 *       (numeric semver compare, same rule as the catalog R-013): id, value, description, url,
 *       misc, family, set;
 *   <li>the mappings YAML {@code <home>/source/<edition>-mappings-<version>.yaml} — STRIDE, CAPEC
 *       (+ capec_map), ASVS, OWASP Dev Guide, SAFECode;
 *   <li>the rich markdown under {@code
 *       <home>/cornucopia.owasp.org/data/cards/<edition>-cards-<version>-en/<familyDir>/<cardId>/}
 *       — {@code explanation.md} and {@code technical-note.md}, located by WALKING the family
 *       directories (never by computing a slug).
 * </ol>
 *
 * <p>The id → locations index is built once and cached. When an id exists in several sets, all
 * locations are reported and {@code preferredSetIds} (the current atelier's sets) wins (FR-008).
 * Unknown id → {@link Optional#empty()} (the tool renders "Carte inconnue"). Missing mappings/rich
 * content → null fields, never an error.
 */
@ApplicationScoped
public class CardInfoService {

  private static final Logger LOG = Logger.getLogger(CardInfoService.class);
  private static final Pattern CARD_FILE_PATTERN =
      Pattern.compile("^(.+)-cards-([0-9][0-9.]*)-en\\.yaml$");

  private final Path home;
  private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

  private volatile Map<String, List<CardLocation>> index;
  private final Map<String, Map<String, CardMappings>> mappingsCache = new HashMap<>();
  private final Map<String, Map<String, Path>> richIndexCache = new HashMap<>();

  @Inject
  public CardInfoService(
      @ConfigProperty(name = "cornucopai.cornucopia-home", defaultValue = "") String home) {
    this(home == null || home.isBlank() ? null : Path.of(home));
  }

  /** Test-friendly constructor; {@code home} is the Cornucopia repo root. */
  public CardInfoService(Path home) {
    this.home = home;
  }

  /** One place where a card id lives: set + version + suit + the en card file. */
  private record CardLocation(
      String setId, String version, String suitId, String suitName, Path cardFile) {}

  public Optional<CardInfo> infoFor(String cardId) {
    return infoFor(cardId, Set.of());
  }

  /**
   * Returns the card info for {@code cardId}, preferring the atelier's current sets.
   *
   * @param preferredSetIds sets of the current atelier — they win on multi-set ids (FR-008)
   */
  public Optional<CardInfo> infoFor(String cardId, Set<String> preferredSetIds) {
    if (cardId == null || cardId.isBlank()) {
      return Optional.empty();
    }
    Map<String, List<CardLocation>> idx = index();
    List<CardLocation> locs = idx.getOrDefault(cardId, List.of());
    if (locs.isEmpty()) {
      locs = idx.getOrDefault(cardId.toUpperCase(), List.of());
    }
    if (locs.isEmpty()) {
      return Optional.empty();
    }
    CardLocation primary =
        locs.stream()
            .filter(l -> preferredSetIds != null && preferredSetIds.contains(l.setId()))
            .findFirst()
            .orElse(locs.get(0));

    CardSetFile.CardEntry entry = findCardEntry(primary, cardId);
    String language = readMetaLanguage(primary.cardFile()).orElse("en");
    CardMappings mappings = mappingsFor(primary, cardId);
    Path richDir = richCardDir(primary.setId(), primary.version(), cardId);
    String explanation = readIfExists(richDir == null ? null : richDir.resolve("explanation.md"));
    String technicalNote =
        readIfExists(richDir == null ? null : richDir.resolve("technical-note.md"));

    List<String> locations =
        locs.stream().map(l -> l.setId() + ":" + l.version() + ":" + l.suitId()).toList();
    return Optional.of(
        new CardInfo(
            cardId,
            entry != null ? entry.getValue() : null,
            primary.setId(),
            primary.version(),
            primary.suitId(),
            primary.suitName(),
            language,
            entry != null ? entry.getDesc() : null,
            entry != null ? entry.getUrl() : null,
            entry != null ? entry.getMisc() : null,
            mappings,
            explanation,
            technicalNote,
            locations));
  }

  // --- id → locations index (latest version per edition, en files) ---

  private Map<String, List<CardLocation>> index() {
    Map<String, List<CardLocation>> local = index;
    if (local == null) {
      synchronized (this) {
        local = index;
        if (local == null) {
          local = buildIndex();
          index = local;
        }
      }
    }
    return local;
  }

  private Map<String, List<CardLocation>> buildIndex() {
    Map<String, List<CardLocation>> byId = new HashMap<>();
    if (home == null || !Files.isDirectory(home.resolve("source"))) {
      LOG.warnf("Cornucopia repo not configured or missing — card info index is empty");
      return byId;
    }
    // group en card files by edition, keep the latest version only (R-013)
    Map<String, Path> latestEnFile = new TreeMap<>();
    Map<String, String> latestVersion = new HashMap<>();
    try (Stream<Path> files = Files.list(home.resolve("source"))) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        Matcher m = CARD_FILE_PATTERN.matcher(file.getFileName().toString());
        if (!m.matches()) {
          continue;
        }
        String edition = m.group(1);
        String version = m.group(2);
        String metaEdition = readMetaEdition(file).orElse(edition);
        String metaVersion = readMetaVersion(file).orElse(version);
        String current = latestVersion.get(metaEdition);
        if (current == null || CardCatalogService.compareVersions(metaVersion, current) > 0) {
          latestVersion.put(metaEdition, metaVersion);
          latestEnFile.put(metaEdition, file);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    for (Map.Entry<String, Path> e : latestEnFile.entrySet()) {
      String edition = e.getKey();
      String version = latestVersion.get(edition);
      CardSetFile parsed = parse(e.getValue(), CardSetFile.class);
      if (parsed.getSuits() == null) {
        continue;
      }
      for (CardSetFile.SuitEntry suit : parsed.getSuits()) {
        if (suit.getCards() == null) {
          continue;
        }
        for (CardSetFile.CardEntry card : suit.getCards()) {
          if (card.getId() == null) {
            continue;
          }
          byId.computeIfAbsent(card.getId(), k -> new ArrayList<>())
              .add(new CardLocation(edition, version, suit.getId(), suit.getName(), e.getValue()));
        }
      }
    }
    return byId;
  }

  private CardSetFile.CardEntry findCardEntry(CardLocation loc, String cardId) {
    CardSetFile parsed = parse(loc.cardFile(), CardSetFile.class);
    if (parsed.getSuits() == null) {
      return null;
    }
    for (CardSetFile.SuitEntry suit : parsed.getSuits()) {
      if (suit.getCards() == null) {
        continue;
      }
      for (CardSetFile.CardEntry card : suit.getCards()) {
        if (cardId.equals(card.getId())) {
          return card;
        }
      }
    }
    return null;
  }

  // --- mappings ---

  private CardMappings mappingsFor(CardLocation loc, String cardId) {
    if (home == null) {
      return null;
    }
    Path mappingsFile =
        home.resolve("source").resolve(loc.setId() + "-mappings-" + loc.version() + ".yaml");
    if (!Files.isRegularFile(mappingsFile)) {
      return null;
    }
    String cacheKey = loc.setId() + ":" + loc.version();
    Map<String, CardMappings> perFile =
        mappingsCache.computeIfAbsent(cacheKey, k -> loadMappings(mappingsFile));
    return perFile.get(cardId);
  }

  private Map<String, CardMappings> loadMappings(Path file) {
    Map<String, CardMappings> byCard = new HashMap<>();
    CardMappingsFile parsed = parse(file, CardMappingsFile.class);
    if (parsed.getSuits() == null) {
      return byCard;
    }
    for (CardMappingsFile.SuitEntry suit : parsed.getSuits()) {
      if (suit.getCards() == null) {
        continue;
      }
      for (CardMappingsFile.CardMappingEntry c : suit.getCards()) {
        if (c.getId() == null) {
          continue;
        }
        byCard.put(
            c.getId(),
            new CardMappings(
                c.getStride(),
                c.getStridePrint(),
                c.getCapec(),
                c.getCapecMap(),
                c.getOwaspAsvs(),
                c.getOwaspAsvsPrint(),
                c.getOwaspDevGuide(),
                c.getOwaspDevGuidePrint(),
                c.getSafecode()));
      }
    }
    return byCard;
  }

  // --- rich content (walked, never slug-computed) ---

  private Path richCardDir(String setId, String version, String cardId) {
    if (home == null) {
      return null;
    }
    Path setDir =
        home.resolve("cornucopia.owasp.org/data/cards/" + setId + "-cards-" + version + "-en");
    if (!Files.isDirectory(setDir)) {
      return null;
    }
    Map<String, Path> cards =
        richIndexCache.computeIfAbsent(
            setDir.toString(),
            k -> {
              Map<String, Path> m = new HashMap<>();
              try (Stream<Path> families = Files.list(setDir)) {
                for (Path family : families.filter(Files::isDirectory).toList()) {
                  try (Stream<Path> cardsInFamily = Files.list(family)) {
                    for (Path card : cardsInFamily.filter(Files::isDirectory).toList()) {
                      m.put(card.getFileName().toString(), card);
                    }
                  }
                }
              } catch (IOException e) {
                LOG.warnf("Cannot walk %s: %s", setDir, e.getMessage());
              }
              return m;
            });
    return cards.get(cardId);
  }

  // --- small helpers ---

  private <T> T parse(Path file, Class<T> type) {
    try {
      return yaml.readValue(file.toFile(), type);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot parse " + file, e);
    }
  }

  private Optional<String> readMetaEdition(Path file) {
    return readMeta(file, "edition");
  }

  private Optional<String> readMetaVersion(Path file) {
    return readMeta(file, "version");
  }

  private Optional<String> readMetaLanguage(Path file) {
    return readMeta(file, "language");
  }

  private Optional<String> readMeta(Path file, String field) {
    try {
      JsonNode root = yaml.readTree(file.toFile());
      JsonNode v = root == null ? null : root.path("meta").path(field);
      return v == null || v.isMissingNode() || v.isNull()
          ? Optional.empty()
          : Optional.of(v.asText());
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  private static String readIfExists(Path file) {
    if (file == null || !Files.isRegularFile(file)) {
      return null;
    }
    try {
      return Files.readString(file);
    } catch (IOException e) {
      return null;
    }
  }
}
