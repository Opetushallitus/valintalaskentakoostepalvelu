package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Luokka jonka avulla voi hallita lähtötietojen haun rinnakkaisuutta taustajärjestelmistä. Perustuu
 * semaforiin, ts. siihen että rinnakkaisille kutsuille on tarjolla tietty määrä lupia, joita kutsut
 * tarvitsevat. Lupia voi tarvita joko yhden per kutsu, tai sitten suuremman määrän riippuen
 * haettavan data määrästä (esim. suoritusrekisteri). Tämä luokka pitää myös kirjaa odotukseen ja
 * kutsujen suorittamiseen käytetystä ajasta jotta tämä voidaan raportoida CloudWatchiin.
 */
public class ConcurrencyLimiter {

  private static final Logger LOG = LoggerFactory.getLogger(ConcurrencyLimiter.class);

  public static final Duration ONGOING = Duration.ofMillis(Long.MIN_VALUE + 1);
  public static final Duration TIMEOUT = Duration.ofMillis(Long.MIN_VALUE + 2);
  public static final Duration ERROR = Duration.ofMillis(Long.MIN_VALUE + 3);

  private int maxPermits;
  private final String nimi;
  private final Semaphore semaphore;
  private final ExecutorService executor;
  private final AtomicInteger waiting;
  private final AtomicInteger active;

  /**
   * Luo uuden limitterin
   *
   * @param nimi limitterin nimi
   * @param executor limitterissä käytettävä {@link Executor}
   */
  public ConcurrencyLimiter(String nimi, ExecutorService executor) {
    this.nimi = nimi;
    this.maxPermits = 0;
    this.semaphore = new Semaphore(0, true);
    this.executor = executor;
    this.waiting = new AtomicInteger(0);
    this.active = new AtomicInteger(0);
  }

  /**
   * Asettaa rinnakkaisten "lupien" määrän
   *
   * @param newPermits uusi lupien määrä
   */
  public void setMaxPermits(int newPermits) {
    int dPermits = newPermits - this.maxPermits;
    this.maxPermits = newPermits;
    if (dPermits == 0) {
      return;
    }
    if (dPermits > 0) {
      LOG.info("Lisätään vaiheen " + this.nimi + " limitteriin " + dPermits + " permittiä.");
      this.semaphore.release(dPermits);
    } else {
      LOG.info("Vähennetään vaiheen " + this.nimi + " limitteristä " + -dPermits + " permittiä.");
      this.executor.submit(() -> this.semaphore.acquireUninterruptibly(-dPermits));
    }
  }

  /**
   * Palauttaa kuinka monta pyyntöä odottaa lupaa
   *
   * @return
   */
  public int getWaiting() {
    return this.waiting.get();
  }

  /**
   * Palauttaa kuinka montaa pyyntöä suoritetaan
   *
   * @return
   */
  public int getActive() {
    return this.active.get();
  }

  /**
   * Palauttaa limitterin nimen
   *
   * @return
   */
  public String getNimi() {
    return this.nimi;
  }

  public static String asDurationString(Duration duration) {
    if (duration == ONGOING) {
      return "ongoing";
    } else if (duration == TIMEOUT) {
      return "timeout";
    } else if (duration == ERROR) {
      return "error";
    }
    return duration.toMillis() + "";
  }

  /**
   * Suoritetaan ratelimitoitu pyyntö
   *
   * @param permits kuinka monta "lupaa" pyyntöön tarvitaan
   * @param waitDurations mappi johon tallennetaan odotuksen kesto
   * @param invokeDurations mappi johon tallennetaan suorituksen kesto
   * @param supplier {@link Supplier} joka palauttaa suoritettavan pyynnön
   * @return {@link CompletableFuture} joka palauttaa pyynnön paluuarvon
   */
  public <T> CompletableFuture<T> withConcurrencyLimit(
      int permits,
      Map<String, Duration> waitDurations,
      Map<String, Duration> invokeDurations,
      Supplier<CompletableFuture<T>> supplier) {

    Instant waitStart = Instant.now();
    CompletableFuture<T> future = new CompletableFuture<>();
    int requiredPermits = Math.min(this.maxPermits, permits);

    this.executor.submit(
        () -> {
          // haetaan lupa suorittaa pyyntö ja tallennetaan odottamiseen mennyt aika
          this.waiting.incrementAndGet();
          try {
            this.semaphore.acquireUninterruptibly(requiredPermits);
          } finally {
            this.waiting.decrementAndGet();
          }

          if (nimi.equals("suoritukset")) {
            LOG.info(
                "Haettiin "
                    + permits
                    + " permittiä, jäi "
                    + this.semaphore.availablePermits()
                    + " permittiä, ajossa "
                    + this.getActive()
                    + ", odottaa "
                    + this.getWaiting());
          }

          Instant invokeStart = Instant.now();
          waitDurations.put(this.nimi, Duration.between(waitStart, invokeStart));

          // suoritetaan pyyntö
          this.active.incrementAndGet();
          try {
            invokeDurations.put(this.nimi, ONGOING);
            T result = supplier.get().get();
            invokeDurations.put(this.nimi, Duration.between(invokeStart, Instant.now()));
            future.complete(result);
          } catch (ExecutionException e) {
            Throwable underlyingCause = getUnderlyingCause(e);
            if (underlyingCause instanceof TimeoutException) {
              invokeDurations.put(this.nimi, TIMEOUT);
              future.completeExceptionally(underlyingCause);
            } else {
              invokeDurations.put(this.nimi, ERROR);
              future.completeExceptionally(e);
            }
          } catch (Exception e) {
            invokeDurations.put(this.nimi, ERROR);
            future.completeExceptionally(e);
          } finally {
            semaphore.release(requiredPermits);
            this.active.decrementAndGet();
          }
        });

    return future;
  }

  private static Throwable getUnderlyingCause(Throwable t) {
    if (t.getCause() != null) {
      return getUnderlyingCause(t.getCause());
    }
    return t;
  }
}
