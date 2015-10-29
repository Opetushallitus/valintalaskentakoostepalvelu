package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import akka.actor.ActorRef;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.function.SynkronoituLaskuri;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


@Service
public class LaskentaStarter {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaStarter.class);

    private final OhjausparametritAsyncResource ohjausparametritAsyncResource;
    private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    private final LaskentaSeurantaAsyncResource seurantaAsyncResource;
    private final TarjontaAsyncResource tarjontaAsyncResource;

    @Autowired
    public LaskentaStarter(
            OhjausparametritAsyncResource ohjausparametritAsyncResource,
            ValintaperusteetAsyncResource valintaperusteetAsyncResource,
            LaskentaSeurantaAsyncResource seurantaAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource
    ) {
        this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
        this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
        this.seurantaAsyncResource = seurantaAsyncResource;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
    }

    public void fetchLaskentaParams(ActorRef laskennanKaynnistajaActor, final String uuid, final BiConsumer<HakuV1RDTO, LaskentaActorParams> startActor) {
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
                            (List<HakukohdeViiteDTO> hakukohdeViitteet) -> {
                                Collection<HakukohdeJaOrganisaatio> hakukohdeOids = maskHakukohteet(hakuOid, hakukohdeViitteet, laskenta);
                                if (!hakukohdeOids.isEmpty()) {
                                    fetchHakuInformation(laskennanKaynnistajaActor, hakuOid, hakukohdeOids, laskenta, startActor);
                                } else {
                                    cancelLaskenta(laskennanKaynnistajaActor, "Haulla " + laskenta.getUuid() + " ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?", null, uuid);
                                }
                            },
                            (Throwable t) -> cancelLaskenta(laskennanKaynnistajaActor, "Haun kohteiden haku epäonnistui haulle: " + uuid, Optional.empty(), uuid)
                    );
                },
                (Throwable t) -> cancelLaskenta(laskennanKaynnistajaActor, "Laskennan haku epäonnistui ", Optional.of(t), uuid)
        );
    }

    private static Collection<HakukohdeJaOrganisaatio> maskHakukohteet(String hakuOid, List<HakukohdeViiteDTO> hakukohdeViitteet, LaskentaDto laskenta) {
        LOG.info("Tarkastellaan hakukohdeviitteita haulle {}", hakuOid);

        final List<HakukohdeJaOrganisaatio> haunHakukohdeOidit = hakukohdeViitteet != null ? publishedNonNulltoHakukohdeJaOrganisaatio(hakukohdeViitteet) : new ArrayList<>();
        final Maski maski = createMaskiFromLaskenta(laskenta);

        return maski.maskaa(haunHakukohdeOidit);
    }

    private void fetchHakuInformation(ActorRef laskennankaynnistajaActor, String hakuOid, Collection<HakukohdeJaOrganisaatio> haunHakukohdeOidit, LaskentaDto laskenta, BiConsumer<HakuV1RDTO, LaskentaActorParams> startActor) {
        AtomicReference<HakuV1RDTO> hakuRef = new AtomicReference<>();
        AtomicReference<LaskentaActorParams> parametritRef = new AtomicReference<>();
        SynkronoituLaskuri counter = SynkronoituLaskuri.builder()
                .setLaskurinAlkuarvo(2)
                .setSynkronoituToiminto(
                        () -> startActor.accept(hakuRef.get(), parametritRef.get()))
                .build();
        tarjontaAsyncResource.haeHaku(hakuOid).subscribe(
                haku -> {
                    hakuRef.set(haku);
                    counter.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                },
                (Throwable t) -> cancelLaskenta(laskennankaynnistajaActor, "Tarjontatietojen haku epäonnistui: ", Optional.of(t), laskenta.getUuid())
        );

        ohjausparametritAsyncResource.haeHaunOhjausparametrit(
                hakuOid,
                parametrit -> {
                    parametritRef.set(laskentaActorParams(hakuOid, laskenta, haunHakukohdeOidit, parametrit));
                    counter.vahennaLaskuriaJaJosValmisNiinSuoritaToiminto();
                },
                (Throwable t) -> cancelLaskenta(laskennankaynnistajaActor, "Ohjausparametrien luku epäonnistui: ", Optional.of(t), laskenta.getUuid())
        );
    }

    private static LaskentaActorParams laskentaActorParams(String hakuOid, LaskentaDto laskenta, Collection<HakukohdeJaOrganisaatio> haunHakukohdeOidit, ParametritDTO parametrit) {
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

    private static List<HakukohdeJaOrganisaatio> publishedNonNulltoHakukohdeJaOrganisaatio(final List<HakukohdeViiteDTO> hakukohdeViitteet) {
        return hakukohdeViitteet.stream()
                .filter(Objects::nonNull)
                .filter(h -> h.getOid() != null)
                .filter(h -> h.getTila().equals("JULKAISTU"))
                .map(h -> new HakukohdeJaOrganisaatio(h.getOid(), h.getTarjoajaOid()))
                .collect(Collectors.toList());
    }

    private void cancelLaskenta(ActorRef laskennanKaynnistajaActor, String msg, Optional<Throwable> t,  String uuid) {
        if (t.isPresent()) LOG.error(msg, t);
        else LOG.error(msg);
        seurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.VALMIS, HakukohdeTila.KESKEYTETTY);
        laskennanKaynnistajaActor.tell(new LaskentaStarterActor.WorkerAvailable(), ActorRef.noSender());
    }

    private static Maski createMaskiFromLaskenta(final LaskentaDto laskenta) {
        final List<String> hakukohdeOids = laskenta.getHakukohteet().stream()
                .filter(h -> !HakukohdeTila.VALMIS.equals(h.getTila()))
                .map(h -> new HakukohdeJaOrganisaatio(h.getHakukohdeOid(), h.getOrganisaatioOid()))
                .map(HakukohdeJaOrganisaatio::getHakukohdeOid)
                .collect(Collectors.toList());

        return Maski.whitelist(hakukohdeOids);
    }
}
