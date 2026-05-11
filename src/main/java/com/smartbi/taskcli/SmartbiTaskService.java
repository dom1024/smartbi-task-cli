package com.smartbi.taskcli;

import java.io.PrintWriter;
import java.io.StringWriter;

import smartbi.net.sf.json.JSONObject;
import smartbi.sdk.ClientConnector;
import smartbi.sdk.InvokeResult;
import smartbi.sdk.service.scheduletask.ScheduleTaskService;

/**
 * All Smartbi SDK usage is confined to this class.
 */
public final class SmartbiTaskService {

  public enum SubmitPhase {
    OK,
    LOGIN_FAILED,
    EXECUTE_FAILED
  }

  public static final class SubmitOutcome {
    private final SubmitPhase phase;

    private SubmitOutcome(SubmitPhase phase) {
      this.phase = phase;
    }

    public static SubmitOutcome ok() {
      return new SubmitOutcome(SubmitPhase.OK);
    }

    public static SubmitOutcome loginFailed() {
      return new SubmitOutcome(SubmitPhase.LOGIN_FAILED);
    }

    public static SubmitOutcome executeFailed() {
      return new SubmitOutcome(SubmitPhase.EXECUTE_FAILED);
    }

    public SubmitPhase getPhase() {
      return phase;
    }
  }

  /**
   * Outcome of {@link #debugRunSchedule(String, String, String, String)} (raw {@link InvokeResult} fields).
   */
  public static final class DebugScheduleInvokeResult {
    public final boolean loginFailed;
    public final boolean threw;
    /** Non-empty when {@link #loginFailed} or {@link #threw}; already safe for stdout */
    public final String errorMessageSanitized;
    public final boolean obtainedInvokeResult;
    public final boolean isSucceed;
    /** String form of {@link InvokeResult#getResult()} */
    public final String resultText;
    /** String form of {@link InvokeResult#getOriginalResult()} */
    public final String originalResultText;

    private DebugScheduleInvokeResult(
        boolean loginFailed,
        boolean threw,
        String errorMessageSanitized,
        boolean obtainedInvokeResult,
        boolean isSucceed,
        String resultText,
        String originalResultText) {
      this.loginFailed = loginFailed;
      this.threw = threw;
      this.errorMessageSanitized = errorMessageSanitized == null ? "" : errorMessageSanitized;
      this.obtainedInvokeResult = obtainedInvokeResult;
      this.isSucceed = isSucceed;
      this.resultText = resultText == null ? "" : resultText;
      this.originalResultText = originalResultText == null ? "" : originalResultText;
    }

    public static DebugScheduleInvokeResult loginFailed() {
      return new DebugScheduleInvokeResult(
          true,
          false,
          "Smartbi session could not be established (login failed, URL, or network)",
          false,
          false,
          "",
          "");
    }

    public static DebugScheduleInvokeResult thrown(Throwable t) {
      return new DebugScheduleInvokeResult(
          false, true, sanitizeThrowableMessages(t), false, false, "", "");
    }

    public static DebugScheduleInvokeResult fromInvokeResult(InvokeResult ir) {
      Object resObj = ir.getResult();
      JSONObject origObj = ir.getOriginalResult();
      String rt = resObj == null ? "null" : String.valueOf(resObj);
      String ot = origObj == null ? "null" : origObj.toString();
      return new DebugScheduleInvokeResult(false, false, "", true, ir.isSucceed(), rt, ot);
    }
  }

  private SmartbiTaskService() {}

  /**
   * Diagnostic: same remote entry as {@code ScheduleTaskService.executeSchedule} —
   * {@code ScheduleSDK.run} via {@link ClientConnector#remoteInvoke}.
   */
  public static DebugScheduleInvokeResult debugRunSchedule(
      String smartbiUrl, String username, String password, String scheduleId) {
    ClientConnector connector = null;
    try {
      connector = new ClientConnector(smartbiUrl);
      if (!connector.open(username, password)) {
        return DebugScheduleInvokeResult.loginFailed();
      }
      InvokeResult ir =
          connector.remoteInvoke("ScheduleSDK", "run", new Object[] {scheduleId});
      return DebugScheduleInvokeResult.fromInvokeResult(ir);
    } catch (Throwable t) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      System.err.println(SensitiveSanitizer.sanitize(sw.toString()));
      return DebugScheduleInvokeResult.thrown(t);
    } finally {
      if (connector != null) {
        try {
          connector.close();
        } catch (Throwable closeEx) {
          StringWriter sw = new StringWriter();
          closeEx.printStackTrace(new PrintWriter(sw));
          System.err.println(SensitiveSanitizer.sanitize(sw.toString()));
        }
      }
    }
  }

  /**
   * Plan-level immediate run: {@link ScheduleTaskService#executeSchedule(String)} / {@code ScheduleSDK.run}.
   */
  public static SubmitOutcome submitSchedule(
      String smartbiUrl, String username, String password, String scheduleId) {
    ClientConnector connector = null;
    try {
      connector = new ClientConnector(smartbiUrl);
      if (!connector.open(username, password)) {
        return SubmitOutcome.loginFailed();
      }
      ScheduleTaskService scheduleTaskService = new ScheduleTaskService(connector);
      boolean remoteOk = scheduleTaskService.executeSchedule(scheduleId);
      if (!remoteOk) {
        return SubmitOutcome.executeFailed();
      }
      return SubmitOutcome.ok();
    } finally {
      if (connector != null) {
        try {
          connector.close();
        } catch (Throwable closeEx) {
          StringWriter sw = new StringWriter();
          closeEx.printStackTrace(new PrintWriter(sw));
          System.err.println(SensitiveSanitizer.sanitize(sw.toString()));
        }
      }
    }
  }

  /**
   * Task-level: {@link ScheduleTaskService#executeTask(String)} / {@code ScheduleSDK.runTaskById}.
   */
  public static SubmitOutcome submitTask(String smartbiUrl, String username, String password, String taskId) {
    ClientConnector connector = null;
    try {
      connector = new ClientConnector(smartbiUrl);
      if (!connector.open(username, password)) {
        return SubmitOutcome.loginFailed();
      }
      ScheduleTaskService scheduleTaskService = new ScheduleTaskService(connector);
      boolean remoteOk = scheduleTaskService.executeTask(taskId);
      if (!remoteOk) {
        return SubmitOutcome.executeFailed();
      }
      return SubmitOutcome.ok();
    } finally {
      if (connector != null) {
        try {
          connector.close();
        } catch (Throwable closeEx) {
          StringWriter sw = new StringWriter();
          closeEx.printStackTrace(new PrintWriter(sw));
          System.err.println(SensitiveSanitizer.sanitize(sw.toString()));
        }
      }
    }
  }

  private static String sanitizeThrowableMessages(Throwable t) {
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
}
