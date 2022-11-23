package fi.vm.sade.valinta.kooste.kela;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatuloksenTila;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.KansalaisuusDto;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.HakutoiveDto;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Valintatila;
import fi.vm.sade.valinta.kooste.kela.dto.KelaCache;
import fi.vm.sade.valinta.kooste.kela.dto.KelaLuonti;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.HaunTyyppiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaDokumentinLuontiKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaHakijaRiviKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.LinjakoodiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.OppilaitosKomponentti;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteImpl;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import io.reactivex.Observable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Jussi Jartamo */
public class KelaRouteTest {

  private final Logger LOG = LoggerFactory.getLogger(KelaRouteTest.class);
  private final TarjontaAsyncResource tarjontaAsyncResource =
      Mockito.mock(TarjontaAsyncResource.class);
  private final DokumenttiAsyncResource dokumenttiAsyncResource =
      Mockito.mock(DokumenttiAsyncResource.class);
  private final KelaHakijaRiviKomponenttiImpl hkRivi =
      Mockito.mock(KelaHakijaRiviKomponenttiImpl.class);
  private final KelaDokumentinLuontiKomponenttiImpl dkRivi =
      Mockito.mock(KelaDokumentinLuontiKomponenttiImpl.class);
  private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource =
      Mockito.mock(ValintaTulosServiceAsyncResource.class);
  private final HaunTyyppiKomponentti haunTyyppiKomponentti =
      Mockito.mock(HaunTyyppiKomponentti.class);
  private final ApplicationResource applicationResource = Mockito.mock(ApplicationResource.class);
  private final OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource =
      Mockito.mock(OppijanumerorekisteriAsyncResource.class);
  private final OppilaitosKomponentti oppilaitosKomponentti =
      Mockito.mock(OppilaitosKomponentti.class);
  private final LinjakoodiKomponentti linjakoodiKomponentti =
      Mockito.mock(LinjakoodiKomponentti.class);

  private final String HAKU1 = "HAKU1OID";
  private final String HAKU2 = "HAKU2OID";
  private final String HAKUKOHDE1 = "HAKUKOHDE1";
  private final String UUID = "uuid";
  private final String HAKEMUS1 = "HAKEMUS1";
  private final String HAKIJAOID1 = "HAKIJAOID1";
  private final String DIRECT_KELA = "direct:kela";

  private TarjontaHakukohde createHakukohdeDTO() {
    return new TarjontaHakukohde(
        HAKUKOHDE1,
        null,
        new HashMap<>(),
        null,
        Collections.emptySet(),
        new HashSet<>(),
        null,
        new HashSet<>(),
        null,
        Collections.emptySet(),
        null);
  }

  private List<ValintaTulosServiceDto> createHakijat() {
    ValintaTulosServiceDto vts = new ValintaTulosServiceDto();
    vts.setHakemusOid(HAKEMUS1);
    vts.setHakijaOid(HAKIJAOID1);
    HakutoiveDto ht = new HakutoiveDto();
    ht.setHakukohdeOid(HAKUKOHDE1);
    ht.setValintatila(Valintatila.HYVAKSYTTY);
    ht.setVastaanottotila(ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI);
    vts.setHakutoiveet(List.of(ht));
    return List.of(vts);
  }

  @Test
  public void kelaLuonninTestaus() {
    Koodi suomiKoodi = new Koodi();
    suomiKoodi.setKoodiArvo("FIN");
    Koodi saintMartinKoodi = new Koodi();
    saintMartinKoodi.setKoodiArvo("MAF");

    HenkiloPerustietoDto onrHenkilo = new HenkiloPerustietoDto();
    onrHenkilo.setOidHenkilo(HAKIJAOID1);
    onrHenkilo.setEtunimet("Feliks Esaias");
    onrHenkilo.setSukunimi("Pakarinen");

    KansalaisuusDto kansalaisuus = new KansalaisuusDto();
    kansalaisuus.setKansalaisuusKoodi("246");
    onrHenkilo.setKansalaisuus(Sets.newHashSet(kansalaisuus));
    Map<String, HenkiloPerustietoDto> henkiloResponse = new HashMap<>();
    henkiloResponse.put(HAKIJAOID1, onrHenkilo);

    Mockito.when(oppijanumerorekisteriAsyncResource.haeHenkilot(Mockito.anyList()))
      .thenReturn(CompletableFuture.completedFuture(henkiloResponse));
    Mockito.when(valintaTulosServiceAsyncResource.getHaunValintatulokset(Mockito.anyString()))
        .thenReturn(Observable.just(createHakijat()));
    Mockito.when(valintaTulosServiceAsyncResource
      .getMuutoshistoria(Mockito.anyString(), Mockito.any()))
      .thenReturn(Observable.just(List.of()));

    Mockito.when(tarjontaAsyncResource.haeHakukohde(Mockito.anyString()))
        .thenReturn(CompletableFuture.completedFuture(createHakukohdeDTO()));
    Mockito.when(tarjontaAsyncResource.haeHaku(Mockito.anyString()))
        .then(
            (Answer<CompletableFuture<Haku>>)
                invocation -> {
                  String s = invocation.getArguments()[0].toString();
                  LOG.error("Tarjonnasta haku {}", s);
                  return CompletableFuture.completedFuture(createHaku(s));
                });
    Mockito.when(haunTyyppiKomponentti.haunTyyppi(Mockito.anyString()))
        .then(
          (Answer<String>) invocation -> {
            LOG.error("Koodistosta haulle {} tyyppi!", invocation.getArguments()[0]);
            return "03";
          });

    Mockito.when(
            applicationResource.findApplications(
                Mockito.anyString(),
                Mockito.anyListOf(String.class),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(), // hakuOid,
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyInt()))
        .then(
          (Answer<HakemusList>) invocation -> {
            LOG.error("Hakemuslistausta haulle!");
            return createHakemusList();
          });
    Mockito.when(applicationResource.getApplicationsByOids(Mockito.anyListOf(String.class)))
        .then(
          (Answer<List<Hakemus>>) invocation -> {
            LOG.error("Hakemuslistausta haulle!");
            return createHakemukset();
          });
    Collection<String> hakuOids = Arrays.asList(HAKU1, HAKU2);
    KelaProsessi kelaProsessi = new KelaProsessi("luonti", hakuOids);
    KelaLuonti kelaLuonti =
        new KelaLuonti(
            UUID,
            hakuOids,
            StringUtils.EMPTY,
            StringUtils.EMPTY,
            new KelaCache(tarjontaAsyncResource),
            kelaProsessi);
    KelaRouteImpl kelaRoute = createRouteBuilder();
    try {
      kelaRoute.aloitaKelaLuonti(new KelaProsessi("", List.of()), kelaLuonti);
    } catch (RuntimeException e) {
      Assert.assertTrue("Expect ei valittuja hakijoita!",
        e.getMessage().startsWith("Kela-dokumenttia ei voi luoda hauille joissa ei ole yhtään valittua hakijaa"));
    }
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

  protected KelaRouteImpl createRouteBuilder() {
    return new KelaRouteImpl(
        dokumenttiAsyncResource,
        hkRivi,
        dkRivi,
        tarjontaAsyncResource,
        haunTyyppiKomponentti,
        oppijanumerorekisteriAsyncResource,
        oppilaitosKomponentti,
        linjakoodiKomponentti,
        valintaTulosServiceAsyncResource,
        null);
  }

  private Haku createHaku(String oid) {
    return new Haku(
        oid, new HashMap<>(), new HashSet<>(), null, "03", "kausi_s", 2022, "ALKKAUSIURI", 2022);
  }
}
