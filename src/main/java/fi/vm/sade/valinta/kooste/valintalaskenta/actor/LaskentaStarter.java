package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import akka.actor.ActorRef;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskentaStarterActor;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaStartParams;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
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

    public void fetchLaskentaParams(ActorRef laskennanKaynnistajaActor, final String uuid, final Consumer<LaskentaActorParams> actorParamsCallback) {
        seurantaAsyncResource.laskenta(
                uuid,
                (LaskentaDto laskenta) -> {
                    String hakuOid = laskenta.getHakuOid();
                    if (StringUtils.isBlank(hakuOid)) {
                        LOG.error("Yritettiin hakea hakukohteita ilman hakuOidia!");
                        throw new RuntimeException("Yritettiin hakea hakukohteita ilman hakuOidia!");
                    }
                    valintaperusteetAsyncResource.haunHakukohteet(
                            hakuOid,
                            (List<HakukohdeViiteDTO> hakukohdeViitteet) -> haunOhjausParametrit(
                                    laskennanKaynnistajaActor,
                                    hakuOid,
                                    hakukohdeViitteet,
                                    laskenta,
                                    actorParamsCallback
                            ),
                            (Throwable t) -> cancelLaskenta(laskennanKaynnistajaActor, "Haun kohteiden haku epäonnistui haulle: " + uuid, uuid)
                    );
                },
                (Throwable t) -> cancelLaskenta(laskennanKaynnistajaActor, "Laskennan haku epäonnistui " + t.getMessage() + ": \r\n" + Arrays.toString(t.getStackTrace()), uuid)
        );
    }

    private void haunOhjausParametrit(ActorRef laskennankaynnistajaActor, String hakuOid, List<HakukohdeViiteDTO> hakukohdeViitteet, LaskentaDto laskenta, Consumer<LaskentaActorParams> actorParamsCallback) {
        LOG.info("Tarkastellaan hakukohdeviitteita haulle {}", hakuOid);

        final List<HakukohdeJaOrganisaatio> haunHakukohdeOidit = hakukohdeViitteet != null ? publishedNonNulltoHakukohdeJaOrganisaatio(hakukohdeViitteet) : new ArrayList<>();
        final Maski maski = createMaskiFromLaskenta(laskenta);

        Collection<HakukohdeJaOrganisaatio> oids = maski.isMask() ? maski.maskaa(haunHakukohdeOidit) : haunHakukohdeOidit;
        if (oids.isEmpty()) {
            cancelLaskenta(laskennankaynnistajaActor, "Haulla " + laskenta.getUuid() + " ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?", laskenta.getUuid());
        } else {
            ohjausparametritAsyncResource.haeHaunOhjausparametrit(
                    hakuOid,
                    parametrit -> actorParamsCallback.accept(laskentaActorParams(hakuOid, laskenta, oids, parametrit)),
                    (Throwable t) -> cancelLaskenta(laskennankaynnistajaActor, "Ohjausparametrien luku epäonnistui: " + t.getMessage() + " " + Arrays.toString(t.getStackTrace()), laskenta.getUuid())
            );
        }
    }

    private LaskentaActorParams laskentaActorParams(String hakuOid, LaskentaDto laskenta, Collection<HakukohdeJaOrganisaatio> haunHakukohdeOidit, ParametritDTO parametrit) {
        return new LaskentaActorParams(
                new LaskentaStartParams(
                        laskenta.getUuid(),
                        hakuOid,
                        laskenta.isErillishaku(),
                        true,
                        LaskentaTyyppi.VALINTARYHMA.equals(laskenta.getTyyppi()),
                        laskenta.getValinnanvaihe(),
                        laskenta.getValintakoelaskenta(),
                        haunHakukohdeOidit,
                        laskenta.getTyyppi()
                ),
                parametrit);
    }

    private List<HakukohdeJaOrganisaatio> publishedNonNulltoHakukohdeJaOrganisaatio(final List<HakukohdeViiteDTO> hakukohdeViitteet) {
        return hakukohdeViitteet.stream()
                .filter(Objects::nonNull)
                .filter(h -> h.getOid() != null)
                .filter(h -> h.getTila().equals("JULKAISTU"))
                .map(h -> new HakukohdeJaOrganisaatio(h.getOid(), h.getTarjoajaOid()))
                .collect(Collectors.toList());
    }

    private void cancelLaskenta(ActorRef laskennanKaynnistajaActor, String msg, String uuid) {
        LOG.error(msg);
        seurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.PERUUTETTU, HakukohdeTila.KESKEYTETTY);
        laskennanKaynnistajaActor.tell(LaskentaStarterActor.WorkerAvailable.class, ActorRef.noSender());
    }

    private Maski createMaskiFromLaskenta(final LaskentaDto laskenta) {
        final List<String> hakukohdeOids = laskenta.getHakukohteet().stream()
                .filter(h -> !HakukohdeTila.VALMIS.equals(h.getTila()))
                .map(h -> new HakukohdeJaOrganisaatio(h.getHakukohdeOid(), h.getOrganisaatioOid()))
                .map(HakukohdeJaOrganisaatio::getHakukohdeOid)
                .collect(Collectors.toList());

        return Maski.whitelist(hakukohdeOids);
    }
}
