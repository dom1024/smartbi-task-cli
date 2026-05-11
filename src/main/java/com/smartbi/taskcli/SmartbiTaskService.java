package com.smartbi.taskcli;

import java.io.PrintWriter;
import java.io.StringWriter;

import smartbi.sdk.ClientConnector;
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

  private SmartbiTaskService() {}

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
}
