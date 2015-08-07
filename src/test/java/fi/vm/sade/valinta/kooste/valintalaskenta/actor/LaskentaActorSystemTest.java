package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaStartParams;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
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
    private final TarjontaAsyncResource tarjontaAsyncResource;
    private final LaskentaActorFactory laskentaActorFactory;
    private final LaskentaStarter LaskentaStarter;

    private final OhjausparametritAsyncResource ohjausparametritAsyncResource;

    public LaskentaActorSystemTest() {
        this.seurantaAsyncResource = mock(LaskentaSeurantaAsyncResource.class);
        this.valintaperusteetAsyncResource = mock(ValintaperusteetAsyncResource.class);
        this.valintalaskentaAsyncResource = mock(ValintalaskentaAsyncResource.class);
        this.applicationAsyncResource = mock(ApplicationAsyncResource.class);
        this.suoritusrekisteriAsyncResource = mock(SuoritusrekisteriAsyncResource.class);
        this.ohjausparametritAsyncResource = mock(OhjausparametritAsyncResource.class);
        this.tarjontaAsyncResource = mock(TarjontaAsyncResource.class);
        this.laskentaActorFactory = spy(new LaskentaActorFactory(5, valintalaskentaAsyncResource, applicationAsyncResource, valintaperusteetAsyncResource, seurantaAsyncResource, suoritusrekisteriAsyncResource));
        this.LaskentaStarter = spy(new LaskentaStarter(ohjausparametritAsyncResource, valintaperusteetAsyncResource, seurantaAsyncResource, tarjontaAsyncResource));
        laskentaActorSystem = spy(new LaskentaActorSystem(seurantaAsyncResource, LaskentaStarter, laskentaActorFactory, 8));
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
        doAnswer(invocation -> create(((LaskentaActorParams) invocation.getArguments()[2]).getUuid(), laskentaActorSystem)
        ).when(laskentaActorFactory).createLaskentaActor(any(), any(), any());
        doAnswer(invocation -> {
            String uuid = (String) invocation.getArguments()[1];
            LaskentaStartParams laskentaStartParams = new LaskentaStartParams(uuid, HAKUOID, false, 0, false, new ArrayList<>(), LaskentaTyyppi.HAKUKOHDE);
            ((BiConsumer<HakuV1RDTO, LaskentaActorParams>) invocation.getArguments()[2]).accept(new HakuV1RDTO(), new LaskentaActorParams(laskentaStartParams, null));
            return null;
        }).when(LaskentaStarter).fetchLaskentaParams(any(), any(), any());

        final Object signal = new Object();

        doAnswer(invocation -> {
            synchronized (signal) {
                signal.notify();
            }
            return null;
        }).when(laskentaActorSystem).ready("1.2.3");

        
        laskentaActorSystem.workAvailable();
        synchronized (signal) {
            signal.wait(10000);
        }
        verify(laskentaActorSystem).ready("1.2.1");
        verify(laskentaActorSystem).ready("1.2.2");
        verify(laskentaActorSystem).ready("1.2.3");
    }

    @Test
    public void testaaActorSupervisor() throws Exception {
        LOG.info("Ajossa olevat laskennat nyt {}", laskentaActorSystem.runningLaskentas());
        laskentaActorSystem.startLaskentaActor(
                new LaskentaStartParams(UUID, HAKUOID, false, null, false, null, null),
                create(UUID, laskentaActorSystem)
        );
    }

    private LaskentaActor create(final String laskentaUuid, final LaskentaSupervisor supervisor) {
        return new LaskentaActor() {

            final String uuid = laskentaUuid;

            @Override
            public void postStop() {
            }

            private volatile boolean valmis = false;
            private LaskentaSupervisor laskentaSupervisor = supervisor;

            public void start() {
                AtomicInteger c = new AtomicInteger(3);
                final Consumer<Void> takaisinkutsu = k -> {
                    if (0 == c.decrementAndGet()) {
                        valmis = true;
                        LOG.error("Actor: Valmis!");
                        laskentaSupervisor.ready(getUuid());
                    }
                };
                new Thread(() -> {
                    while (!valmis) {
                        try {
                            Thread.sleep(50L);
                        } catch (InterruptedException ignored) {
                        }
                        LOG.error("Thread: kutsutaan Actoria!");
                        takaisinkutsu.accept(null);
                    }
                }).start();
            }

            public void lopeta() {
                LOG.error("Actor: Lopeta!");
                supervisor.ready(getUuid());
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
