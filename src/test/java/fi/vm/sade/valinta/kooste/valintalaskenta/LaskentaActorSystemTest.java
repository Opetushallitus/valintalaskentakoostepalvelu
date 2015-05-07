package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.TypedActor;
import akka.actor.TypedActorExtension;
import akka.actor.TypedProps;
import akka.japi.Creator;

import com.typesafe.config.ConfigFactory;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaSupervisor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaSupervisorActorImpl;

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
	private final ActorSystem actorSystem = ActorSystem.create("ValintalaskentaActorSystem", ConfigFactory.defaultOverrides());

	@Test
	public void testaaActorSupervisor() throws InterruptedException {
		LaskentaSupervisorActorImpl laskentaSupervisor = new LaskentaSupervisorActorImpl(actorSystem);

		LOG.info("Ajossa olevat laskennat nyt {}", laskentaSupervisor.ajossaOlevatLaskennat());

		laskentaSupervisor.luoJaKaynnistaLaskenta(UUID, HAKUOID, false, create(laskentaSupervisor));

		LOG.info("Ajossa olevat laskennat nyt {}", laskentaSupervisor.ajossaOlevatLaskennat());
		laskentaSupervisor.valmis(UUID);
		
	}

	private LaskentaActor create(final LaskentaSupervisor laskentaSupervisor) {
		return new LaskentaActor() {
			@Override
			public void postStop() {}
			private Thread t;
			private volatile boolean valmis = false;
			private AtomicReference<ActorRef> refinery = new AtomicReference<>();

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
