package fi.vm.sade.valinta.kooste.valintalaskenta;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorFactory;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaSupervisor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import static fi.vm.sade.valinta.kooste.valintalaskenta.spec.ValintalaskentaSpec.*;

import fi.vm.sade.valinta.kooste.valintalaskenta.spec.ValintalaskentaSpec;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import junit.framework.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jussi Jartamo
 */
public class ValintaryhmaLaskentaActorTest extends ValintalaskentaSpec {

    @Test
    public void testaaValintaryhmaActorYhdellaHakukohteellaKunKaikkiMeneeHyvin() {
        final String UUID1 = "UUID1";
        final String HAKUOID1 = "HAKUOID1";
        final String HAKUKOHDE1 = "HAKUKOHDE1";
        final String ORGANISAATIO1 = "ORGANISAATIO1";

        ApplicationAsyncResource applicationAsyncResource = new ApplicationMock().addFilter(
                (haku, hakukohde) -> {
                    return true;
                },
                (hakemukset, poikkeus) -> {
                    hakemukset.accept(Collections.emptyList());
                }
        ).build();
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusrekisteriMock().addFilter(
                (hakukohde, referenssiPvm) -> {
                    return true;
                },
                (oppijat, poikkeus) -> {
                    oppijat.accept(Collections.emptyList());
                }
        ).build();

        ValintaperusteetAsyncResource valintaperusteetAsyncResource = Mockito.mock(ValintaperusteetAsyncResource.class);
        new HakijaryhmaMock().addFilter(
                hakukohdeOid -> {
                    return true;
                },
                (hakijaryhmat, poikkeus) -> {
                    hakijaryhmat.accept(Collections.emptyList());
                }
        ).build(valintaperusteetAsyncResource);
        new ValintaperusteetMock().addFilter(
                (hakukohdeOid,valinnanvaihe) -> {
                    return true;
                },
                (valintaperusteet, poikkeus) -> {
                    valintaperusteet.accept(Collections.emptyList());
                }
        ).build(valintaperusteetAsyncResource);

        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource =
                new LaskentaSeurantaMock().build();

        LaskentaActorFactory actorFactory = new LaskentaActorFactory(
                new ValintalaskentaMock()
                        .addFilter(l -> true, (c,t) -> {
                            c.accept("VALMIS");
                        })
                        .build(),
                applicationAsyncResource,
                valintaperusteetAsyncResource,
                laskentaSeurantaAsyncResource,
                suoritusrekisteriAsyncResource,
                Mockito.mock(LaskentaSupervisor.class)
        );

        LaskentaActor actor = actorFactory.createValintaryhmaActor(UUID1, HAKUOID1, null, false,null, Arrays.asList(new HakukohdeJaOrganisaatio(HAKUKOHDE1, ORGANISAATIO1)));
        actor.aloita();
        Assert.assertTrue(actor.isValmis());

        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.VALMIS));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testaaValintaryhmaActorYhdellaHakukohteellaKunJokinResurssiEpaonnistuu() {
        final String UUID1 = "UUID1";
        final String HAKUOID1 = "HAKUOID1";
        final String HAKUKOHDE1 = "HAKUKOHDE1";
        final String ORGANISAATIO1 = "ORGANISAATIO1";

        ApplicationAsyncResource applicationAsyncResource = new ApplicationMock().addFilter(
                (haku, hakukohde) -> {
                    return true;
                },
                (hakemukset, poikkeus) -> {
                    hakemukset.accept(Collections.emptyList());
                }
        ).build();
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusrekisteriMock().addFilter(
                (hakukohde, referenssiPvm) -> {
                    return true;
                },
                (oppijat, poikkeus) -> {
                    oppijat.accept(Collections.emptyList());
                }
        ).build();

        ValintaperusteetAsyncResource valintaperusteetAsyncResource = Mockito.mock(ValintaperusteetAsyncResource.class);
        new HakijaryhmaMock().addFilter(
                hakukohdeOid -> {
                    return true;
                },
                (hakijaryhmat, poikkeus) -> {
                    poikkeus.accept(new RuntimeException("Hakijaryhmien haku ei onnistu!"));
                }
        ).build(valintaperusteetAsyncResource);
        new ValintaperusteetMock().addFilter(
                (hakukohdeOid,valinnanvaihe) -> {
                    return true;
                },
                (valintaperusteet, poikkeus) -> {
                    valintaperusteet.accept(Collections.emptyList());
                }
        ).build(valintaperusteetAsyncResource);

        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource =
                new LaskentaSeurantaMock().build();

        LaskentaActorFactory actorFactory = new LaskentaActorFactory(
                new ValintalaskentaMock()
                        .addFilter(l -> true, (c,t) -> {
                            c.accept("VALMIS");
                        })
                        .build(),
                applicationAsyncResource,
                valintaperusteetAsyncResource,
                laskentaSeurantaAsyncResource,
                suoritusrekisteriAsyncResource,
                Mockito.mock(LaskentaSupervisor.class)
        );

        LaskentaActor actor = actorFactory.createValintaryhmaActor(UUID1, HAKUOID1, null, false,null, Arrays.asList(new HakukohdeJaOrganisaatio(HAKUKOHDE1, ORGANISAATIO1)));
        actor.aloita();
        Assert.assertTrue(actor.isValmis());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.KESKEYTETTY));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testaaValintaryhmaActorYhdellaHakukohteellaKunLaskentaVaiheEpaonnistuu() {
        final String UUID1 = "UUID1";
        final String HAKUOID1 = "HAKUOID1";
        final String HAKUKOHDE1 = "HAKUKOHDE1";
        final String ORGANISAATIO1 = "ORGANISAATIO1";

        ApplicationAsyncResource applicationAsyncResource = new ApplicationMock().addFilter(
                (haku, hakukohde) -> {
                    return true;
                },
                (hakemukset, poikkeus) -> {
                    hakemukset.accept(Collections.emptyList());
                }
        ).build();
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusrekisteriMock().addFilter(
                (hakukohde, referenssiPvm) -> {
                    return true;
                },
                (oppijat, poikkeus) -> {
                    oppijat.accept(Collections.emptyList());
                }
        ).build();

        ValintaperusteetAsyncResource valintaperusteetAsyncResource = Mockito.mock(ValintaperusteetAsyncResource.class);
        new HakijaryhmaMock().addFilter(
                hakukohdeOid -> {
                    return true;
                },
                (hakijaryhmat, poikkeus) -> {
                    hakijaryhmat.accept(Collections.emptyList());
                    //poikkeus.accept(new RuntimeException("Hakijaryhmien haku ei onnistu!"));
                }
        ).build(valintaperusteetAsyncResource);
        new ValintaperusteetMock().addFilter(
                (hakukohdeOid,valinnanvaihe) -> {
                    return true;
                },
                (valintaperusteet, poikkeus) -> {
                    valintaperusteet.accept(Collections.emptyList());
                }
        ).build(valintaperusteetAsyncResource);

        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource =
                new LaskentaSeurantaMock().build();

        LaskentaActorFactory actorFactory = new LaskentaActorFactory(
                new ValintalaskentaMock()
                        .addFilter(l -> true, (c,t) -> {
                           t.accept(new RuntimeException("Epaonnistuminen!"));
                        })
                        .build(),
                applicationAsyncResource,
                valintaperusteetAsyncResource,
                laskentaSeurantaAsyncResource,
                suoritusrekisteriAsyncResource,
                Mockito.mock(LaskentaSupervisor.class)
        );

        LaskentaActor actor = actorFactory.createValintaryhmaActor(UUID1, HAKUOID1, null, false,null, Arrays.asList(new HakukohdeJaOrganisaatio(HAKUKOHDE1, ORGANISAATIO1)));
        actor.aloita();
        Assert.assertTrue(actor.isValmis());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.KESKEYTETTY));
                //Mockito.eq(LaskentaTila.VALMIS), Mockito.eq(HakukohdeTila.KESKEYTETTY));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testaaValintaryhmalaskentaKahdellaHakukohteellaKunKaikkiMeneeHyvin() {
        final String UUID1 = "UUID1";
        final String HAKUOID1 = "HAKUOID1";
        final String HAKUKOHDE1 = "HAKUKOHDE1";
        final String HAKUKOHDE2 = "HAKUKOHDE2";
        final String ORGANISAATIO1 = "ORGANISAATIO1";

        ApplicationAsyncResource applicationAsyncResource = new ApplicationMock().addFilter(
                (haku, hakukohde) -> {
                    return true;
                },
                (hakemukset, poikkeus) -> {
                    hakemukset.accept(Collections.emptyList());
                }
        ).build();
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusrekisteriMock().addFilter(
                (hakukohde, referenssiPvm) -> {
                    return true;
                },
                (oppijat, poikkeus) -> {
                    oppijat.accept(Collections.emptyList());
                }
        ).build();

        ValintaperusteetAsyncResource valintaperusteetAsyncResource = Mockito.mock(ValintaperusteetAsyncResource.class);
        new HakijaryhmaMock().addFilter(
                hakukohdeOid -> {
                    return true;
                },
                (hakijaryhmat, poikkeus) -> {
                    hakijaryhmat.accept(Collections.emptyList());
                }
        ).build(valintaperusteetAsyncResource);
        new ValintaperusteetMock().addFilter(
                (hakukohdeOid,valinnanvaihe) -> {
                    return true;
                },
                (valintaperusteet, poikkeus) -> {
                    valintaperusteet.accept(Collections.emptyList());
                }
        ).build(valintaperusteetAsyncResource);

        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource =
                new LaskentaSeurantaMock().build();

        LaskentaActorFactory actorFactory = new LaskentaActorFactory(
                new ValintalaskentaMock()
                        .addFilter(l -> true, (c,t) -> {
                            c.accept("VALMIS");
                            //t.accept(new RuntimeException("Epaonnistuminen!"));
                        })
                        .build(),
                applicationAsyncResource,
                valintaperusteetAsyncResource,
                laskentaSeurantaAsyncResource,
                suoritusrekisteriAsyncResource,
                Mockito.mock(LaskentaSupervisor.class)
        );

        LaskentaActor actor = actorFactory.createValintaryhmaActor(UUID1, HAKUOID1, null, false,null, Arrays.asList(
                new HakukohdeJaOrganisaatio(HAKUKOHDE1, ORGANISAATIO1),
                new HakukohdeJaOrganisaatio(HAKUKOHDE2, ORGANISAATIO1)));

        actor.aloita();
        Assert.assertTrue(actor.isValmis());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.VALMIS));
        //Mockito.eq(LaskentaTila.VALMIS), Mockito.eq(HakukohdeTila.KESKEYTETTY));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testaaValintaryhmalaskentaKahdellaHakukohteellaKunToinenLaskentaEpaonnistuu() {
        final String UUID1 = "UUID1";
        final String HAKUOID1 = "HAKUOID1";
        final String HAKUKOHDE1 = "HAKUKOHDE1";
        final String HAKUKOHDE2 = "HAKUKOHDE2";
        final String ORGANISAATIO1 = "ORGANISAATIO1";

        ApplicationAsyncResource applicationAsyncResource = new ApplicationMock().addFilter(
                (haku, hakukohde) -> {
                    return true;
                },
                (hakemukset, poikkeus) -> {
                    hakemukset.accept(Collections.emptyList());
                }
        ).build();
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusrekisteriMock().addFilter(
                (hakukohde, referenssiPvm) -> {
                    return true;
                },
                (oppijat, poikkeus) -> {
                    oppijat.accept(Collections.emptyList());
                }
        ).build();

        ValintaperusteetAsyncResource valintaperusteetAsyncResource = Mockito.mock(ValintaperusteetAsyncResource.class);
        new HakijaryhmaMock().addFilter(
                hakukohdeOid -> {
                    return true;
                },
                (hakijaryhmat, poikkeus) -> {
                    hakijaryhmat.accept(Collections.emptyList());
                }
        ).build(valintaperusteetAsyncResource);
        new ValintaperusteetMock().addFilter(
                (hakukohdeOid,valinnanvaihe) -> {
                    return true;
                },
                (valintaperusteet, poikkeus) -> {
                    valintaperusteet.accept(Collections.emptyList());
                }
        ).build(valintaperusteetAsyncResource);

        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource =
                new LaskentaSeurantaMock().build();

        LaskentaActorFactory actorFactory = new LaskentaActorFactory(
                new ValintalaskentaMock()
                        .addFilter(l -> true, (c, t) -> {
                            t.accept(new RuntimeException("Epaonnistuminen!"));
                        })
                        .build(),
                applicationAsyncResource,
                valintaperusteetAsyncResource,
                laskentaSeurantaAsyncResource,
                suoritusrekisteriAsyncResource,
                Mockito.mock(LaskentaSupervisor.class)
        );

        LaskentaActor actor = actorFactory.createValintaryhmaActor(UUID1, HAKUOID1, null, false, null, Arrays.asList(
                new HakukohdeJaOrganisaatio(HAKUKOHDE1, ORGANISAATIO1),
                new HakukohdeJaOrganisaatio(HAKUKOHDE2, ORGANISAATIO1)));

        actor.aloita();
        Assert.assertTrue(actor.isValmis());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.KESKEYTETTY));
        //Mockito.eq(LaskentaTila.VALMIS), Mockito.eq(HakukohdeTila.KESKEYTETTY));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testaaValintaryhmalaskentaKahdellaHakukohteellaKunToisenHakukohteenYksittainenPalvelukutsuEpaonnistuu() {
        final String UUID1 = "UUID1";
        final String HAKUOID1 = "HAKUOID1";
        final String HAKUKOHDE1 = "HAKUKOHDE1";
        final String HAKUKOHDE2 = "HAKUKOHDE2";
        final String ORGANISAATIO1 = "ORGANISAATIO1";

        ApplicationAsyncResource applicationAsyncResource = new ApplicationMock().addFilter(
                (haku, hakukohde) -> {
                    return true;
                },
                (hakemukset, poikkeus) -> {
                    hakemukset.accept(Collections.emptyList());
                }
        ).build();
        SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = new SuoritusrekisteriMock().addFilter(
                (hakukohde, referenssiPvm) -> {
                    return true;
                },
                (oppijat, poikkeus) -> {
                    oppijat.accept(Collections.emptyList());
                }
        ).build();
        final AtomicInteger ekaOnnistuu = new AtomicInteger(0);
        ValintaperusteetAsyncResource valintaperusteetAsyncResource = Mockito.mock(ValintaperusteetAsyncResource.class);
        new HakijaryhmaMock().addFilter(
                hakukohdeOid -> {
                    return true;
                },
                (hakijaryhmat, poikkeus) -> {
                    if(ekaOnnistuu.getAndIncrement() == 0) {
                        hakijaryhmat.accept(Collections.emptyList());
                    } else {
                        poikkeus.accept(new RuntimeException("Vain ensimmäinen kutsu onnistuu!"));
                    }
                }
        ).build(valintaperusteetAsyncResource);
        new ValintaperusteetMock().addFilter(
                (hakukohdeOid,valinnanvaihe) -> {
                    return true;
                },
                (valintaperusteet, poikkeus) -> {
                    valintaperusteet.accept(Collections.emptyList());
                }
        ).build(valintaperusteetAsyncResource);

        LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource =
                new LaskentaSeurantaMock().build();

        LaskentaActorFactory actorFactory = new LaskentaActorFactory(
                new ValintalaskentaMock()
                        .addFilter(l -> true, (c, t) -> {
                            Assert.fail("Laskentaa ei pitäisi koskaan kutsua kun datoja ei saatu haettua!");
                        })
                        .build(),
                applicationAsyncResource,
                valintaperusteetAsyncResource,
                laskentaSeurantaAsyncResource,
                suoritusrekisteriAsyncResource,
                Mockito.mock(LaskentaSupervisor.class)
        );

        LaskentaActor actor = actorFactory.createValintaryhmaActor(UUID1, HAKUOID1, null, false, null, Arrays.asList(
                new HakukohdeJaOrganisaatio(HAKUKOHDE1, ORGANISAATIO1),
                new HakukohdeJaOrganisaatio(HAKUKOHDE2, ORGANISAATIO1)));

        actor.aloita();
        Assert.assertTrue(actor.isValmis());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaHakukohteenTila(Mockito.anyString(), Mockito.anyString(), Mockito.any());
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.atMost(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.KESKEYTETTY));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.atLeast(1)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any(), Mockito.eq(HakukohdeTila.KESKEYTETTY));
        //Mockito.eq(LaskentaTila.VALMIS), Mockito.eq(HakukohdeTila.KESKEYTETTY));
        Mockito.verify(laskentaSeurantaAsyncResource, Mockito.times(0)).merkkaaLaskennanTila(Mockito.anyString(), Mockito.any());
    }
}
