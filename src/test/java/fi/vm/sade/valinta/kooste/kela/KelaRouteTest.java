package fi.vm.sade.valinta.kooste.kela;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.KomotoResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.HakuV1Resource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import fi.vm.sade.valinta.kooste.kela.dto.KelaCache;
import fi.vm.sade.valinta.kooste.kela.dto.KelaLuonti;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.*;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteImpl;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class KelaRouteTest extends CamelTestSupport {

	private final Logger LOG = LoggerFactory.getLogger(KelaRouteTest.class);
	private final HakuV1Resource hakuResource = Mockito
			.mock(HakuV1Resource.class);
	private final DokumenttiResource dokumenttiResource = Mockito
			.mock(DokumenttiResource.class);
	private final KelaHakijaRiviKomponenttiImpl hkRivi = Mockito
			.mock(KelaHakijaRiviKomponenttiImpl.class);
	private final KelaDokumentinLuontiKomponenttiImpl dkRivi = Mockito
			.mock(KelaDokumentinLuontiKomponenttiImpl.class);
	private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource = Mockito
			.mock(ValintaTulosServiceAsyncResource.class);
	private final HaunTyyppiKomponentti haunTyyppiKomponentti = Mockito
			.mock(HaunTyyppiKomponentti.class);
	private final ApplicationResource applicationResource = Mockito
			.mock(ApplicationResource.class);
	private final OppilaitosKomponentti oppilaitosKomponentti = Mockito
			.mock(OppilaitosKomponentti.class);
	private final LinjakoodiKomponentti linjakoodiKomponentti = Mockito
			.mock(LinjakoodiKomponentti.class);
	private final HakukohdeResource hakukohdeResource = Mockito
			.mock(HakukohdeResource.class);
        private final KomotoResource komotoResource = Mockito
                        .mock(KomotoResource.class);

	private final String HAKU1 = "HAKU1OID";
	private final String HAKU2 = "HAKU2OID";
	private final String HAKUKOHDE1 = "HAKUKOHDE1";
	private final String UUID = "uuid";
	private final String HAKEMUS1 = "HAKEMUS1";
	private final String DIRECT_KELA = "direct:kela";
	@Produce(uri = DIRECT_KELA)
	protected ProducerTemplate template;

	private HakukohdeDTO createHakukohdeDTO() {
		HakukohdeDTO hakukohdeDTO = new HakukohdeDTO();
		hakukohdeDTO.setOid(HAKUKOHDE1);
		return hakukohdeDTO;
	}

	private List<ValintaTulosServiceDto> createHakijat() {
		ValintaTulosServiceDto vts = new ValintaTulosServiceDto();
		vts.setHakemusOid(HAKEMUS1);
		/*
		HakijaDTO h = new HakijaDTO();
		h.setEtunimi("Eero");
		h.setHakemusOid(HAKEMUS1);
		TreeSet<HakutoiveDTO> hakutoiveet = new TreeSet<HakutoiveDTO>();
		HakutoiveDTO htoive = new HakutoiveDTO();
		HakutoiveenValintatapajonoDTO jono = new HakutoiveenValintatapajonoDTO();
		jono.setTila(HakemuksenTila.HYVAKSYTTY);
		jono.setVastaanottotieto(ValintatuloksenTila.VASTAANOTTANUT);
		htoive.getHakutoiveenValintatapajonot().add(jono);
		hakutoiveet.add(htoive);
		h.setHakutoiveet(hakutoiveet);
		*/
		return Arrays.asList();
	}

	@Test
	public void kelaLuonninTestaus() {
		Mockito.when(
				valintaTulosServiceAsyncResource.getHaunValintatulokset(Mockito
						.anyString())).thenReturn(Observable.just(createHakijat()));
		Mockito.when(hakukohdeResource.getByOID(Mockito.anyString()))
				.thenReturn(createHakukohdeDTO());
		Mockito.when(hakuResource.findByOid(Mockito.anyString())).then(
				new Answer<ResultV1RDTO<HakuV1RDTO>>() {
					@Override
					public ResultV1RDTO<HakuV1RDTO> answer(
							InvocationOnMock invocation) throws Throwable {
						String s = invocation.getArguments()[0].toString();
						LOG.error("Tarjonnasta haku {}", s);
						return new ResultV1RDTO<HakuV1RDTO>(createHaku(s));
					}
				});
		Mockito.when(haunTyyppiKomponentti.haunTyyppi(Mockito.anyString()))
				.then(new Answer<String>() {
					@Override
					public String answer(InvocationOnMock invocation)
							throws Throwable {
						LOG.error("Koodistosta haulle {} tyyppi!",
								invocation.getArguments()[0]);
						return "03";
					}
				});

		Mockito.when(
				applicationResource.findApplications(Mockito.anyString(),
						Mockito.anyListOf(String.class), Mockito.anyString(),
						Mockito.anyString(), Mockito.anyString(), // hakuOid,
						Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
				.then(new Answer<HakemusList>() {
					@Override
					public HakemusList answer(InvocationOnMock invocation)
							throws Throwable {
						LOG.error("Hakemuslistausta haulle!");
						return createHakemusList();
					}
				});
		Mockito.when(
				applicationResource.getApplicationsByOids(Mockito
						.anyListOf(String.class))).then(
				new Answer<List<Hakemus>>() {
					@Override
					public List<Hakemus> answer(InvocationOnMock invocation)
							throws Throwable {
						LOG.error("Hakemuslistausta haulle!");
						return createHakemukset();
					}
				});
		Collection<String> hakuOids = Arrays.asList(HAKU1, HAKU2);
		KelaProsessi kelaProsessi = new KelaProsessi("luonti", hakuOids);
		KelaLuonti kelaLuonti = new KelaLuonti(UUID, hakuOids,
				StringUtils.EMPTY, StringUtils.EMPTY, new KelaCache(
						hakukohdeResource, komotoResource), kelaProsessi);
		template.sendBodyAndProperty(kelaLuonti,
				ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI, kelaProsessi);
	}

	private List<Hakemus> createHakemukset() {
		List<Hakemus> hakemukset = Lists.newArrayList();
		Hakemus h = new Hakemus();
		h.setOid(HAKEMUS1);
		Map<String, String> info = Maps.newHashMap();

		h.setAdditionalInfo(info);
		Answers answers = new Answers();
		answers.setHenkilotiedot(Maps.newHashMap());
		h.setAnswers(answers);
		hakemukset.add(h);
		return hakemukset;
	}

	private HakemusList createHakemusList() {
		HakemusList hakemusList = new HakemusList();
		SuppeaHakemus h0 = new SuppeaHakemus();
		h0.setOid(HAKEMUS1);
		hakemusList.getResults().add(h0);
		return hakemusList;
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new KelaRouteImpl(DIRECT_KELA, dokumenttiResource, hkRivi,
				dkRivi, hakuResource,
				haunTyyppiKomponentti, applicationResource,
				oppilaitosKomponentti, linjakoodiKomponentti,
				hakukohdeResource, valintaTulosServiceAsyncResource,
				null);
	}

	private HakuV1RDTO createHaku(String oid) {
		HakuV1RDTO h = new HakuV1RDTO();
		h.setOid(oid);
		h.setKoulutuksenAlkamiskausiUri("ALKKAUSIURI");
		h.setHakutyyppiUri("HAKUTYYPPIURI");
		return h;
	}
}
