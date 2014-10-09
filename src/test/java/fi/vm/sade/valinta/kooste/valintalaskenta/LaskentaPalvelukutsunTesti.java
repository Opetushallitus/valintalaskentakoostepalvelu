package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;
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

import com.google.common.collect.Lists;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.AbstraktiLaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintakoelaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategia;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategiaImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuStrategia;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.YksiPalvelukutsuKerrallaPalvelukutsuStrategia;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class LaskentaPalvelukutsunTesti {
	private final static Logger LOG = LoggerFactory
			.getLogger(LaskentaPalvelukutsunTesti.class);
	private static final HakukohdeJaOrganisaatio HAKUKOHDE_OID = new HakukohdeJaOrganisaatio(
			"hk.oid", "org.oid");

	@Test
	public void testaaLaskentaPalvelukutsunSuoritusta() {
		ValintaperusteetAsyncResource valintaperusteetAsyncResource = createMock();

		Palvelukutsu hakijaryhmat = new HakijaryhmatPalvelukutsu(HAKUKOHDE_OID,
				valintaperusteetAsyncResource);
		Palvelukutsu valintaperusteet = new HakijaryhmatPalvelukutsu(
				HAKUKOHDE_OID, valintaperusteetAsyncResource);
		Palvelukutsu hakemukset = new HakijaryhmatPalvelukutsu(HAKUKOHDE_OID,
				valintaperusteetAsyncResource);
		Palvelukutsu lisatiedot = new HakijaryhmatPalvelukutsu(HAKUKOHDE_OID,
				valintaperusteetAsyncResource);

		PalvelukutsuStrategia p1 = new YksiPalvelukutsuKerrallaPalvelukutsuStrategia();
		PalvelukutsuStrategia p2 = new YksiPalvelukutsuKerrallaPalvelukutsuStrategia();
		PalvelukutsuStrategia p3 = new YksiPalvelukutsuKerrallaPalvelukutsuStrategia();
		PalvelukutsuStrategia p4 = new YksiPalvelukutsuKerrallaPalvelukutsuStrategia();
		Collection<PalvelukutsuStrategia> strategiat = Arrays.asList(p1, p2,
				p3, p4);

		Collection<PalvelukutsuJaPalvelukutsuStrategia> palvelukutsut = Arrays
				.asList(new PalvelukutsuJaPalvelukutsuStrategiaImpl(
						hakijaryhmat, p1),
						new PalvelukutsuJaPalvelukutsuStrategiaImpl(
								valintaperusteet, p2),
						new PalvelukutsuJaPalvelukutsuStrategiaImpl(hakemukset,
								p3),
						new PalvelukutsuJaPalvelukutsuStrategiaImpl(lisatiedot,
								p4));

		LaskentaPalvelukutsu laskentaPalvelukutsu = new AbstraktiLaskentaPalvelukutsu(
				HAKUKOHDE_OID.getHakukohdeOid(), palvelukutsut) {
			@Override
			public void vapautaResurssit() {
				// TODO Auto-generated method stub

			}

			@Override
			public Palvelukutsu teePalvelukutsu(
					Consumer<Palvelukutsu> takaisinkutsu) {
				return null;
			}

		};

		laskentaPalvelukutsu.laitaTyojonoon(pk -> {
			LOG.info("Laskenta on valmis!");
		});
		strategiat.forEach(ps -> ps.aloitaUusiPalvelukutsu());

		Assert.assertTrue("Laskentapalvelukutsu ei saisi olla onnistunut",
				laskentaPalvelukutsu.onkoPeruutettu());
	}

	public static ValintaperusteetAsyncResource createMock() {
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
				Consumer<Throwable> poikkeusCallback = (Consumer<Throwable>) invocation
						.getArguments()[2];
				poikkeusCallback.accept(new RuntimeException());
				return TyhjaPeruutettava.tyhjaPeruutettava();
			}
		});
		return valintaperusteetAsyncResource;
	}
}
