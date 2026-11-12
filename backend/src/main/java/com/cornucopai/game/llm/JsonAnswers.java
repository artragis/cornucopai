package com.cornucopai.game.llm;

/**
 * Tolerant extraction of a JSON object from chatty local-model output (R-042): strips markdown
 * fences and extracts the first balanced {@code {...}} block.
 */
public final class JsonAnswers {

  private JsonAnswers() {}

  public static String stripToJson(String out) {
    if (out == null) {
      return "";
    }
    String s = out.trim();
    if (s.startsWith("```")) {
      int firstNl = s.indexOf('\n');
      if (firstNl > 0) {
        s = s.substring(firstNl + 1);
      }
      int end = s.lastIndexOf("```");
      if (end > 0) {
        s = s.substring(0, end);
      }
      s = s.trim();
    }
    int start = s.indexOf('{');
    if (start < 0) {
      return s;
    }
    int depth = 0;
    boolean inString = false;
    boolean escaped = false;
    for (int i = start; i < s.length(); i++) {
      char c = s.charAt(i);
      if (escaped) {
        escaped = false;
        continue;
      }
      if (c == '\\' && inString) {
        escaped = true;
        continue;
      }
      if (c == '"') {
        inString = !inString;
        continue;
      }
      if (inString) {
        continue;
      }
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          return s.substring(start, i + 1);
        }
      }
    }
    return s.substring(start);
  }
}
