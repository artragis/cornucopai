package com.cornucopai.game.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * T022 (spec 007 US2): the per-atelier shared project memory is a thread-safe ordered journal of
 * facts; recent(n) returns the n most recent in order (R-049, FR-008 with n=20 per clarification
 * 2026-11-11).
 */
class SharedProjectMemoryTest {

  @Test
  void addAndFactsKeepInsertionOrder() {
    SharedProjectMemory memory = new SharedProjectMemory("atelierX");
    memory.add(new ProjectFact("fait 1", "Joueur-1", 1, Instant.parse("2026-11-11T10:00:00Z")));
    memory.add(new ProjectFact("fait 2", "Joueur-2", 2, Instant.parse("2026-11-11T10:01:00Z")));
    assertThat(memory.facts()).extracting(ProjectFact::text).containsExactly("fait 1", "fait 2");
    assertThat(memory.atelierId()).isEqualTo("atelierX");
  }

  @Test
  void recentReturnsMostRecentInOrder() {
    SharedProjectMemory memory = new SharedProjectMemory("atelierX");
    for (int i = 1; i <= 25; i++) {
      memory.add(
          new ProjectFact("fait " + i, "Joueur-1", i, Instant.parse("2026-11-11T10:00:00Z")));
    }
    List<ProjectFact> recent = memory.recent(20);
    assertThat(recent).hasSize(20);
    assertThat(recent.get(0).text()).isEqualTo("fait 6"); // 25-20+1
    assertThat(recent.get(19).text()).isEqualTo("fait 25");
  }

  @Test
  void recentWithCountAboveSizeReturnsAll() {
    SharedProjectMemory memory = new SharedProjectMemory("atelierX");
    memory.add(new ProjectFact("a", "J1", 1, Instant.now()));
    assertThat(memory.recent(20)).hasSize(1);
  }

  @Test
  void emptyMemoryReturnsEmpty() {
    SharedProjectMemory memory = new SharedProjectMemory("atelierX");
    assertThat(memory.facts()).isEmpty();
    assertThat(memory.recent(20)).isEmpty();
  }
}
