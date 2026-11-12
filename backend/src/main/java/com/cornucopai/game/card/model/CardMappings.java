package com.cornucopai.game.card.model;

import java.util.List;
import java.util.Map;

/**
 * Security cross-references of one card, read from a Cornucopia {@code
 * <edition>-mappings-<version>.yaml} file (R-024). Lists hold YAML scalars (strings or numbers) as
 * {@code Object}; {@code capecMap} is the optional per-CAPEC detail map. Any field may be null when
 * the mappings file doesn't define it.
 */
public record CardMappings(
    List<Object> stride,
    List<Object> stridePrint,
    List<Object> capec,
    Map<String, Object> capecMap,
    List<Object> owaspAsvs,
    List<Object> owaspAsvsPrint,
    List<Object> owaspDevGuide,
    List<Object> owaspDevGuidePrint,
    List<Object> safecode) {}
