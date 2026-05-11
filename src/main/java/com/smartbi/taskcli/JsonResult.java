package com.smartbi.taskcli;

/**
 * Single-line JSON for stdout (scheduling systems).
 */
public final class JsonResult {

  private final boolean success;
  private final String idType;
  private final String id;
  private final String message;

  public JsonResult(boolean success, String idType, String id, String message) {
    this.success = success;
    this.idType = idType == null ? "" : idType;
    this.id = id == null ? "" : id;
    this.message = message == null ? "" : message;
  }

  public String toLine() {
    return "{\"success\":"
        + success
        + ",\"idType\":\""
        + escapeJson(idType)
        + "\",\"id\":\""
        + escapeJson(id)
        + "\",\"message\":\""
        + escapeJson(message)
        + "\"}";
  }

  static String escapeJson(String raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(raw.length() + 8);
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      switch (c) {
        case '\\':
          sb.append("\\\\");
          break;
        case '"':
          sb.append("\\\"");
          break;
        case '\b':
          sb.append("\\b");
          break;
        case '\f':
          sb.append("\\f");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        default:
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
      }
    }
    return sb.toString();
  }
}
