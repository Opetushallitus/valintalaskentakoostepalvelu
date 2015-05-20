package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import com.typesafe.config.ConfigFactory;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaSupervisor;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class LaskennanKaynnistajaActorTest {

    private static final int MAX_WORKER_COUNT = 10;
    private final LaskentaSupervisor laskentaSupervisor = mock(LaskentaSupervisor.class);
    private LaskennanKaynnistajaActor actor;
    private TestActorRef<LaskennanKaynnistajaActor> ref;

    @Before
    public void setUp() {
        Props props = LaskennanKaynnistajaActor.props(laskentaSupervisor, MAX_WORKER_COUNT);
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
        verify(laskentaSupervisor, times(1)).haeJaKaynnistaLaskenta();
    }

    @Test
    public void maxWorkersCanBeInitialized() throws Exception {
        IntStream.rangeClosed(1, MAX_WORKER_COUNT).forEach(i -> ref.tell(new WorkAvailable(), ActorRef.noSender()));
        assertEquals(MAX_WORKER_COUNT, actor.getWorkerCount());
        verify(laskentaSupervisor, times(MAX_WORKER_COUNT)).haeJaKaynnistaLaskenta();
    }

    @Test
    public void maxWorkersCanNotBeExceeded() throws Exception {
        IntStream.rangeClosed(1, MAX_WORKER_COUNT + 1).forEach(i -> ref.tell(new WorkAvailable(), ActorRef.noSender()));
        assertEquals(MAX_WORKER_COUNT, actor.getWorkerCount());
        verify(laskentaSupervisor, times(MAX_WORKER_COUNT)).haeJaKaynnistaLaskenta();
    }

    @Test
    public void startLaskentaWhenWorkerAvailable() {
        IntStream.rangeClosed(1, MAX_WORKER_COUNT + 1).forEach(i -> ref.tell(new WorkAvailable(), ActorRef.noSender()));
        ref.tell(new WorkerAvailable(), ActorRef.noSender());
        assertEquals(10, actor.getWorkerCount());
        verify(laskentaSupervisor, times(MAX_WORKER_COUNT + 1)).haeJaKaynnistaLaskenta();
    }

    @Test
    public void workersAreReleasedWhenNoWorkAvailable() {
        ref.tell(new WorkAvailable(), ActorRef.noSender());
        ref.tell(new NoWorkAvailable(), ActorRef.noSender());
        assertEquals(0, actor.getWorkerCount());
        verify(laskentaSupervisor, times(1)).haeJaKaynnistaLaskenta();
    }

    @Test
    public void workerCountCanNotBeNegative() {
        ref.tell(new NoWorkAvailable(), ActorRef.noSender());
        assertEquals(0, actor.getWorkerCount());
        verify(laskentaSupervisor, never()).haeJaKaynnistaLaskenta();
    }
}
