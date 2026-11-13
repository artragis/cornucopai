package com.cornucopai.game;

/** Thrown when an agent id does not exist (mapped to HTTP 404 by the API layer). */
public class AgentNotFoundException extends RuntimeException {
  public AgentNotFoundException(String id) {
    super("unknown agent id: " + id);
  }
}
