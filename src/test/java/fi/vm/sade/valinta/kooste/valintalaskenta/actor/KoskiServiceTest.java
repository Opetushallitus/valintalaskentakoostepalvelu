package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.gson.Gson;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.SuoritustiedotDTO;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class KoskiServiceTest {
    private static final Gson GSON = new Gson();
    private final String koskifuntionimet = "HAEAMMATILLINENYTOARVOSANA,HAEAMMATILLINENYTOARVIOINTIASTEIKKO";

    private CompletableFuture<List<ValintaperusteetDTO>> koskiFunktionSisaltavaValintaperuste = luoKoskifunktionSisaltavaValintaperuste();
    private CompletableFuture<List<ValintaperusteetDTO>> kosketonValintaperuste = luoKosketonValintaperuste();
    private final KoskiAsyncResource koskiAsyncResource = mock(KoskiAsyncResource.class);
    private HakemusWrapper hakemusWrapper = mock(HakemusWrapper.class);
    private CompletableFuture<List<HakemusWrapper>> hakemukset = CompletableFuture.completedFuture(Collections.singletonList(hakemusWrapper));
    private final String oppijanumero = "1.2.246.562.24.50534365452";
    private final KoskiOppija koskiOppija = new KoskiOppija();
    private Set<KoskiOppija> koskioppijat = Collections.singleton(koskiOppija);
    private final SuoritustiedotDTO suoritustiedotDTO = new SuoritustiedotDTO();

    @Before
    public void setUpTestdata() {
        when(hakemusWrapper.getPersonOid()).thenReturn(oppijanumero);
        KoskiOppija.KoskiHenkilö koskiHenkilo = new KoskiOppija.KoskiHenkilö();
        koskiHenkilo.oid = oppijanumero;
        koskiOppija.setHenkilö(koskiHenkilo);
    }

    @Test
    public void koskestaHaettavienHakukohteidenListallaVoiRajoittaaMilleHakukohteilleHaetaanKoskesta() throws ExecutionException, InterruptedException {
        KoskiService service = new KoskiService("hakukohdeoid1,hakukohdeoid2", koskifuntionimet, koskiAsyncResource);

        when(koskiAsyncResource.findKoskiOppijat(Collections.singletonList(oppijanumero))).thenReturn(CompletableFuture.completedFuture(koskioppijat));

        Map<String, KoskiOppija> koskiOppijatOppijanumeroittain = service.haeKoskiOppijat("hakukohdeoid1", koskiFunktionSisaltavaValintaperuste, hakemukset, suoritustiedotDTO).get();
        assertThat(koskiOppijatOppijanumeroittain.entrySet(), hasSize(1));
        assertEquals(koskiOppijatOppijanumeroittain.get(oppijanumero), koskiOppija);
        assertTrue(suoritustiedotDTO.onKoskiopiskeluoikeudet(oppijanumero));
        assertEquals(GSON.toJson(koskiOppija.getOpiskeluoikeudet()), suoritustiedotDTO.haeKoskiOpiskeluoikeudetJson(oppijanumero));

        assertThat(service.haeKoskiOppijat("jokumuuhakukohde", koskiFunktionSisaltavaValintaperuste, hakemukset, suoritustiedotDTO).get().entrySet(), is(empty()));
    }

    @Test
    public void koskidataaKayttavienFunktionimienListallaVoiRajoittaaMilleHakukohteilleHaetaanKoskesta() throws ExecutionException, InterruptedException {
        KoskiService service = new KoskiService("ALL", koskifuntionimet, koskiAsyncResource);

        when(koskiAsyncResource.findKoskiOppijat(Collections.singletonList(oppijanumero))).thenReturn(CompletableFuture.completedFuture(koskioppijat));

        Map<String, KoskiOppija> koskiOppijatOppijanumeroittain = service.haeKoskiOppijat("hakukohdeoid1", koskiFunktionSisaltavaValintaperuste, hakemukset, suoritustiedotDTO).get();
        assertThat(koskiOppijatOppijanumeroittain.entrySet(), hasSize(1));
        assertEquals(koskiOppijatOppijanumeroittain.get(oppijanumero), koskiOppija);
        assertTrue(suoritustiedotDTO.onKoskiopiskeluoikeudet(oppijanumero));
        assertEquals(GSON.toJson(koskiOppija.getOpiskeluoikeudet()), suoritustiedotDTO.haeKoskiOpiskeluoikeudetJson(oppijanumero));

        assertThat(service.haeKoskiOppijat("hakukohdeoid1", kosketonValintaperuste, hakemukset, suoritustiedotDTO).get().entrySet(), is(empty()));
    }

    private CompletableFuture<List<ValintaperusteetDTO>> luoKoskifunktionSisaltavaValintaperuste() {
        return kaariListaanJaFutureen(
            GSON.fromJson(classpathResourceAsString("fi/vm/sade/valinta/kooste/valintalaskenta/actor/valintaperusteita_koskikaavojen_kanssa.json"),
                ValintaperusteetDTO.class));
    }

    private CompletableFuture<List<ValintaperusteetDTO>> luoKosketonValintaperuste() {
        return kaariListaanJaFutureen(
            GSON.fromJson(classpathResourceAsString("fi/vm/sade/valinta/kooste/valintalaskenta/actor/valintaperusteita_ilman_koskikaavoja.json"),
                ValintaperusteetDTO.class));
    }

    private <T> CompletableFuture<List<T>> kaariListaanJaFutureen(T x) {
        return CompletableFuture.completedFuture(Collections.singletonList(x));
    }

    private static String classpathResourceAsString(String path) {
        try {
            return IOUtils.toString(new ClassPathResource(path).getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
