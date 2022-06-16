package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import static fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskentaStarterActor.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import com.typesafe.config.ConfigFactory;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaSupervisor;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;

public class LaskentaStarterActorTest {

  private static final int MAX_WORKER_COUNT = 10;
  private final LaskentaSupervisor laskentaSupervisor = mock(LaskentaSupervisor.class);
  private LaskentaStarterActor actor;
  private TestActorRef<LaskentaStarterActor> ref;

  @Before
  public void setUp() {
    Props props = props(laskentaSupervisor, MAX_WORKER_COUNT);
    ActorSystem actorSystem = ActorSystem.create("ValintalaskentaActorSystem", ConfigFactory.defaultOverrides());
    this.ref = TestActorRef.create(actorSystem, props, "testA");
    this.actor = ref.underlyingActor();
  }

  @Test
  public void createdActorHasNoWorkers() {
    assertEquals(0, actor.getWorkerCount());
  }

  @Test
  public void workerProvidedOnWorkAvailableMessage() throws Exception {
    ref.tell(new WorkAvailable(), ActorRef.noSender());
    assertEquals(1, actor.getWorkerCount());
    verify(laskentaSupervisor, times(1)).fetchAndStartLaskenta();
  }

  @Test
  public void maxWorkersCanBeInitialized() throws Exception {
    signalWorkAvailableTimes(MAX_WORKER_COUNT);
    assertEquals(MAX_WORKER_COUNT, actor.getWorkerCount());
    verify(laskentaSupervisor, times(MAX_WORKER_COUNT)).fetchAndStartLaskenta();
  }

  @Test
  public void maxWorkersCanNotBeExceeded() throws Exception {
    signalWorkAvailableTimes(MAX_WORKER_COUNT + 1);
    assertEquals(MAX_WORKER_COUNT, actor.getWorkerCount());
    verify(laskentaSupervisor, times(MAX_WORKER_COUNT)).fetchAndStartLaskenta();
  }

  @Test
  public void startLaskentaWhenWorkerAvailable() {
    signalWorkAvailableTimes(MAX_WORKER_COUNT + 1);
    ref.tell(new WorkerAvailable(), ActorRef.noSender());
    assertEquals(10, actor.getWorkerCount());
    verify(laskentaSupervisor, times(MAX_WORKER_COUNT + 1)).fetchAndStartLaskenta();
  }

  @Test
  public void workersAreReleasedWhenNoWorkAvailable() {
    ref.tell(new WorkAvailable(), ActorRef.noSender());
    ref.tell(new NoWorkAvailable(), ActorRef.noSender());
    assertEquals(0, actor.getWorkerCount());
    verify(laskentaSupervisor, times(1)).fetchAndStartLaskenta();
  }

  @Test
  public void workerCountCanNotBeNegative() {
    ref.tell(new NoWorkAvailable(), ActorRef.noSender());
    assertEquals(0, actor.getWorkerCount());
    verify(laskentaSupervisor, never()).fetchAndStartLaskenta();
  }

  private void signalWorkAvailableTimes(int count) {
    IntStream.rangeClosed(1, count).forEach(i -> ref.tell(new WorkAvailable(), ActorRef.noSender()));
  }
}
