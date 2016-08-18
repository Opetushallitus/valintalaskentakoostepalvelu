package fi.vm.sade.valinta.kooste.valintalaskenta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorFactory;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaSupervisor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorParams;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaStartParams;
import fi.vm.sade.valinta.kooste.valintalaskenta.spec.ValintalaskentaSpec;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Matchers.any;

/**
 * @author Jussi Jartamo
 */
public class ValintaryhmaLaskentaActorTest extends ValintalaskentaSpec {

    private final LaskentaSupervisor laskentaSupervisor = Mockito.mock(LaskentaSupervisor.class);

    @Test
    public void testaaValintaryhmaActorYhdellaHakukohteellaKunKaikkiMeneeHyvin() {
        final String UUID1 = "UUID1";
        final String HAKUOID1 = "HAKUOID1";
        final String HAKUKOHDE1 = "HAKUKOHDE1";
        final String ORGANISAATIO1 = "ORGANISAATIO1";
        final HakuV1RDTO hakuDTO = new HakuV1RDTO();
        hakuDTO.setOid(HAKUOID1);

        ApplicationAsyncResource applicationAsyncResource = new ApplicationMock().addFilter(
                (haku, hakukohde) -> true,
                Collections.emptyList()
        ).build();
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusrekisteriMock().addFilter(
                (hakukohde, referenssiPvm) -> true,
                (oppijat, poikkeus) -> oppijat.accept(Collections.emptyList())
        ).build();

        ValintaperusteetAsyncResource valintaperusteetAsyncResource = Mockito.mock(ValintaperusteetAsyncResource.class);
        new HakijaryhmaMock().addFilter(
                hakukohdeOid -> true,
                (hakijaryhmat, poikkeus) -> hakijaryhmat.accept(Collections.emptyList())
        ).build(valintaperusteetAsyncResource);
        new ValintaperusteetMock().addFilter(
                (hakukohdeOid,valinnanvaihe) -> true,
                (valintaperusteet, poikkeus) -> valintaperusteet.accept(Collections.emptyList())
        ).build(valintaperusteetAsyncResource);

        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource = new LaskentaSeurantaMock().build();

        LaskentaActorFactory actorFactory = new LaskentaActorFactory(
                5,
                new ValintalaskentaMock()
                        .addFilter(l -> true, (c,t) -> c.accept("VALMIS"))
                        .build(),
                applicationAsyncResource,
                valintaperusteetAsyncResource,
                laskentaSeurantaAsyncResource,
                suoritusrekisteriAsyncResource
        );
        List<HakukohdeJaOrganisaatio> hakukohdeJaOrganisaatios = Arrays.asList(new HakukohdeJaOrganisaatio(HAKUKOHDE1, ORGANISAATIO1));
        LaskentaStartParams laskentaStartParams = new LaskentaStartParams(
                UUID1,
                HAKUOID1,
                false,
                null,
                false,
                hakukohdeJaOrganisaatios,
                null // tyyppi
        );
        LaskentaActor actor = actorFactory.createValintaryhmaActor(laskentaSupervisor, hakuDTO, new LaskentaActorParams(laskentaStartParams, null));
        actor.start();

        Assert.assertTrue(actor.isValmis());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any(), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.VALMIS), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), any(Optional.class));
    }

    @Test
    public void testaaValintaryhmaActorYhdellaHakukohteellaKunJokinResurssiEpaonnistuu() {
        final String UUID1 = "UUID1";
        final String HAKUOID1 = "HAKUOID1";
        final String HAKUKOHDE1 = "HAKUKOHDE1";
        final String ORGANISAATIO1 = "ORGANISAATIO1";
        final HakuV1RDTO hakuDTO = new HakuV1RDTO();
        hakuDTO.setOid(HAKUOID1);

        ApplicationAsyncResource applicationAsyncResource = new ApplicationMock().addFilter(
                (haku, hakukohde) -> true,
                Collections.emptyList()
        ).build();
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusrekisteriMock().addFilter(
                (hakukohde, referenssiPvm) -> true,
                (oppijat, poikkeus) -> oppijat.accept(Collections.emptyList())
        ).build();

        ValintaperusteetAsyncResource valintaperusteetAsyncResource = Mockito.mock(ValintaperusteetAsyncResource.class);
        new HakijaryhmaMock().addFilter(
                hakukohdeOid -> true,
                (hakijaryhmat, poikkeus) -> poikkeus.accept(new RuntimeException("Hakijaryhmien haku ei onnistu!"))
        ).build(valintaperusteetAsyncResource);
        new ValintaperusteetMock().addFilter(
                (hakukohdeOid,valinnanvaihe) -> true,
                (valintaperusteet, poikkeus) -> valintaperusteet.accept(Collections.emptyList())
        ).build(valintaperusteetAsyncResource);

        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource =
                new LaskentaSeurantaMock().build();

        LaskentaActorFactory actorFactory = new LaskentaActorFactory(
                5,
                new ValintalaskentaMock()
                        .addFilter(l -> true, (c,t) -> c.accept("VALMIS"))
                        .build(),
                applicationAsyncResource,
                valintaperusteetAsyncResource,
                laskentaSeurantaAsyncResource,
                suoritusrekisteriAsyncResource
        );

        List<HakukohdeJaOrganisaatio> hakukohdeJaOrganisaatios = Arrays.asList(new HakukohdeJaOrganisaatio(HAKUKOHDE1, ORGANISAATIO1));
        LaskentaStartParams laskentaStartParams = new LaskentaStartParams(
                UUID1,
                HAKUOID1,
                false,
                null,
                false,
                hakukohdeJaOrganisaatios,
                null // tyyppi
        );
        LaskentaActor actor = actorFactory.createValintaryhmaActor(laskentaSupervisor, hakuDTO, new LaskentaActorParams(laskentaStartParams, null));
        actor.start();

        Assert.assertTrue(actor.isValmis());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any(), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.KESKEYTETTY), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), any(Optional.class));
    }

    @Test
    public void testaaValintaryhmaActorYhdellaHakukohteellaKunLaskentaVaiheEpaonnistuu() {
        final String UUID1 = "UUID1";
        final String HAKUOID1 = "HAKUOID1";
        final String HAKUKOHDE1 = "HAKUKOHDE1";
        final String ORGANISAATIO1 = "ORGANISAATIO1";
        final HakuV1RDTO hakuDTO = new HakuV1RDTO();
        hakuDTO.setOid(HAKUOID1);

        ApplicationAsyncResource applicationAsyncResource = new ApplicationMock().addFilter(
                (haku, hakukohde) -> true,
                Collections.emptyList()
        ).build();
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusrekisteriMock().addFilter(
                (hakukohde, referenssiPvm) -> true,
                (oppijat, poikkeus) -> oppijat.accept(Collections.emptyList())
        ).build();

        ValintaperusteetAsyncResource valintaperusteetAsyncResource = Mockito.mock(ValintaperusteetAsyncResource.class);
        new HakijaryhmaMock().addFilter(
                hakukohdeOid -> true,
                (hakijaryhmat, poikkeus) -> hakijaryhmat.accept(Collections.emptyList())
        ).build(valintaperusteetAsyncResource);
        new ValintaperusteetMock().addFilter(
                (hakukohdeOid,valinnanvaihe) -> true,
                (valintaperusteet, poikkeus) -> valintaperusteet.accept(Collections.emptyList())
        ).build(valintaperusteetAsyncResource);

        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource =
                new LaskentaSeurantaMock().build();

        LaskentaActorFactory actorFactory = new LaskentaActorFactory(
                5,
                new ValintalaskentaMock()
                        .addFilter(l -> true, (c,t) -> t.accept(new RuntimeException("Epaonnistuminen!")))
                        .build(),
                applicationAsyncResource,
                valintaperusteetAsyncResource,
                laskentaSeurantaAsyncResource,
                suoritusrekisteriAsyncResource
        );

        List<HakukohdeJaOrganisaatio> hakukohdeJaOrganisaatios = Arrays.asList(new HakukohdeJaOrganisaatio(HAKUKOHDE1, ORGANISAATIO1));
        LaskentaStartParams laskentaStartParams = new LaskentaStartParams(
                UUID1,
                HAKUOID1,
                false,
                null,
                false,
                hakukohdeJaOrganisaatios,
                null // tyyppi
        );
        LaskentaActor actor = actorFactory.createValintaryhmaActor(laskentaSupervisor, hakuDTO, new LaskentaActorParams(laskentaStartParams, null));
        actor.start();

        Assert.assertTrue(actor.isValmis());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any(), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.KESKEYTETTY), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), any(Optional.class));
    }

    @Test
    public void testaaValintaryhmalaskentaKahdellaHakukohteellaKunKaikkiMeneeHyvin() {
        final String UUID1 = "UUID1";
        final String HAKUOID1 = "HAKUOID1";
        final String HAKUKOHDE1 = "HAKUKOHDE1";
        final String HAKUKOHDE2 = "HAKUKOHDE2";
        final String ORGANISAATIO1 = "ORGANISAATIO1";
        final HakuV1RDTO hakuDTO = new HakuV1RDTO();
        hakuDTO.setOid(HAKUOID1);

        ApplicationAsyncResource applicationAsyncResource = new ApplicationMock().addFilter(
                (haku, hakukohde) -> true,
                Collections.emptyList()
        ).build();
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusrekisteriMock().addFilter(
                (hakukohde, referenssiPvm) -> true,
                (oppijat, poikkeus) -> oppijat.accept(Collections.emptyList())
        ).build();

        ValintaperusteetAsyncResource valintaperusteetAsyncResource = Mockito.mock(ValintaperusteetAsyncResource.class);
        new HakijaryhmaMock().addFilter(
                hakukohdeOid -> true,
                (hakijaryhmat, poikkeus) -> hakijaryhmat.accept(Collections.emptyList())
        ).build(valintaperusteetAsyncResource);
        new ValintaperusteetMock().addFilter(
                (hakukohdeOid,valinnanvaihe) -> true,
                (valintaperusteet, poikkeus) -> valintaperusteet.accept(Collections.emptyList())
        ).build(valintaperusteetAsyncResource);

        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource =
                new LaskentaSeurantaMock().build();

        LaskentaActorFactory actorFactory = new LaskentaActorFactory(
                5,
                new ValintalaskentaMock()
                        .addFilter(l -> true, (c,t) -> c.accept("VALMIS"))
                        .build(),
                applicationAsyncResource,
                valintaperusteetAsyncResource,
                laskentaSeurantaAsyncResource,
                suoritusrekisteriAsyncResource
        );

        List<HakukohdeJaOrganisaatio> hakukohdeJaOrganisaatios = Arrays.asList(new HakukohdeJaOrganisaatio(HAKUKOHDE1, ORGANISAATIO1), new HakukohdeJaOrganisaatio(HAKUKOHDE2, ORGANISAATIO1));
        LaskentaStartParams laskentaStartParams = new LaskentaStartParams(
                UUID1,
                HAKUOID1,
                false,
                null,
                false,
                hakukohdeJaOrganisaatios,
                null // tyyppi
        );
        LaskentaActor actor = actorFactory.createValintaryhmaActor(laskentaSupervisor, hakuDTO, new LaskentaActorParams(laskentaStartParams, null));

        actor.start();
        Assert.assertTrue(actor.isValmis());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any(), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.VALMIS), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), any(Optional.class));
    }

    @Test
    public void testaaValintaryhmalaskentaKahdellaHakukohteellaKunToinenLaskentaEpaonnistuu() {
        final String UUID1 = "UUID1";
        final String HAKUOID1 = "HAKUOID1";
        final String HAKUKOHDE1 = "HAKUKOHDE1";
        final String HAKUKOHDE2 = "HAKUKOHDE2";
        final String ORGANISAATIO1 = "ORGANISAATIO1";
        final HakuV1RDTO hakuDTO = new HakuV1RDTO();
        hakuDTO.setOid(HAKUOID1);

        ApplicationAsyncResource applicationAsyncResource = new ApplicationMock().addFilter(
                (haku, hakukohde) -> true,
                Collections.emptyList()
        ).build();
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusrekisteriMock().addFilter(
                (hakukohde, referenssiPvm) -> true,
                (oppijat, poikkeus) -> oppijat.accept(Collections.emptyList())
        ).build();

        ValintaperusteetAsyncResource valintaperusteetAsyncResource = Mockito.mock(ValintaperusteetAsyncResource.class);
        new HakijaryhmaMock().addFilter(
                hakukohdeOid -> true,
                (hakijaryhmat, poikkeus) -> hakijaryhmat.accept(Collections.emptyList())
        ).build(valintaperusteetAsyncResource);
        new ValintaperusteetMock().addFilter(
                (hakukohdeOid,valinnanvaihe) -> true,
                (valintaperusteet, poikkeus) -> valintaperusteet.accept(Collections.emptyList())
        ).build(valintaperusteetAsyncResource);

        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource =
                new LaskentaSeurantaMock().build();

        LaskentaActorFactory actorFactory = new LaskentaActorFactory(
                5,
                new ValintalaskentaMock()
                        .addFilter(l -> true, (c, t) -> t.accept(new RuntimeException("Epaonnistuminen!")))
                        .build(),
                applicationAsyncResource,
                valintaperusteetAsyncResource,
                laskentaSeurantaAsyncResource,
                suoritusrekisteriAsyncResource
        );

        List<HakukohdeJaOrganisaatio> hakukohdeJaOrganisaatios = Arrays.asList(new HakukohdeJaOrganisaatio(HAKUKOHDE1, ORGANISAATIO1), new HakukohdeJaOrganisaatio(HAKUKOHDE2, ORGANISAATIO1));
        LaskentaStartParams laskentaStartParams = new LaskentaStartParams(
                UUID1,
                HAKUOID1,
                false,
                null,
                false,
                hakukohdeJaOrganisaatios,
                null // tyyppi
        );
        LaskentaActor actor = actorFactory.createValintaryhmaActor(laskentaSupervisor, hakuDTO, new LaskentaActorParams(laskentaStartParams, null));

        actor.start();
        Assert.assertTrue(actor.isValmis());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any(), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.KESKEYTETTY), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), any(Optional.class));
    }

    @Test
    public void testaaValintaryhmalaskentaKahdellaHakukohteellaKunToisenHakukohteenYksittainenPalvelukutsuEpaonnistuu() {
        final String UUID1 = "UUID1";
        final String HAKUOID1 = "HAKUOID1";
        final String HAKUKOHDE1 = "HAKUKOHDE1";
        final String HAKUKOHDE2 = "HAKUKOHDE2";
        final String ORGANISAATIO1 = "ORGANISAATIO1";
        final HakuV1RDTO hakuDTO = new HakuV1RDTO();
        hakuDTO.setOid(HAKUOID1);

        ApplicationAsyncResource applicationAsyncResource = new ApplicationMock().addFilter((haku, hakukohde) -> true, Collections.EMPTY_LIST).build();
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusrekisteriMock().addFilter(
                (hakukohde, referenssiPvm) -> true,
                (oppijat, poikkeus) -> oppijat.accept(Collections.emptyList())
        ).build();
        final AtomicInteger ekaOnnistuu = new AtomicInteger(0);
        ValintaperusteetAsyncResource valintaperusteetAsyncResource = Mockito.mock(ValintaperusteetAsyncResource.class);
        new HakijaryhmaMock().addFilter(
                hakukohdeOid -> true,
                (hakijaryhmat, poikkeus) -> {
                    if(ekaOnnistuu.getAndIncrement() == 0) {
                        hakijaryhmat.accept(Collections.emptyList());
                    } else {
                        poikkeus.accept(new RuntimeException("Vain ensimmäinen kutsu onnistuu!"));
                    }
                }
        ).build(valintaperusteetAsyncResource);
        new ValintaperusteetMock().addFilter(
                (hakukohdeOid,valinnanvaihe) -> true,
                (valintaperusteet, poikkeus) -> valintaperusteet.accept(Collections.emptyList())
        ).build(valintaperusteetAsyncResource);

        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource =
                new LaskentaSeurantaMock().build();

        LaskentaActorFactory actorFactory = new LaskentaActorFactory(
                5,
                new ValintalaskentaMock()
                        .addFilter(l -> true, (c, t) -> {
                            Assert.fail("Laskentaa ei pitäisi koskaan kutsua kun datoja ei saatu haettua!");
                        })
                        .build(),
                applicationAsyncResource,
                valintaperusteetAsyncResource,
                laskentaSeurantaAsyncResource,
                suoritusrekisteriAsyncResource
        );

        List<HakukohdeJaOrganisaatio> hakukohdeJaOrganisaatios = Arrays.asList(new HakukohdeJaOrganisaatio(HAKUKOHDE1, ORGANISAATIO1), new HakukohdeJaOrganisaatio(HAKUKOHDE2, ORGANISAATIO1));
        LaskentaStartParams laskentaStartParams = new LaskentaStartParams(
                UUID1,
                HAKUOID1,
                false,
                null,
                false,
                hakukohdeJaOrganisaatios,
                null // tyyppi
        );
        LaskentaActor actor = actorFactory.createValintaryhmaActor(laskentaSupervisor, hakuDTO, new LaskentaActorParams(laskentaStartParams, null));

        actor.start();
        Assert.assertTrue(actor.isValmis());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any(), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.atMost(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.KESKEYTETTY), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.atLeast(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.KESKEYTETTY), any(Optional.class));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), any(Optional.class));
    }
}
