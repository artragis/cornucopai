package com.cornucopai.game;

/** Thrown when agentName is already used by another agent (FR-018, mapped to HTTP 409). */
public class AgentNameConflictException extends RuntimeException {
  public AgentNameConflictException(String agentName) {
    super("agentName already used by another agent: " + agentName);
  }
}
