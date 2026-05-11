package com.smartbi.taskcli;

/**
 * Configuration from environment variables (no secrets printed).
 */
public final class SmartbiConfig {

  private final String url;
  private final String username;
  private final String password;

  private SmartbiConfig(String url, String username, String password) {
    this.url = url;
    this.username = username;
    this.password = password;
  }

  /**
   * @return config if all required variables are set; otherwise null
   */
  public static SmartbiConfig fromEnvOrNull() {
    String url = trimToNull(System.getenv("SMARTBI_URL"));
    String user = trimToNull(System.getenv("SMARTBI_USERNAME"));
    String pass = System.getenv("SMARTBI_PASSWORD");
    if (pass != null) {
      pass = pass.isEmpty() ? null : pass;
    }
    if (url == null || user == null || pass == null) {
      return null;
    }
    return new SmartbiConfig(url, user, pass);
  }

  /**
   * Human-safe reason for validation failure (never includes password).
   */
  public static String envValidationMessage() {
    if (trimToNull(System.getenv("SMARTBI_URL")) == null) {
      return "SMARTBI_URL is not set";
    }
    if (trimToNull(System.getenv("SMARTBI_USERNAME")) == null) {
      return "SMARTBI_USERNAME is not set";
    }
    String pass = System.getenv("SMARTBI_PASSWORD");
    if (pass == null || pass.isEmpty()) {
      return "SMARTBI_PASSWORD is not set";
    }
    return null;
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
