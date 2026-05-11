package com.smartbi.taskcli;

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
   * Opens a session, submits runTaskById via {@link ScheduleTaskService#executeTask(String)}, closes connector.
   *
   * @return {@link SubmitOutcome}; {@link SubmitPhase#OK} only when login succeeds and
   *         {@link ScheduleTaskService#executeTask(String)} returns true (remote invocation succeed bit).
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
          closeEx.printStackTrace(System.err);
        }
      }
    }
  }
}
