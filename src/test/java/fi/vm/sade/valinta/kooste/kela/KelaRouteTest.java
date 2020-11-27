package fi.vm.sade.valinta.kooste.kela;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Hakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
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

/** @author Jussi Jartamo */
public class KelaRouteTest extends CamelTestSupport {

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
  private final String DIRECT_KELA = "direct:kela";

  @Produce(uri = DIRECT_KELA)
  protected ProducerTemplate template;

  private Hakukohde createHakukohdeDTO() {
    return new Hakukohde(
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
    Mockito.when(valintaTulosServiceAsyncResource.getHaunValintatulokset(Mockito.anyString()))
        .thenReturn(Observable.just(createHakijat()));
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
            new Answer<String>() {
              @Override
              public String answer(InvocationOnMock invocation) throws Throwable {
                LOG.error("Koodistosta haulle {} tyyppi!", invocation.getArguments()[0]);
                return "03";
              }
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
            new Answer<HakemusList>() {
              @Override
              public HakemusList answer(InvocationOnMock invocation) throws Throwable {
                LOG.error("Hakemuslistausta haulle!");
                return createHakemusList();
              }
            });
    Mockito.when(applicationResource.getApplicationsByOids(Mockito.anyListOf(String.class)))
        .then(
            new Answer<List<Hakemus>>() {
              @Override
              public List<Hakemus> answer(InvocationOnMock invocation) throws Throwable {
                LOG.error("Hakemuslistausta haulle!");
                return createHakemukset();
              }
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
    template.sendBodyAndProperty(
        kelaLuonti, ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI, kelaProsessi);
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
    return new KelaRouteImpl(
        DIRECT_KELA,
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
        oid, new HashMap<>(), new HashSet<>(), null, null, null, null, "ALKKAUSIURI", null);
  }
}
