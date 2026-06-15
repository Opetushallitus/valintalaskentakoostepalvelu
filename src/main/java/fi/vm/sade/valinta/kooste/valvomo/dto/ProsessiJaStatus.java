package fi.vm.sade.valinta.kooste.valvomo.dto;

import java.time.Duration;
import java.time.Instant;

public class ProsessiJaStatus<T> {

  public static enum Status {
    STARTED,
    FINISHED,
    FAILED
  }

  private T prosessi;
  private String message;
  private Status status;
  private String duration;

  public ProsessiJaStatus() {
    this.prosessi = null;
    this.message = null;
    this.status = null;
  }

  public ProsessiJaStatus(T prosessi, String message, Status status) {
    this.prosessi = prosessi;
    this.duration = createDurationSoFar(prosessi);
    this.message = message;
    this.status = status;
  }

  public String getDuration() {
    return duration;
  }

  public String getDurationSinceStarted() {
    return createDurationSoFar(prosessi);
  }

  public Status getStatus() {
    return status;
  }

  public T getProsessi() {
    return prosessi;
  }

  public String getMessage() {
    return message;
  }

  private static <T> String createDurationSoFar(T prosessi) {
    if (prosessi instanceof Timestamped) {
      Instant createdAt = ((Timestamped) prosessi).getCreatedAt().toInstant();
      Duration duration = Duration.between(createdAt, Instant.now());
      return String.valueOf(duration.getSeconds());
    }
    return "<< process is not timestamped >>";
  }
}
