package com.smartbi.taskcli;

/**
 * Single-line JSON for stdout (scheduling systems).
 */
public final class JsonResult {

  private final boolean success;
  private final String taskId;
  private final String message;

  public JsonResult(boolean success, String taskId, String message) {
    this.success = success;
    this.taskId = taskId == null ? "" : taskId;
    this.message = message == null ? "" : message;
  }

  public String toLine() {
    return "{\"success\":"
        + success
        + ",\"taskId\":\""
        + escapeJson(taskId)
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
