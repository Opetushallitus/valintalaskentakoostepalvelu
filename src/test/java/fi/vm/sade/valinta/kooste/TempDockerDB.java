package fi.vm.sade.valinta.kooste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TempDockerDB {

  private static final Logger LOG = LoggerFactory.getLogger(TempDockerDB.class);
  private static final String dbName = "valintalaskentakoostepalvelu";
  private static final String containerName = "valintalaskentakoostepalvelu-postgres";

  private static final int startStopRetries = 100;
  private static final int startStopRetryIntervalMillis = 100;

  private static boolean databaseIsRunning() {
    try {
      Process p =
          Runtime.getRuntime()
              .exec(
                  "docker exec "
                      + containerName
                      + " pg_isready -q -t 1 -h localhost -U oph -d "
                      + dbName);
      return p.waitFor() == 0;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  ;

  private static void startDatabaseContainer() {
    LOG.info("Starting PostgreSQL container");

    try {
      Process p =
          Runtime.getRuntime().exec("docker-compose -f postgresql/docker-compose.yml up -d");
      p.waitFor();
      if (!tryTimes(() -> databaseIsRunning(), startStopRetries, startStopRetryIntervalMillis)) {
        String error = new String(p.getErrorStream().readAllBytes());
        throw new RuntimeException("postgres not accepting connections: " + error);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void start() {
    try {
      if (!databaseIsRunning()) {
        startDatabaseContainer();
      }
    } finally {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
    }
  }

  private static void stop() {
    try {
      LOG.info("Killing PostgreSQL container");
      Process p = Runtime.getRuntime().exec("docker-compose -f postgresql/docker-compose.yml stop");
      p.waitFor();
    } catch (Exception e) {
      LOG.warn("PostgreSQL container didn't stop gracefully");
    }
  }

  private interface Function<T> {
    T apply();
  }

  private static boolean tryTimes(Function<Boolean> runnable, int times, int interval)
      throws InterruptedException {
    if (times == 0) {
      return false;
    }
    if (runnable.apply()) {
      return true;
    }
    Thread.sleep(interval);
    return tryTimes(runnable, times - 1, interval);
  }
}
