package fi.vm.sade.valinta.kooste.valintalaskenta;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorSystem;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 * 
 */
public class ValintalaskentaTesti {
	private final static Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaTesti.class);

	@Test
	public void testaaValintalaskentaa() {
		List<HakukohdeJaOrganisaatio> hakukohdeOids = Arrays.asList(
				new HakukohdeJaOrganisaatio("h1", "o1"),
				new HakukohdeJaOrganisaatio("h2", "o2"),
				new HakukohdeJaOrganisaatio("h3", "o3"));
		String uuid = "uuid";
		String hakuOid = "hakuOid";
		LaskentaSeurantaAsyncResource seurantaAsyncResource = createMockLaskentaSeurantaAsyncResource();
		ValintaperusteetAsyncResource valintaperusteetAsyncResource = createMockValintaperusteetAsyncResource();
		ValintalaskentaAsyncResource valintalaskentaAsyncResource = createMockValintalaskentaAsyncResource();
		ApplicationAsyncResource applicationAsyncResource = createMockApplicationAsyncResource();
		SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = createMockSuoritusrekisteriAsyncResource();
		LaskentaActorSystem laskentaActorSystem = new LaskentaActorSystem(
				seurantaAsyncResource, valintaperusteetAsyncResource,
				valintalaskentaAsyncResource, applicationAsyncResource,
				suoritusrekisteriAsyncResource);

		ValintalaskentaKerrallaRoute valintalaskentaKerrallaRoute = laskentaActorSystem;
		LaskentaAloitus laskentaJaHaku = new LaskentaAloitus(uuid, hakuOid,
				null, null, hakukohdeOids, LaskentaTyyppi.HAKUKOHDE);
		valintalaskentaKerrallaRoute
				.suoritaValintalaskentaKerralla(laskentaJaHaku);
	}

	public static LaskentaSeurantaAsyncResource createMockLaskentaSeurantaAsyncResource() {
		LaskentaSeurantaAsyncResource asyncResource = mock(LaskentaSeurantaAsyncResource.class);

		Mockito.doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				LOG.error("Laskennan seurantaa kutsuttiin hakukohteen merkkaukselle");
				return null;
			}
		})
				.when(asyncResource)
				.merkkaaHakukohteenTila(anyString(), anyString(),
						any(HakukohdeTila.class));
		Mockito.doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				LOG.error("Laskennan seurantaa kutsuttiin hakukohteen merkkaukselle");
				return null;
			}
		}).when(asyncResource)
				.merkkaaLaskennanTila(anyString(), any(LaskentaTila.class));

		return asyncResource;
	}

	public static ValintaperusteetAsyncResource createMockValintaperusteetAsyncResource() {
		ValintaperusteetAsyncResource valintaperusteetAsyncResource = mock(ValintaperusteetAsyncResource.class);
		when(
				valintaperusteetAsyncResource.haeHakijaryhmat(anyString(),
						any(), any())).then(new Answer<Peruutettava>() {
			@SuppressWarnings("unchecked")
			@Override
			public Peruutettava answer(InvocationOnMock invocation)
					throws Throwable {
				LOG.info("Palvelukutsun I/O:n tekeva funktio!");
				Consumer<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmatCallback = (Consumer<List<ValintaperusteetHakijaryhmaDTO>>) invocation
						.getArguments()[1];
				hakijaryhmatCallback.accept(Collections.emptyList());
				return TyhjaPeruutettava.tyhjaPeruutettava();
			}
		});
		return valintaperusteetAsyncResource;
	}

	public static ValintalaskentaAsyncResource createMockValintalaskentaAsyncResource() {
		ValintalaskentaAsyncResource asyncResource = mock(ValintalaskentaAsyncResource.class);
		// when(asyncResource.merkkaaHakukohteenTila(anyString(), anyString(),
		// any(HakukohdeTila.class));

		// then(new Answer<Void>() {
		// @SuppressWarnings("unchecked")
		// @Override
		// public Void answer(InvocationOnMock invocation) throws Throwable {
		// LOG.info("Palvelukutsun I/O:n tekeva funktio!");
		// Consumer<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmatCallback =
		// (Consumer<List<ValintaperusteetHakijaryhmaDTO>>) invocation
		// .getArguments()[1];
		// hakijaryhmatCallback.accept(Collections.emptyList());
		// return null;
		// }
		// });
		return asyncResource;
	}

	public static SuoritusrekisteriAsyncResource createMockSuoritusrekisteriAsyncResource() {
		SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = mock(SuoritusrekisteriAsyncResource.class);
		return suoritusrekisteriAsyncResource;
	}

	public static ApplicationAsyncResource createMockApplicationAsyncResource() {
		ApplicationAsyncResource asyncResource = mock(ApplicationAsyncResource.class);
		// when(asyncResource.merkkaaHakukohteenTila(anyString(), anyString(),
		// any(HakukohdeTila.class));

		// then(new Answer<Void>() {
		// @SuppressWarnings("unchecked")
		// @Override
		// public Void answer(InvocationOnMock invocation) throws Throwable {
		// LOG.info("Palvelukutsun I/O:n tekeva funktio!");
		// Consumer<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmatCallback =
		// (Consumer<List<ValintaperusteetHakijaryhmaDTO>>) invocation
		// .getArguments()[1];
		// hakijaryhmatCallback.accept(Collections.emptyList());
		// return null;
		// }
		// });
		return asyncResource;
	}
}
