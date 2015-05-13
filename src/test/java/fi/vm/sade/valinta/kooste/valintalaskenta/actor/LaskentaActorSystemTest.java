package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import akka.actor.*;
import com.typesafe.config.ConfigFactory;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

/**
 * 
 * @author Jussi Jartamo
 * 
 * 
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

    private final ActorSystem actorSystem = ActorSystem.create("ValintalaskentaActorSystem", ConfigFactory.defaultOverrides());


    public LaskentaActorSystemTest(){
        this.seurantaAsyncResource = mock(LaskentaSeurantaAsyncResource.class);
        this.valintaperusteetAsyncResource= mock(ValintaperusteetAsyncResource.class);
        this.valintalaskentaAsyncResource = mock(ValintalaskentaAsyncResource.class);
        this.applicationAsyncResource = mock(ApplicationAsyncResource.class);
        this.suoritusrekisteriAsyncResource = mock(SuoritusrekisteriAsyncResource.class);
        LaskentaActorFactory laskentaActorFactory = new LaskentaActorFactory(valintalaskentaAsyncResource,applicationAsyncResource,valintaperusteetAsyncResource,seurantaAsyncResource,suoritusrekisteriAsyncResource);
        laskentaActorSystem = new LaskentaActorSystem(laskentaActorFactory);
    }

    @After
    public void resetMocks(){
        reset(seurantaAsyncResource, valintaperusteetAsyncResource, valintalaskentaAsyncResource, applicationAsyncResource, suoritusrekisteriAsyncResource);
    }

	@Test
	public void testaaActorSupervisor() throws Exception {
		LOG.info("Ajossa olevat laskennat nyt {}", laskentaActorSystem.ajossaOlevatLaskennat());
        laskentaActorSystem.luoJaKaynnistaLaskenta(UUID, HAKUOID, false, create(laskentaActorSystem));

		LOG.info("Ajossa olevat laskennat nyt {}", laskentaActorSystem.ajossaOlevatLaskennat());
        laskentaActorSystem.valmis(UUID);
	}

	private LaskentaActor create(final LaskentaSupervisor supervisor) {
		return new LaskentaActor() {

			@Override
			public void postStop() {}
			private Thread t;
			private volatile boolean valmis = false;
			private AtomicReference<ActorRef> refinery = new AtomicReference<>();
            private LaskentaSupervisor laskentaSupervisor = supervisor;

			public void aloita() {
				{
					ActorContext context = TypedActor.context();
					LOG.error("Actor: Aloitetaan! {}", context);
					LOG.error("Actor: Self! {}", context.self());
					refinery.set(context.self());
				}
				AtomicInteger c = new AtomicInteger(3);
				final Consumer<Void> takaisinkutsu = k -> {
					if (0 == c.decrementAndGet()) {
						valmis = true;
						LOG.error("Actor: Valmis!");
						LOG.error("Actor: Self! {}", refinery.get());
						laskentaSupervisor.valmis(getUuid());

						try {
							t.stop();
						} catch (Exception ignored) {}
					}
				};
				t = new Thread(() -> {
					while (true) {
						try {
							Thread.sleep(50L);
						} catch (InterruptedException ignored) {}
						LOG.error("Thread: kutsutaan Actoria!");
						takaisinkutsu.accept(null);
					}
				});
				t.start();
			}

			public void lopeta() {
				LOG.error("Actor: Lopeta!");
				refinery.get().tell(PoisonPill.getInstance(), refinery.get());
			}

			public boolean isValmis() {
				return valmis;
			}

			public String getUuid() {
				return "uuid";
			}

			public String getHakuOid() {
				LOG.error("Actor: PING!");
				return "hakuOid";
			}

		};
	}
}
