package com.smartbi.taskcli;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Usage:
 * <ul>
 *   <li>{@code java -jar smartbi-task-cli.jar run --schedule-id <scheduleId>}</li>
 *   <li>{@code java -jar smartbi-task-cli.jar run-task --task-id <taskId>}</li>
 * </ul>
 * Environment: SMARTBI_URL, SMARTBI_USERNAME, SMARTBI_PASSWORD
 */
public final class SmartbiTaskCli {

  private static final int EXIT_OK = 0;
  private static final int EXIT_SMARTBI_FAILED = 1;
  private static final int EXIT_USAGE_OR_ENV = 2;

  private SmartbiTaskCli() {}

  public static void main(String[] args) {
    String idTypeForJson = "";
    String idForJson = "";
    try {
      ParsedArgs parsed = parseArgs(args);
      if (parsed.error != null) {
        System.out.println(new JsonResult(false, "", "", parsed.error).toLine());
        System.exit(EXIT_USAGE_OR_ENV);
        return;
      }
      idTypeForJson = parsed.idTypeJson;
      idForJson = parsed.id;

      String envError = SmartbiConfig.envValidationMessage();
      if (envError != null) {
        System.out.println(new JsonResult(false, idTypeForJson, idForJson, envError).toLine());
        System.exit(EXIT_USAGE_OR_ENV);
        return;
      }

      SmartbiConfig config = SmartbiConfig.fromEnvOrNull();
      if (config == null) {
        System.out.println(
            new JsonResult(false, idTypeForJson, idForJson, "configuration incomplete").toLine());
        System.exit(EXIT_USAGE_OR_ENV);
        return;
      }

      SmartbiTaskService.SubmitOutcome outcome;
      if (parsed.scheduleMode) {
        outcome =
            SmartbiTaskService.submitSchedule(
                config.getUrl(), config.getUsername(), config.getPassword(), idForJson);
      } else {
        outcome =
            SmartbiTaskService.submitTask(
                config.getUrl(), config.getUsername(), config.getPassword(), idForJson);
      }

      switch (outcome.getPhase()) {
        case OK:
          System.out.println(new JsonResult(true, idTypeForJson, idForJson, "submitted").toLine());
          System.exit(EXIT_OK);
          return;
        case LOGIN_FAILED:
          System.out.println(
              new JsonResult(
                      false,
                      idTypeForJson,
                      idForJson,
                      "Smartbi session could not be established (login failed, URL, or network)")
                  .toLine());
          System.exit(EXIT_SMARTBI_FAILED);
          return;
        case EXECUTE_FAILED:
          System.out.println(
              new JsonResult(
                      false,
                      idTypeForJson,
                      idForJson,
                      parsed.scheduleMode
                          ? "Smartbi invocation failed (executeSchedule returned false)"
                          : "Smartbi invocation failed (executeTask returned false)")
                  .toLine());
          System.exit(EXIT_SMARTBI_FAILED);
          return;
        default:
          System.out.println(
              new JsonResult(false, idTypeForJson, idForJson, "unexpected outcome").toLine());
          System.exit(EXIT_SMARTBI_FAILED);
          return;
      }
    } catch (Throwable t) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      System.err.println(SensitiveSanitizer.sanitize(sw.toString()));
      String safe = safeExceptionMessage(t);
      System.out.println(new JsonResult(false, idTypeForJson, idForJson, safe).toLine());
      System.exit(EXIT_SMARTBI_FAILED);
    }
  }

  /**
   * Short message for JSON; never returns raw {@link Throwable#getMessage()} without masking.
   */
  private static String safeExceptionMessage(Throwable t) {
    if (t == null) {
      return "unexpected error";
    }
    StringBuilder sb = new StringBuilder();
    for (Throwable x = t; x != null; x = x.getCause()) {
      String m = x.getMessage();
      if (m != null && !m.trim().isEmpty()) {
        if (sb.length() > 0) {
          sb.append(" | ");
        }
        sb.append(m.trim());
      }
    }
    if (sb.length() == 0) {
      return t.getClass().getSimpleName();
    }
    return SensitiveSanitizer.sanitize(sb.toString());
  }

  static final class ParsedArgs {
    /** {@code true} -> {@code run --schedule-id}; {@code false} -> {@code run-task --task-id} */
    final boolean scheduleMode;
    /** JSON field value: {@code scheduleId} or {@code taskId} */
    final String idTypeJson;
    final String id;
    /** Non-null means parse failure; {@link #idTypeJson} and {@link #id} may be partial or empty */
    final String error;

    private ParsedArgs(boolean scheduleMode, String idTypeJson, String id, String error) {
      this.scheduleMode = scheduleMode;
      this.idTypeJson = idTypeJson == null ? "" : idTypeJson;
      this.id = id == null ? "" : id;
      this.error = error;
    }

    static ParsedArgs okSchedule(String scheduleId) {
      return new ParsedArgs(true, "scheduleId", scheduleId, null);
    }

    static ParsedArgs okTask(String taskId) {
      return new ParsedArgs(false, "taskId", taskId, null);
    }

    static ParsedArgs failure(String error) {
      return new ParsedArgs(false, "", "", error);
    }
  }

  /**
   * Accepts {@code run --schedule-id <id>} or {@code run-task --task-id <id>}.
   *
   * @return parsed command; {@link ParsedArgs#error} set when invalid
   */
  static ParsedArgs parseArgs(String[] args) {
    if (args == null || args.length != 3) {
      return ParsedArgs.failure(
          "expected: run --schedule-id <scheduleId> OR run-task --task-id <taskId>");
    }
    String cmd = args[0];
    String flag = args[1];
    String value = args[2];
    if (value == null || value.trim().isEmpty()) {
      if ("run".equals(cmd) && "--schedule-id".equals(flag)) {
        return ParsedArgs.failure("missing --schedule-id value");
      }
      if ("run-task".equals(cmd) && "--task-id".equals(flag)) {
        return ParsedArgs.failure("missing --task-id value");
      }
      return ParsedArgs.failure(
          "expected: run --schedule-id <scheduleId> OR run-task --task-id <taskId>");
    }
    String id = value.trim();
    if ("run".equals(cmd) && "--schedule-id".equals(flag)) {
      return ParsedArgs.okSchedule(id);
    }
    if ("run-task".equals(cmd) && "--task-id".equals(flag)) {
      return ParsedArgs.okTask(id);
    }
    if ("run".equals(cmd) && "--task-id".equals(flag)) {
      return ParsedArgs.failure(
          "obsolete: run --task-id; use run --schedule-id <scheduleId> for plan execution, "
              + "or run-task --task-id <taskId> for internal task id");
    }
    return ParsedArgs.failure(
        "expected: run --schedule-id <scheduleId> OR run-task --task-id <taskId>");
  }
}
