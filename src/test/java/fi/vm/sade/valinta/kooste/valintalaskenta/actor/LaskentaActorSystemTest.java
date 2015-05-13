package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.TypedActor;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Jussi Jartamo
 */
public class LaskentaActorSystemTest {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaActorSystemTest.class);

    private final String UUID = "uuid";
    private final String HAKUOID = "hakuOid";

    private final LaskentaActorSystem laskentaActorSystem;
    private final LaskentaSeurantaAsyncResource seurantaAsyncResource;
    private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
    private final LaskentaActorFactory laskentaActorFactory;
    private final LaskentaKaynnistin LaskentaKaynnistin;

    private final OhjausparametritAsyncResource ohjausparametritAsyncResource;

    public LaskentaActorSystemTest() {
        this.seurantaAsyncResource = mock(LaskentaSeurantaAsyncResource.class);
        this.valintaperusteetAsyncResource = mock(ValintaperusteetAsyncResource.class);
        this.valintalaskentaAsyncResource = mock(ValintalaskentaAsyncResource.class);
        this.applicationAsyncResource = mock(ApplicationAsyncResource.class);
        this.suoritusrekisteriAsyncResource = mock(SuoritusrekisteriAsyncResource.class);
        this.ohjausparametritAsyncResource = mock(OhjausparametritAsyncResource.class);
        this.laskentaActorFactory = spy(new LaskentaActorFactory(valintalaskentaAsyncResource, applicationAsyncResource, valintaperusteetAsyncResource, seurantaAsyncResource, suoritusrekisteriAsyncResource));
        this.LaskentaKaynnistin = spy(new LaskentaKaynnistin(ohjausparametritAsyncResource, valintaperusteetAsyncResource, seurantaAsyncResource));
        laskentaActorSystem = spy(new LaskentaActorSystem(seurantaAsyncResource, LaskentaKaynnistin, laskentaActorFactory));
    }

    @After
    public void resetMocks() {
        reset(seurantaAsyncResource, valintaperusteetAsyncResource, valintalaskentaAsyncResource, applicationAsyncResource, suoritusrekisteriAsyncResource);
    }

    @Test
    public void testStarting() throws InterruptedException {
        final AtomicInteger count = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (count.getAndIncrement() < 3)
                ((Consumer<String>) invocation.getArguments()[0]).accept("1.2." + count.toString());
            else {
                ((Consumer<String>) invocation.getArguments()[0]).accept(null);
            }
            LOG.info("Count {}", count);
            return null;
        }).when(seurantaAsyncResource).otaSeuraavaLaskentaTyonAlle(any(), any());
        doAnswer(invocation -> {
                    return create(((LaskentaActorParams) invocation.getArguments()[1]).getUuid(), laskentaActorSystem);
                }
        ).when(laskentaActorFactory).createLaskentaActor(any(), any());
        doAnswer(invocation -> {
            String uuid = (String) invocation.getArguments()[0];
            ((Consumer<LaskentaActorParams>) invocation.getArguments()[1]).accept(new LaskentaActorParams(new LaskentaAloitus(uuid, HAKUOID, false, 0, false, new ArrayList<HakukohdeJaOrganisaatio>(), LaskentaTyyppi.HAKUKOHDE), null));
            return null;
        }).when(LaskentaKaynnistin).haeLaskentaParams(any(), any());

        final Object signal = new Object();

        doAnswer(invocation -> {
            synchronized (signal) {
                signal.notify();
            }
            return null;
        }).when(laskentaActorSystem).valmis("1.2.3");

        
        laskentaActorSystem.workAvailable();
        synchronized (signal) {
            signal.wait(10000);
        }
        verify(laskentaActorSystem).valmis("1.2.1");
        verify(laskentaActorSystem).valmis("1.2.2");
        verify(laskentaActorSystem).valmis("1.2.3");
    }

    @Test
    public void testaaActorSupervisor() throws Exception {
        LOG.info("Ajossa olevat laskennat nyt {}", laskentaActorSystem.ajossaOlevatLaskennat());
        laskentaActorSystem.luoJaKaynnistaLaskenta(UUID, HAKUOID, false, create(UUID, laskentaActorSystem));

        LOG.info("Ajossa olevat laskennat nyt {}", laskentaActorSystem.ajossaOlevatLaskennat());
        laskentaActorSystem.valmis(UUID);
    }

    private LaskentaActor create(final String laskentaUuid, final LaskentaSupervisor supervisor) {
        return new LaskentaActor() {

            final String uuid = laskentaUuid;

            @Override
            public void postStop() {
            }

            private Thread t;
            private volatile boolean valmis = false;
            private AtomicReference<ActorRef> refinery = new AtomicReference<>();
            private LaskentaSupervisor laskentaSupervisor = supervisor;

            public void aloita() {
                AtomicInteger c = new AtomicInteger(3);
                final Consumer<Void> takaisinkutsu = k -> {
                    if (0 == c.decrementAndGet()) {
                        valmis = true;
                        LOG.error("Actor: Valmis!");
                        LOG.error("Actor: Self! {}", refinery.get());
                        laskentaSupervisor.valmis(getUuid());

                        try {
                            t.stop();
                        } catch (Exception ignored) {
                        }
                    }
                };
                t = new Thread(() -> {
                    while (true) {
                        try {
                            Thread.sleep(50L);
                        } catch (InterruptedException ignored) {
                        }
                        LOG.error("Thread: kutsutaan Actoria!");
                        takaisinkutsu.accept(null);
                    }
                });
                t.start();
            }

            public void lopeta() {
                LOG.error("Actor: Lopeta!");
                supervisor.valmis(getUuid());
            }

            public boolean isValmis() {
                return valmis;
            }

            public String getUuid() {
                return uuid;
            }

            public String getHakuOid() {
                LOG.error("Actor: PING!");
                return "hakuOid";
            }

        };
    }
}
