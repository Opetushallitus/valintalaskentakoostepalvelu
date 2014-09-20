package fi.vm.sade.valinta.kooste.valintalaskenta;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.PalvelukutsunUudelleenAktivointiPoikkeus;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.AbstraktiPalvelukutsuStrategia;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuStrategia;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.YksiPalvelukutsuKerrallaPalvelukutsuStrategia;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class PalvelukutsuStrategiaTesti {

	private final static Logger LOG = LoggerFactory
			.getLogger(PalvelukutsuStrategiaTesti.class);

	private final String HAKUKOHDE_OID1 = "hakukohde.oid1";
	private final String HAKUKOHDE_OID2 = "hakukohde.oid2";

	@Test
	public void palvelukutsustrategiaKaynnistaaPalvelukutsunTesti() {
		ValintaperusteetAsyncResource valintaperusteetAsyncResource = createMock();
		PalvelukutsuStrategia palvelukutsuStrategia = new YksiPalvelukutsuKerrallaPalvelukutsuStrategia();

		final Palvelukutsu pk = new HakijaryhmatPalvelukutsu(HAKUKOHDE_OID1,
				valintaperusteetAsyncResource);

		Consumer<Palvelukutsu> takaisinkutsu = mock(new Consumer<Palvelukutsu>() {
			public void accept(Palvelukutsu t) {
				LOG.info("Takaisinkutsu resurssin tarvitsijalle.");
			}
		}.getClass());

		doCallRealMethod().when(takaisinkutsu).accept(any());

		palvelukutsuStrategia.laitaPalvelukutsuJonoon(pk, takaisinkutsu);
		palvelukutsuStrategia.aloitaUusiPalvelukutsu();
		palvelukutsuStrategia.laitaPalvelukutsuJonoon(pk, takaisinkutsu);
		try {
			palvelukutsuStrategia.aloitaUusiPalvelukutsu();
			Assert.fail("Sama palvelukutsu uudestaan tyojonossa mutta palvelukutsun aloitus onnistui!");
		} catch (PalvelukutsunUudelleenAktivointiPoikkeus e) {

		}
		verify(takaisinkutsu, only()).accept(any());
	}

	@Test
	public void palvelukutsustrategiaValmiitTyotTyhjeneeTyoJonostaTesti() {
		ValintaperusteetAsyncResource valintaperusteetAsyncResource = createMock();
		AbstraktiPalvelukutsuStrategia palvelukutsuStrategia = new YksiPalvelukutsuKerrallaPalvelukutsuStrategia();

		final Palvelukutsu pk1 = new HakijaryhmatPalvelukutsu(HAKUKOHDE_OID1,
				valintaperusteetAsyncResource);
		final Palvelukutsu pk2 = new HakijaryhmatPalvelukutsu(HAKUKOHDE_OID2,
				valintaperusteetAsyncResource);
		Consumer<Palvelukutsu> takaisinkutsu = mock(new Consumer<Palvelukutsu>() {
			public void accept(Palvelukutsu t) {
				LOG.info("Takaisinkutsu resurssin tarvitsijalle.");
			}
		}.getClass());

		doCallRealMethod().when(takaisinkutsu).accept(any());

		Assert.assertTrue(palvelukutsuStrategia.getAloitetutPalvelukutsut()
				.isEmpty()
				&& palvelukutsuStrategia.getPalvelukutsuJono().isEmpty());
		palvelukutsuStrategia.laitaPalvelukutsuJonoon(pk1, takaisinkutsu);
		Assert.assertTrue(palvelukutsuStrategia.getAloitetutPalvelukutsut()
				.isEmpty()
				&& palvelukutsuStrategia.getPalvelukutsuJono().size() == 1);
		palvelukutsuStrategia.laitaPalvelukutsuJonoon(pk2, takaisinkutsu);
		Assert.assertTrue(palvelukutsuStrategia.getAloitetutPalvelukutsut()
				.isEmpty()
				&& palvelukutsuStrategia.getPalvelukutsuJono().size() == 2);
		palvelukutsuStrategia.aloitaUusiPalvelukutsu();
		Assert.assertTrue(
				"Ensiksi laitettu PK1 viela tyojonossa vaikka tyojonon pitaisi olla FIFO!",
				palvelukutsuStrategia.getPalvelukutsuJono().stream()
						.noneMatch(tk -> tk.getPalvelukutsu().equals(pk1)));
		Assert.assertTrue(
				"Viimeiseksi laitettu PK2 ei ole tyojonossa vaikka tyojonon pitaisi olla FIFO!",
				palvelukutsuStrategia.getPalvelukutsuJono().stream()
						.allMatch(tk -> tk.getPalvelukutsu().equals(pk2)));
		palvelukutsuStrategia.aloitaUusiPalvelukutsu();
		Assert.assertTrue(palvelukutsuStrategia.getAloitetutPalvelukutsut()
				.isEmpty()
				&& palvelukutsuStrategia.getPalvelukutsuJono().isEmpty());
	}

	@Test
	public void palvelukutsustrategiaEiKaynnistaPeruutettuaTyotaTesti() {
		PalvelukutsuStrategia palvelukutsuStrategia = new YksiPalvelukutsuKerrallaPalvelukutsuStrategia();
		ValintaperusteetAsyncResource valintaperusteetAsyncResource = createMock();
		final Palvelukutsu pk = new HakijaryhmatPalvelukutsu(HAKUKOHDE_OID1,
				valintaperusteetAsyncResource);
		Consumer<Palvelukutsu> takaisinkutsu = mock(new Consumer<Palvelukutsu>() {
			public void accept(Palvelukutsu t) {
				LOG.info("Takaisinkutsu resurssin tarvitsijalle.");
			}
		}.getClass());

		doCallRealMethod().when(takaisinkutsu).accept(any());
		palvelukutsuStrategia.laitaPalvelukutsuJonoon(pk, takaisinkutsu);
		pk.peruuta();
		try {
			palvelukutsuStrategia.aloitaUusiPalvelukutsu();
			Assert.fail("Sama palvelukutsu uudestaan tyojonossa mutta palvelukutsun aloitus onnistui!");
		} catch (PalvelukutsunUudelleenAktivointiPoikkeus e) {

		}
		verify(takaisinkutsu, never()).accept(any());
	}

	public static Palvelukutsu createPalvelukutsuMock() {
		final Palvelukutsu palvelukutsu = Mockito.mock(Palvelukutsu.class);
		Mockito.when(palvelukutsu.teePalvelukutsu(Mockito.any())).then(
				new Answer<Palvelukutsu>() {
					@Override
					public Palvelukutsu answer(InvocationOnMock invocation)
							throws Throwable {
						return palvelukutsu;
					}
				});
		return palvelukutsu;
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
				hakijaryhmatCallback.accept(Collections.emptyList());
				return TyhjaPeruutettava.tyhjaPeruutettava();
			}
		});
		return valintaperusteetAsyncResource;
	}
}
