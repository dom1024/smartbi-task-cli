package com.smartbi.taskcli;

/**
 * Masks values after sensitive keywords (password, token, cookies, auth headers, etc.)
 * for any text destined for stderr or stdout messages derived from throwables.
 */
public final class SensitiveSanitizer {

  private SensitiveSanitizer() {}

  public static String sanitize(String raw) {
    if (raw == null) {
      return "";
    }
    String s = raw;
    s = s.replaceAll("(?i)(Authorization\\s*:\\s*)(Bearer\\s+)(\\S+)", "$1$2***");
    s = s.replaceAll("(?i)(Authorization\\s*:\\s*)(Basic\\s+)(\\S+)", "$1$2***");
    s = s.replaceAll("(?i)(Authorization\\s*:\\s*)(\\S+)", "$1***");
    s = s.replaceAll("(?i)((?:password|pwd|SMARTBI_PASSWORD)\\s*=\\s*)(\\S+)", "$1***");
    s = s.replaceAll("(?i)((?:password|pwd|SMARTBI_PASSWORD)\\s*:\\s*)(\\S+)", "$1***");
    s = s.replaceAll("(?i)(\\btoken\\b\\s*[:=]\\s*)(\\S+)", "$1***");
    s = s.replaceAll("(?i)(JSESSIONID\\s*=\\s*)([^;\\s]+)", "$1***");
    s = s.replaceAll("(?i)(Cookie\\s*:\\s*)([^\\r\\n]+)", "$1***");
    s = s.replaceAll("(?i)(\\\"password\\\"\\s*:\\s*\\\")([^\"\\\\]*)(\")", "$1***$3");
    s = s.replaceAll("(?i)(\\\"pwd\\\"\\s*:\\s*\\\")([^\"\\\\]*)(\")", "$1***$3");
    s = s.replaceAll("(?i)(\\\"token\\\"\\s*:\\s*\\\")([^\"\\\\]*)(\")", "$1***$3");
    s = s.replaceAll("(?i)(\\\"cookie\\\"\\s*:\\s*\\\")([^\"\\\\]*)(\")", "$1***$3");
    s = s.replaceAll("(?i)(\\\"JSESSIONID\\\"\\s*:\\s*\\\")([^\"\\\\]*)(\")", "$1***$3");
    s = s.replaceAll("(?i)(\\\"Authorization\\\"\\s*:\\s*\\\")([^\"\\\\]*)(\")", "$1***$3");
    s = s.replaceAll("(?i)(\\\"SMARTBI_PASSWORD\\\"\\s*:\\s*\\\")([^\"\\\\]*)(\")", "$1***$3");
    return s;
  }
}
