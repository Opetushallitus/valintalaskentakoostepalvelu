package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaStartParams;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Service
public class LaskentaStarter {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaStarter.class);

    private final OhjausparametritAsyncResource ohjausparametritAsyncResource;
    private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    private final LaskentaSeurantaAsyncResource seurantaAsyncResource;

    @Autowired
    public LaskentaStarter(
            OhjausparametritAsyncResource ohjausparametritAsyncResource,
            ValintaperusteetAsyncResource valintaperusteetAsyncResource,
            LaskentaSeurantaAsyncResource seurantaAsyncResource) {
        this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
        this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
        this.seurantaAsyncResource = seurantaAsyncResource;
    }

    public void fetchLaskentaParams(final String uuid, final Consumer<LaskentaActorParams> actorParamsCallback) {
        seurantaAsyncResource.laskenta(
                uuid,
                (LaskentaDto laskenta) -> haunHakukohteet(laskenta, actorParamsCallback),
                (Throwable t) -> {
                    LOG.error("Laskennan haku epäonnistui {}:\r\n{}", t.getMessage(), Arrays.toString(t.getStackTrace()));
                    actorParamsCallback.accept(null);
                }
        );
    }

    private void kasitteleHaunkohteetOids(
            final LaskentaDto laskenta,
            final Collection<HakukohdeJaOrganisaatio> haunHakukohteetOids,
            final Consumer<LaskentaActorParams> actorParamsCallback
    ) {
        final Maski maski = luoMaskiLaskennanPohjalta(laskenta);
        final String hakuOid = laskenta.getHakuOid();

        Collection<HakukohdeJaOrganisaatio> oids = maski.isMask() ? maski.maskaa(haunHakukohteetOids) : haunHakukohteetOids;
        if (oids.isEmpty()) {
            LOG.error("Hakukohdemaskauksen jalkeen haulla ei ole hakukohteita! Ei voida aloittaa laskentaa ilman hakukohteita.");
            actorParamsCallback.accept(null);
            return;
        }

        ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid, parametrit -> {
                        actorParamsCallback.accept(
                                new LaskentaActorParams(
                                        new LaskentaStartParams(
                                                laskenta.getUuid(),
                                                hakuOid,
                                                laskenta.isErillishaku(),
                                                maski.isMask(),
                                                LaskentaTyyppi.VALINTARYHMA.equals(laskenta.getTyyppi()),
                                                laskenta.getValinnanvaihe(),
                                                laskenta.getValintakoelaskenta(),
                                                oids,
                                                laskenta.getTyyppi()
                                        ),
                                        parametrit)
                        );
                },
                poikkeus -> {
                    LOG.error("Ohjausparametrien luku epäonnistui: {} {}", poikkeus.getMessage(), Arrays.toString(poikkeus.getStackTrace()));
                    actorParamsCallback.accept(null);
                });
    }

    private void haunHakukohteet(final LaskentaDto laskenta, final Consumer<LaskentaActorParams> actorParamsCallback) {
        String hakuOid = laskenta.getHakuOid();
        if (StringUtils.isBlank(hakuOid)) {
            LOG.error("Yritettiin hakea hakukohteita ilman hakuOidia!");
            throw new RuntimeException("Yritettiin hakea hakukohteita ilman hakuOidia!");
        }
        valintaperusteetAsyncResource.haunHakukohteet(
                hakuOid,
                (List<HakukohdeViiteDTO> hakukohdeViitteet) -> kasitteleHakukohdeViitteet(
                        laskenta,
                        hakukohdeViitteet,
                        actorParamsCallback
                ),
                (Throwable t) -> {
                    LOG.error("Haun kohteiden haku epäonnistui haulle: {}", hakuOid);
                    actorParamsCallback.accept(null);
                }
        );
    }

    private void kasitteleHakukohdeViitteet(
            final LaskentaDto laskenta,
            final List<HakukohdeViiteDTO> hakukohdeViitteet,
            final Consumer<LaskentaActorParams> actorParamsCallback
    ) {
        final String hakuOid = laskenta.getHakuOid();
        LOG.info("Tarkastellaan hakukohdeviitteita haulle {}", hakuOid);

        if (hakukohdeViitteet == null || hakukohdeViitteet.isEmpty()) {
            LOG.error("Valintaperusteet palautti tyhjat hakukohdeviitteet haulle {}!", hakuOid);
            actorParamsCallback.accept(null);
            return;
        }
        final List<HakukohdeJaOrganisaatio> haunHakukohdeOidit = filteroiHakukohteet(hakuOid, hakukohdeViitteet);
        if (haunHakukohdeOidit.isEmpty()) {
            LOG.error("Haulla {} ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?", hakuOid);
            actorParamsCallback.accept(null);
        } else {
            kasitteleHaunkohteetOids(laskenta, haunHakukohdeOidit, actorParamsCallback);
        }
    }

    private List<HakukohdeJaOrganisaatio> filteroiHakukohteet(final String hakuOid, final List<HakukohdeViiteDTO> hakukohdeViitteet) {
        return hakukohdeViitteet.stream()
                .filter(Objects::nonNull)
                .filter(h -> {
                    if (h == null) {
                        LOG.error("nonNull filteri ei toimi!");
                        return false;
                    }
                    if (h.getOid() == null) {
                        LOG.error("Hakukohdeviitteen oid oli null haussa {}", hakuOid);
                        return false;
                    }
                    if (h.getTila() == null) {
                        LOG.error("Hakukohdeviitteen tila oli null hakukohteelle {}", h.getOid());
                        return false;
                    }
                    if (!"JULKAISTU".equals(h.getTila())) {
                        LOG.warn("Ohitetaan hakukohde {} koska sen tila on {}.", h.getOid(), h.getTila());
                        return false;
                    }
                    return true;
                })
                .map(u -> new HakukohdeJaOrganisaatio(u.getOid(), u.getTarjoajaOid()))
                .collect(Collectors.toList());
    }

    private Maski luoMaskiLaskennanPohjalta(final LaskentaDto laskenta) {
        final List<HakukohdeJaOrganisaatio> hakukohdeMaski = laskenta.getHakukohteet().stream()
                .filter(h -> !HakukohdeTila.VALMIS.equals(h.getTila()))
                .map(h -> new HakukohdeJaOrganisaatio(h.getHakukohdeOid(), h.getOrganisaatioOid()))
                .collect(Collectors.toList());
        return new Maski(
                true,
                hakukohdeMaski.stream()
                        .map(HakukohdeJaOrganisaatio::getHakukohdeOid)
                        .collect(Collectors.toList())
        );
    }
}
