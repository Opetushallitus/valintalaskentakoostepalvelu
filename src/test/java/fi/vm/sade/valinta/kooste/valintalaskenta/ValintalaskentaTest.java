package fi.vm.sade.valinta.kooste.valintalaskenta;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorFactory;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorSystem;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaStarter;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaStartParams;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusDto;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusTyyppi;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import org.apache.poi.ss.formula.functions.T;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ValintalaskentaTest {
    private static final Hakemus hakemus = new Hakemus();
    private final ApplicationAsyncResource applicationAsyncResource = mock(ApplicationAsyncResource.class);
    private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = mock(SuoritusrekisteriAsyncResource.class);
    private final ValintalaskentaAsyncResource valintalaskentaAsyncResource = mock(ValintalaskentaAsyncResource.class);
    private final ValintaperusteetAsyncResource valintaperusteetAsyncResource = mock(ValintaperusteetAsyncResource.class);
    private final LaskentaSeurantaAsyncResource seurantaAsyncResource = mock(LaskentaSeurantaAsyncResource.class);
    private final TarjontaAsyncResource tarjontaAsyncResource = mock(TarjontaAsyncResource.class);
    private final OhjausparametritAsyncResource ohjausparametritAsyncResource = mock(OhjausparametritAsyncResource.class);
    private final ValintapisteAsyncResource valintapisteAsyncResource = mock(ValintapisteAsyncResource.class);
    private final LaskentaActorSystem laskentaActorSystem = new LaskentaActorSystem(seurantaAsyncResource, new LaskentaStarter(ohjausparametritAsyncResource, valintaperusteetAsyncResource, seurantaAsyncResource, tarjontaAsyncResource), new LaskentaActorFactory(
        5,
        valintalaskentaAsyncResource,
        applicationAsyncResource,
        valintaperusteetAsyncResource,
        seurantaAsyncResource,
        suoritusrekisteriAsyncResource,
        tarjontaAsyncResource, valintapisteAsyncResource
    ), 8);
    private final String hakukohde1Oid = "h1";
    private final String hakukohde2Oid = "h2";
    private final String hakukohde3Oid = "h3";
    private final String uuid = "uuid";
    private final String hakuOid = "hakuOid";
    private final HakuV1RDTO hakuDTO = new HakuV1RDTO();
    private final List<HakukohdeJaOrganisaatio> hakukohdeJaOrganisaatios = Arrays.asList(
        new HakukohdeJaOrganisaatio(hakukohde1Oid, "o1"),
        new HakukohdeJaOrganisaatio(hakukohde2Oid, "o2"),
        new HakukohdeJaOrganisaatio(hakukohde3Oid, "o3"));
    private final AuditSession auditSession = new AuditSession("virkailijaOid", Collections.singletonList("APP_VALINTA_EVERYTHING_CRUD"), "Firefox", "127.0.0.1");
    private PisteetWithLastModified pisteet;

    @Before
    public void setUpTestData() {
        hakemus.setPersonOid("personOid");
        hakuDTO.setOid(hakuOid);
        pisteet = new PisteetWithLastModified(Optional.empty(), Collections.singletonList
            (new Valintapisteet(hakemus.getOid(), hakemus.getPersonOid(), "Frank", "Tester", Collections.emptyList())));

        when(valintaperusteetAsyncResource.haeValintaperusteet(any(), any())).thenReturn(
            Observable.just(Collections.singletonList(new ValintaperusteetDTO())));
        when(valintaperusteetAsyncResource.haeHakijaryhmat(hakukohde1Oid)).thenReturn(Observable.just(Collections.emptyList()));
        when(valintaperusteetAsyncResource.haeHakijaryhmat(hakukohde2Oid)).thenReturn(Observable.just(Collections.emptyList()));
        when(valintaperusteetAsyncResource.haeHakijaryhmat(hakukohde3Oid)).thenReturn(Observable.just(Collections.emptyList()));

        when(valintapisteAsyncResource.getValintapisteet(eq(hakuOid), eq(hakukohde1Oid), any(AuditSession.class))).thenReturn(Observable.just(pisteet));
        when(valintapisteAsyncResource.getValintapisteet(eq(hakuOid), eq(hakukohde2Oid), any(AuditSession.class))).thenReturn(Observable.just(pisteet));
        when(valintapisteAsyncResource.getValintapisteet(eq(hakuOid), eq(hakukohde3Oid), any(AuditSession.class))).thenReturn(Observable.just(pisteet));

        when(valintalaskentaAsyncResource.laskeKaikki(any())).thenReturn(Observable.just("OK"));

        when(suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohde1Oid, hakuOid)).thenReturn(Observable.just(Collections.singletonList(new Oppija())));
        when(suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohde2Oid, hakuOid)).thenReturn(Observable.just(Collections.singletonList(new Oppija())));
        when(suoritusrekisteriAsyncResource.getOppijatByHakukohde(hakukohde3Oid, hakuOid)).thenReturn(Observable.just(Collections.singletonList(new Oppija())));

        when(tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(hakuOid)).thenReturn(Observable.just(Collections.emptyMap()));
    }

    @Test
    public void onnistuneestaValintalaskennastaPidetaanKirjaaSeurantapalveluun() throws InterruptedException {
        when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde1Oid)).thenReturn(Observable.just(Collections.singletonList(hakemus)));
        when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde2Oid)).thenReturn(Observable.just(Collections.singletonList(hakemus)));
        when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde3Oid)).thenReturn(Observable.just(Collections.singletonList(hakemus)));
        when(seurantaAsyncResource.merkkaaHakukohteenTila(uuid, hakukohde1Oid, HakukohdeTila.VALMIS, Optional.empty())).thenReturn(Observable.just(Response.noContent().build()));
        when(seurantaAsyncResource.merkkaaHakukohteenTila(uuid, hakukohde2Oid, HakukohdeTila.VALMIS, Optional.empty())).thenReturn(Observable.just(Response.noContent().build()));
        when(seurantaAsyncResource.merkkaaHakukohteenTila(uuid, hakukohde3Oid, HakukohdeTila.VALMIS, Optional.empty())).thenReturn(Observable.just(Response.noContent().build()));
        when(seurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty())).thenReturn(Observable.just(Response.noContent().build()));
        when(seurantaAsyncResource.otaSeuraavaLaskentaTyonAlle()).thenReturn(Observable.just(null));

        LaskentaStartParams laskentaJaHaku = new LaskentaStartParams(auditSession, uuid, hakuOid, false, null, null, hakukohdeJaOrganisaatios, LaskentaTyyppi.HAKUKOHDE);

        laskentaActorSystem.suoritaValintalaskentaKerralla(hakuDTO, null, laskentaJaHaku);
        Thread.sleep(500);

        verify(seurantaAsyncResource).otaSeuraavaLaskentaTyonAlle();
        verify(seurantaAsyncResource).merkkaaHakukohteenTila(uuid, hakukohde1Oid, HakukohdeTila.VALMIS, Optional.empty());
        verify(seurantaAsyncResource).merkkaaHakukohteenTila(uuid, hakukohde2Oid, HakukohdeTila.VALMIS, Optional.empty());
        verify(seurantaAsyncResource).merkkaaHakukohteenTila(uuid, hakukohde3Oid, HakukohdeTila.VALMIS, Optional.empty());
        verify(seurantaAsyncResource).merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
        Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
    }

    @Test
    public void onnistuneestaValintaryhmalaskennastaPidetaanKirjaaSeurantapalveluun() throws InterruptedException {
        when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde1Oid)).thenReturn(Observable.just(Collections.singletonList(hakemus)));
        when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde2Oid)).thenReturn(Observable.just(Collections.singletonList(hakemus)));
        when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde3Oid)).thenReturn(Observable.just(Collections.singletonList(hakemus)));

        Integer vaiheenNumero = 1;

        when(valintaperusteetAsyncResource.haeValintaperusteet(eq(hakukohde1Oid), eq(vaiheenNumero))).thenReturn(Observable.just(Collections.emptyList()));
        when(valintaperusteetAsyncResource.haeValintaperusteet(eq(hakukohde2Oid), eq(vaiheenNumero))).thenReturn(Observable.just(Collections.emptyList()));
        when(valintaperusteetAsyncResource.haeValintaperusteet(eq(hakukohde3Oid), eq(vaiheenNumero))).thenReturn(Observable.just(Collections.emptyList()));

        when(valintaperusteetAsyncResource.haeHakijaryhmat(eq(hakukohde1Oid))).thenReturn(Observable.just(Collections.emptyList()));
        when(valintaperusteetAsyncResource.haeHakijaryhmat(eq(hakukohde2Oid))).thenReturn(Observable.just(Collections.emptyList()));
        when(valintaperusteetAsyncResource.haeHakijaryhmat(eq(hakukohde3Oid))).thenReturn(Observable.just(Collections.emptyList()));

        LaskentaStartParams laskentaJaHaku = new LaskentaStartParams(auditSession, uuid, hakuOid, false, vaiheenNumero, null, hakukohdeJaOrganisaatios, LaskentaTyyppi.VALINTARYHMA);

        when(valintalaskentaAsyncResource.laskeJaSijoittele(anyListOf(LaskeDTO.class))).thenReturn(Observable.just("Valintaryhmälaskenta onnistui"));
        when(seurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty())).thenReturn(Observable.just(Response.noContent().build()));
        when(seurantaAsyncResource.otaSeuraavaLaskentaTyonAlle()).thenReturn(Observable.just(null));

        laskentaActorSystem.suoritaValintalaskentaKerralla(hakuDTO, null, laskentaJaHaku);
        Thread.sleep(500);


        verify(valintaperusteetAsyncResource).haeValintaperusteet(eq(hakukohde1Oid), eq(vaiheenNumero));
        verify(valintaperusteetAsyncResource).haeValintaperusteet(eq(hakukohde2Oid), eq(vaiheenNumero));
        verify(valintaperusteetAsyncResource).haeValintaperusteet(eq(hakukohde3Oid), eq(vaiheenNumero));
        verify(valintaperusteetAsyncResource).haeHakijaryhmat(eq(hakukohde1Oid));
        verify(valintaperusteetAsyncResource).haeHakijaryhmat(eq(hakukohde2Oid));
        verify(valintaperusteetAsyncResource).haeHakijaryhmat(eq(hakukohde3Oid));
        Mockito.verifyNoMoreInteractions(valintaperusteetAsyncResource);

        verify(valintalaskentaAsyncResource).laskeJaSijoittele(anyListOf(LaskeDTO.class));
        Mockito.verifyNoMoreInteractions(valintalaskentaAsyncResource);

        verify(seurantaAsyncResource).otaSeuraavaLaskentaTyonAlle();
        verify(seurantaAsyncResource).merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
        Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
    }

    @Test
    public void epaonnistuneetLaskennatKirjataanSeurantapalveluun() throws InterruptedException {
        when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde1Oid)).thenReturn(
            Observable.error(new RuntimeException(getClass().getSimpleName() + " : Ei saatu haettua hakemuksia kohteelle " + hakukohde1Oid)));
        when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde2Oid)).thenReturn(Observable.just(Collections.singletonList(hakemus)));
        when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde3Oid)).thenReturn(Observable.just(Collections.singletonList(hakemus)));
        when(seurantaAsyncResource.merkkaaHakukohteenTila(uuid, hakukohde2Oid, HakukohdeTila.VALMIS, Optional.empty())).thenReturn(Observable.just(Response.ok().build()));
        when(seurantaAsyncResource.merkkaaHakukohteenTila(uuid, hakukohde3Oid, HakukohdeTila.VALMIS, Optional.empty())).thenReturn(Observable.just(Response.ok().build()));
        when(seurantaAsyncResource.merkkaaHakukohteenTila(uuid, hakukohde3Oid, HakukohdeTila.KESKEYTETTY, Optional.empty())).thenReturn(Observable.just(Response.ok().build()));

        LaskentaStartParams laskentaJaHaku = new LaskentaStartParams(auditSession, uuid, hakuOid, false, null, null, hakukohdeJaOrganisaatios, LaskentaTyyppi.HAKUKOHDE);

        laskentaActorSystem.suoritaValintalaskentaKerralla(hakuDTO, null, laskentaJaHaku);
        Thread.sleep(500);

        verify(valintapisteAsyncResource, times(2)).getValintapisteet(eq(hakuOid), eq(hakukohde1Oid), any(AuditSession.class));
        verify(valintapisteAsyncResource).getValintapisteet(eq(hakuOid), eq(hakukohde2Oid), any(AuditSession.class));
        verify(valintapisteAsyncResource).getValintapisteet(eq(hakuOid), eq(hakukohde3Oid), any(AuditSession.class));
        verify(seurantaAsyncResource).merkkaaHakukohteenTila(uuid, hakukohde2Oid, HakukohdeTila.VALMIS, Optional.empty());
        verify(seurantaAsyncResource).merkkaaHakukohteenTila(uuid, hakukohde3Oid, HakukohdeTila.VALMIS, Optional.empty());
        verify(seurantaAsyncResource).merkkaaHakukohteenTila(eq(uuid), eq(hakukohde1Oid), eq(HakukohdeTila.KESKEYTETTY), argThat(new ArgumentMatcher<Optional<IlmoitusDto>>() {
            private final IlmoitusTyyppi odotettuIlmoitustyyppi = IlmoitusTyyppi.VIRHE;
            private final String odotettuOtsikonSisalto = "Ei saatu haettua hakemuksia kohteelle " + hakukohde1Oid;

            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof Optional)) {
                    return false;
                }
                IlmoitusDto ilmoitusDto = ((Optional<IlmoitusDto>) argument).get();
                return odotettuIlmoitustyyppi.equals(ilmoitusDto.getTyyppi()) && ilmoitusDto.getOtsikko().contains(odotettuOtsikonSisalto);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(IlmoitusDto.class.getSimpleName() + ", jossa " + odotettuIlmoitustyyppi + " ja otsikossa \"" + odotettuOtsikonSisalto + "\"");
            }
        }));
        verify(seurantaAsyncResource).merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, Optional.empty());
        Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
    }

    @Test
    public void poikkeukseenEpaonnistuneitaLaskentojaEiKirjataSeurantapalveluun() throws InterruptedException {
        when(applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohde1Oid)).thenThrow(new RuntimeException(getClass().getSimpleName() +
            " : Ei saatu taaskaan haettua hakemuksia kohteelle " + hakukohde1Oid));

        laskentaActorSystem.suoritaValintalaskentaKerralla(hakuDTO, null, new LaskentaStartParams(auditSession, uuid, hakuOid, false, null, null, hakukohdeJaOrganisaatios, LaskentaTyyppi.HAKUKOHDE));
        Thread.sleep(500);

        Mockito.verifyNoMoreInteractions(seurantaAsyncResource);
    }
}
