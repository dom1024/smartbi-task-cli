package com.smartbi.taskcli;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Usage: {@code java -jar smartbi-task-cli.jar run --task-id <taskId>}
 * <p>
 * Environment: SMARTBI_URL, SMARTBI_USERNAME, SMARTBI_PASSWORD
 */
public final class SmartbiTaskCli {

  private static final int EXIT_OK = 0;
  private static final int EXIT_SMARTBI_FAILED = 1;
  private static final int EXIT_USAGE_OR_ENV = 2;

  private SmartbiTaskCli() {}

  public static void main(String[] args) {
    String taskIdForJson = "";
    try {
      String parseError = validateArgs(args);
      if (parseError != null) {
        System.out.println(new JsonResult(false, "", parseError).toLine());
        System.exit(EXIT_USAGE_OR_ENV);
        return;
      }
      taskIdForJson = args[2];

      String envError = SmartbiConfig.envValidationMessage();
      if (envError != null) {
        System.out.println(new JsonResult(false, taskIdForJson, envError).toLine());
        System.exit(EXIT_USAGE_OR_ENV);
        return;
      }

      SmartbiConfig config = SmartbiConfig.fromEnvOrNull();
      if (config == null) {
        System.out.println(
            new JsonResult(false, taskIdForJson, "configuration incomplete").toLine());
        System.exit(EXIT_USAGE_OR_ENV);
        return;
      }

      SmartbiTaskService.SubmitOutcome outcome =
          SmartbiTaskService.submitTask(
              config.getUrl(), config.getUsername(), config.getPassword(), taskIdForJson);

      switch (outcome.getPhase()) {
        case OK:
          System.out.println(
              new JsonResult(true, taskIdForJson, "submitted").toLine());
          System.exit(EXIT_OK);
          return;
        case LOGIN_FAILED:
          System.out.println(
              new JsonResult(
                      false,
                      taskIdForJson,
                      "Smartbi session could not be established (login failed, URL, or network)")
                  .toLine());
          System.exit(EXIT_SMARTBI_FAILED);
          return;
        case EXECUTE_FAILED:
          System.out.println(
              new JsonResult(
                      false,
                      taskIdForJson,
                      "Smartbi invocation failed (executeTask returned false)")
                  .toLine());
          System.exit(EXIT_SMARTBI_FAILED);
          return;
        default:
          System.out.println(
              new JsonResult(false, taskIdForJson, "unexpected outcome").toLine());
          System.exit(EXIT_SMARTBI_FAILED);
          return;
      }
    } catch (Throwable t) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      System.err.println(SensitiveSanitizer.sanitize(sw.toString()));
      String safe = safeExceptionMessage(t);
      System.out.println(new JsonResult(false, taskIdForJson, safe).toLine());
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

  /**
   * Expect exactly: run --task-id &lt;id&gt;
   *
   * @return error message or null if valid
   */
  static String validateArgs(String[] args) {
    if (args == null || args.length != 3) {
      return "expected: run --task-id <taskId>";
    }
    if (!"run".equals(args[0])) {
      return "expected first argument: run";
    }
    if (!"--task-id".equals(args[1])) {
      return "expected second argument: --task-id";
    }
    if (args[2] == null || args[2].trim().isEmpty()) {
      return "missing --task-id value";
    }
    return null;
  }
}
