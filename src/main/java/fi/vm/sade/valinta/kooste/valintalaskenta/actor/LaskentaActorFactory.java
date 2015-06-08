package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.UuidHakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintakoelaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintalaskentaJaValintakoelaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintalaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintaryhmaPalvelukutsuYhdiste;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintaryhmatKatenoivaValintalaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.SuoritusrekisteriPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.ValintaperusteetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategiaImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuStrategia;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.YksiPalvelukutsuKerrallaPalvelukutsuStrategia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Jussi Jartamo
 */
@Service
public class LaskentaActorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaActorFactory.class);

    private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    private final LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource;
    private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;

    @Autowired
    public LaskentaActorFactory(
            ValintalaskentaAsyncResource valintalaskentaAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            ValintaperusteetAsyncResource valintaperusteetAsyncResource,
            LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource,
            SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource
    ) {
        this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
        this.laskentaSeurantaAsyncResource = laskentaSeurantaAsyncResource;
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
    }

    public LaskentaActor createValintaryhmaActor(LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final PalvelukutsuStrategia laskentaStrategia = createStrategia();
        final PalvelukutsuStrategia valintaperusteetStrategia = createStrategia();
        final PalvelukutsuStrategia hakemuksetStrategia = createStrategia();
        final PalvelukutsuStrategia hakijaryhmatStrategia = createStrategia();
        final PalvelukutsuStrategia suoritusrekisteriStrategia = createStrategia();
        final Collection<PalvelukutsuStrategia> strategiat = Arrays.asList(
                laskentaStrategia,
                valintaperusteetStrategia,
                hakemuksetStrategia,
                hakijaryhmatStrategia,
                suoritusrekisteriStrategia
        );

        final List<ValintaryhmaPalvelukutsuYhdiste> valintaryhmaPalvelukutsuYhdiste = Lists.newArrayList();
        final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<HakemuksetPalvelukutsu>> hakemuksetPalvelukutsut = Lists.newArrayList();
        final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<ValintaperusteetPalvelukutsu>> valintaperusteetPalvelukutsut = Lists.newArrayList();
        final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<HakijaryhmatPalvelukutsu>> hakijaryhmatPalvelukutsut = Lists.newArrayList();
        final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<SuoritusrekisteriPalvelukutsu>> suoritusrekisteriPalvelukutsut = Lists.newArrayList();
        actorParams.getHakukohdeOids().forEach(hk -> {
            UuidHakukohdeJaOrganisaatio uudiHk = new UuidHakukohdeJaOrganisaatio(actorParams.getUuid(), hk);
            ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu = new ValintaperusteetPalvelukutsu(uudiHk, actorParams.getValinnanvaihe(), valintaperusteetAsyncResource);
            HakemuksetPalvelukutsu hakemuksetPalvelukutsu = new HakemuksetPalvelukutsu(actorParams.getHakuOid(), uudiHk, applicationAsyncResource);
            SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsu = new SuoritusrekisteriPalvelukutsu(uudiHk, suoritusrekisteriAsyncResource);
            HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu = new HakijaryhmatPalvelukutsu(uudiHk, valintaperusteetAsyncResource);
            hakemuksetPalvelukutsut.add(new PalvelukutsuJaPalvelukutsuStrategiaImpl<>(hakemuksetPalvelukutsu, hakemuksetStrategia));
            valintaperusteetPalvelukutsut.add(new PalvelukutsuJaPalvelukutsuStrategiaImpl<>(valintaperusteetPalvelukutsu, valintaperusteetStrategia));
            hakijaryhmatPalvelukutsut.add(new PalvelukutsuJaPalvelukutsuStrategiaImpl<>(hakijaryhmatPalvelukutsu, hakijaryhmatStrategia));
            suoritusrekisteriPalvelukutsut.add(new PalvelukutsuJaPalvelukutsuStrategiaImpl<>(suoritusrekisteriPalvelukutsu, suoritusrekisteriStrategia));
            valintaryhmaPalvelukutsuYhdiste.add(new ValintaryhmaPalvelukutsuYhdiste(
                    hk.getHakukohdeOid(),
                    hakemuksetPalvelukutsu,
                    valintaperusteetPalvelukutsu,
                    hakijaryhmatPalvelukutsu,
                    suoritusrekisteriPalvelukutsu
            ));
        });

        ValintaryhmatKatenoivaValintalaskentaPalvelukutsu laskentaPk = new ValintaryhmatKatenoivaValintalaskentaPalvelukutsu(
				haku,
                actorParams.getParametritDTO(),
                actorParams.isErillishaku(),
                new UuidHakukohdeJaOrganisaatio(actorParams.getUuid(), new HakukohdeJaOrganisaatio("Valintaryhmalaskenta(" + actorParams.getHakukohdeOids().size() + "kohdetta)", "kaikkiOrganisaatiot")),
                valintalaskentaAsyncResource,
                valintaryhmaPalvelukutsuYhdiste,
                hakemuksetPalvelukutsut,
                valintaperusteetPalvelukutsut,
                hakijaryhmatPalvelukutsut,
                suoritusrekisteriPalvelukutsut
        );

        ValintaryhmaLaskentaActorImpl v = new ValintaryhmaLaskentaActorImpl(
                laskentaSupervisor,
                actorParams.getUuid(),
                actorParams.getHakuOid(),
                laskentaPk,
                strategiat,
                laskentaStrategia,
                laskentaSeurantaAsyncResource
        );
        laskentaPk.setCallback(v);
        return v;
    }

    public LaskentaActor createValintakoelaskentaActor(LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final PalvelukutsuStrategia laskentaStrategia = createStrategia();
        final PalvelukutsuStrategia valintaperusteetStrategia = createStrategia();
        final PalvelukutsuStrategia hakemuksetStrategia = createStrategia();
        final PalvelukutsuStrategia suoritusrekisteriStrategia = createStrategia();
        final Collection<PalvelukutsuStrategia> strategiat = Arrays.asList(
                laskentaStrategia,
                valintaperusteetStrategia,
                hakemuksetStrategia,
                suoritusrekisteriStrategia
        );
        final Collection<LaskentaPalvelukutsu> palvelukutsut = actorParams.getHakukohdeOids()
                .parallelStream()
                .map(hakukohdeOid -> {
                    UuidHakukohdeJaOrganisaatio uudiHk = new UuidHakukohdeJaOrganisaatio(actorParams.getUuid(), hakukohdeOid);
                    return new ValintakoelaskentaPalvelukutsu(
							haku,
                            actorParams.getParametritDTO(),
                            actorParams.isErillishaku(),
                            uudiHk,
                            valintalaskentaAsyncResource,
                            new HakemuksetPalvelukutsu(actorParams.getHakuOid(), uudiHk, applicationAsyncResource),
                            new ValintaperusteetPalvelukutsu(uudiHk, actorParams.getValinnanvaihe(), valintaperusteetAsyncResource),
                            new SuoritusrekisteriPalvelukutsu(uudiHk, suoritusrekisteriAsyncResource),
                            hakemuksetStrategia, valintaperusteetStrategia,
                            suoritusrekisteriStrategia
                    );
                })
                .collect(Collectors.toList());
        return new LaskentaActorImpl(laskentaSupervisor, actorParams.getUuid(), actorParams.getHakuOid(), palvelukutsut, strategiat, laskentaStrategia, laskentaSeurantaAsyncResource);
    }

    public LaskentaActor createValintalaskentaActor(LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final PalvelukutsuStrategia laskentaStrategia = createStrategia();
        final PalvelukutsuStrategia valintaperusteetStrategia = createStrategia();
        final PalvelukutsuStrategia hakemuksetStrategia = createStrategia();
        final PalvelukutsuStrategia hakijaryhmatStrategia = createStrategia();
        final PalvelukutsuStrategia suoritusrekisteriStrategia = createStrategia();
        final Collection<PalvelukutsuStrategia> strategiat = Arrays.asList(
                laskentaStrategia,
                valintaperusteetStrategia,
                hakemuksetStrategia,
                hakijaryhmatStrategia,
                suoritusrekisteriStrategia
        );
        final Collection<LaskentaPalvelukutsu> palvelukutsut = actorParams.getHakukohdeOids()
                .parallelStream()
                .map(hakukohdeOid -> {
                    UuidHakukohdeJaOrganisaatio uudiHk = new UuidHakukohdeJaOrganisaatio(actorParams.getUuid(), hakukohdeOid);
                    return new ValintalaskentaPalvelukutsu(
                            haku,
                            actorParams.getParametritDTO(),
                            actorParams.isErillishaku(),
                            uudiHk,
                            valintalaskentaAsyncResource,
                            new HakemuksetPalvelukutsu(actorParams.getHakuOid(), uudiHk, applicationAsyncResource),
                            new ValintaperusteetPalvelukutsu(uudiHk, actorParams.getValinnanvaihe(), valintaperusteetAsyncResource),
                            new HakijaryhmatPalvelukutsu(uudiHk, valintaperusteetAsyncResource),
                            new SuoritusrekisteriPalvelukutsu(uudiHk, suoritusrekisteriAsyncResource),
                            hakemuksetStrategia,
                            valintaperusteetStrategia,
                            hakijaryhmatStrategia,
                            suoritusrekisteriStrategia
                    );
                })
                .collect(Collectors.toList());
        return new LaskentaActorImpl(laskentaSupervisor, actorParams.getUuid(), actorParams.getHakuOid(), palvelukutsut, strategiat, laskentaStrategia, laskentaSeurantaAsyncResource);
    }

    public LaskentaActor createValintalaskentaJaValintakoelaskentaActor(LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        final PalvelukutsuStrategia laskentaStrategia = createStrategia();
        final PalvelukutsuStrategia valintaperusteetStrategia = createStrategia();
        final PalvelukutsuStrategia hakemuksetStrategia = createStrategia();
        final PalvelukutsuStrategia hakijaryhmatStrategia = createStrategia();
        final PalvelukutsuStrategia suoritusrekisteriStrategia = createStrategia();
        final Collection<PalvelukutsuStrategia> strategiat = Arrays.asList(
                laskentaStrategia,
                valintaperusteetStrategia,
                hakemuksetStrategia,
                hakijaryhmatStrategia,
                suoritusrekisteriStrategia
        );
        final Collection<LaskentaPalvelukutsu> palvelukutsut = actorParams.getHakukohdeOids()
                .parallelStream()
                .map(hakukohdeOid -> {
                            UuidHakukohdeJaOrganisaatio uudiHk = new UuidHakukohdeJaOrganisaatio(actorParams.getUuid(), hakukohdeOid);
                            return new ValintalaskentaJaValintakoelaskentaPalvelukutsu(
                                    haku,
                                    actorParams.getParametritDTO(),
                                    actorParams.isErillishaku(),
                                    uudiHk,
                                    valintalaskentaAsyncResource,
                                    new HakemuksetPalvelukutsu(actorParams.getHakuOid(), uudiHk, applicationAsyncResource),
                                    new ValintaperusteetPalvelukutsu(uudiHk, actorParams.getValinnanvaihe(), valintaperusteetAsyncResource),
                                    new HakijaryhmatPalvelukutsu(uudiHk, valintaperusteetAsyncResource),
                                    new SuoritusrekisteriPalvelukutsu(uudiHk, suoritusrekisteriAsyncResource),
                                    hakemuksetStrategia,
                                    valintaperusteetStrategia,
                                    hakijaryhmatStrategia,
                                    suoritusrekisteriStrategia
                            );
                        }
                )
                .collect(Collectors.toList());
        return new LaskentaActorImpl(laskentaSupervisor, actorParams.getUuid(), actorParams.getHakuOid(), palvelukutsut, strategiat, laskentaStrategia, laskentaSeurantaAsyncResource);
    }

    private PalvelukutsuStrategia createStrategia() {
        return new YksiPalvelukutsuKerrallaPalvelukutsuStrategia();
    }

    public LaskentaActor createLaskentaActor(LaskentaSupervisor laskentaSupervisor, HakuV1RDTO haku, LaskentaActorParams actorParams) {
        if (LaskentaTyyppi.VALINTARYHMALASKENTA.equals(actorParams.getLaskentaTyyppi())) {
            LOG.info("Muodostetaan VALINTARYHMALASKENTA");
            return createValintaryhmaActor(laskentaSupervisor, haku, actorParams);
        }
        if (LaskentaTyyppi.VALINTAKOELASKENTA.equals(actorParams.getLaskentaTyyppi())) {
            LOG.info("Muodostetaan VALINTAKOELASKENTA");
            return createValintakoelaskentaActor(laskentaSupervisor, haku, actorParams);
        }
        if (LaskentaTyyppi.VALINTALASKENTA.equals(actorParams.getLaskentaTyyppi())) {
            LOG.info("Muodostetaan VALINTALASKENTA");
            return createValintalaskentaActor(laskentaSupervisor, haku, actorParams);
        }
        LOG.info("Muodostetaan KAIKKI VAIHEET LASKENTA koska valinnanvaihe oli {} ja valintakoelaskenta ehto {}", actorParams.getValinnanvaihe(), actorParams.isValintakoelaskenta());
        return createValintalaskentaJaValintakoelaskentaActor(laskentaSupervisor, haku, actorParams);
    }
}
