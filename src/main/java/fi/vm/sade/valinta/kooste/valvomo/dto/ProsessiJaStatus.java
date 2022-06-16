package fi.vm.sade.valinta.kooste.valvomo.dto;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

public class ProsessiJaStatus<T> {

  public static enum Status {
    STARTED, FINISHED, FAILED
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
      DateTime createdAt = new DateTime(((Timestamped) prosessi).getCreatedAt().getTime());
      Duration duration = new Interval(createdAt, DateTime.now()).toDuration();
      return String.valueOf(duration.getStandardSeconds());
    }
    return "<< process is not timestamped >>";
  }
}
