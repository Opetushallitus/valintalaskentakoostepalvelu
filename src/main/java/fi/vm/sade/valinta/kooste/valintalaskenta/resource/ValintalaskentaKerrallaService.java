package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaInfo;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.TunnisteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ValintalaskentaKerrallaService {
    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaKerrallaService.class);

    @Autowired
    private ValintalaskentaKerrallaRouteValvomo valintalaskentaValvomo;
    @Autowired
    private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    @Autowired
    private ValintalaskentaKerrallaRoute valintalaskentaRoute;
    @Autowired
    private LaskentaSeurantaAsyncResource seurantaAsyncResource;

    public ValintalaskentaKerrallaService() {
    }

    public void kaynnistaLaskentaHaulle(LaskentaParams laskentaParams, Consumer<Response> callback) {
        String hakuOid = laskentaParams.getHakuOid();
        Optional<String> uuidForExistingNonMaskedLaskenta = uuidForExistingNonMaskedLaskenta(laskentaParams.getMaski(), hakuOid);

        if (uuidForExistingNonMaskedLaskenta.isPresent()) {
            String uuid = uuidForExistingNonMaskedLaskenta.get();
            LOG.warn("Laskenta on jo kaynnissa haulle {} joten palautetaan seurantatunnus({}) ajossa olevaan hakuun", uuid, uuid);
            callback.accept(redirectResponse(new TunnisteDto(uuid, false)));
        } else {
            LOG.info("Aloitetaan laskenta haulle {}", hakuOid);
            valintaperusteetAsyncResource.haunHakukohteet(hakuOid,
                    (List<HakukohdeViiteDTO> hakukohdeViitteet) -> {
                        Collection<HakukohdeJaOrganisaatio> haunHakukohteetOids = kasitteleHakukohdeViitteet(hakukohdeViitteet, hakuOid, laskentaParams.getMaski(), callback);
                        createLaskenta(haunHakukohteetOids, (TunnisteDto uuid) -> notifyWorkAvailable(uuid, callback), laskentaParams, callback);
                    },
                    (Throwable poikkeus) -> {
                        LOG.error("kaynnistaLaskentaHaulle throws", poikkeus);
                        callback.accept(errorResponse(poikkeus.getMessage()));
                    }
            );
        }
    }

    public void kaynnistaLaskentaUudelleen(final String uuid, final Consumer<Response> callbackResponse) {
        try {
            final Laskenta l = valintalaskentaValvomo.fetchLaskenta(uuid);
            if (l != null && !l.isValmis()) {
                LOG.warn("Laskenta {} on viela ajossa, joten palautetaan linkki siihen.", uuid);
                callbackResponse.accept(redirectResponse(new TunnisteDto(uuid,false)));
            }
            seurantaAsyncResource.resetoiTilat(
                    uuid,
                    (LaskentaDto laskenta) -> valintaperusteetAsyncResource.haunHakukohteet(laskenta.getHakuOid(),
                            (List<HakukohdeViiteDTO> hakukohdeViitteet) -> notifyWorkAvailable(new TunnisteDto(laskenta.getUuid(), laskenta.getLuotiinkoUusiLaskenta()), callbackResponse),
                            (Throwable poikkeus) -> {
                                LOG.error("seurantaAsyncResource throws", poikkeus);
                                callbackResponse.accept(errorResponse(poikkeus.getMessage()));
                            }
                    ),
                    (Throwable t) -> {
                        LOG.error("Laskennan uudelleenajo epäonnistui. Uuid: " + uuid, t);
                        callbackResponse.accept(errorResponse("Uudelleen ajo laskennalle heitti poikkeuksen!"));
                    });
        } catch (Throwable t) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe", t);
            callbackResponse.accept(errorResponse("Odottamaton virhe laskennan kaynnistamisessa! " + t.getMessage()));
            throw t;
        }
    }

    private Optional<Laskenta> haeAjossaOlevaLaskentaHaulle(final String hakuOid) {
        return valintalaskentaValvomo.runningLaskentas().stream()
                .filter(l -> hakuOid.equals(l.getHakuOid()) && !l.isOsittainenLaskenta())
                .findFirst();
    }

    private static Collection<HakukohdeJaOrganisaatio> kasitteleHakukohdeViitteet(
            final List<HakukohdeViiteDTO> hakukohdeViitteet,
            final String hakuOid,
            final Optional<Maski> maski,
            final Consumer<Response> callback
    ) {
        LOG.info("Tarkastellaan hakukohdeviitteita haulle {}", hakuOid);

        if (hakukohdeViitteet == null || hakukohdeViitteet.isEmpty()) {
            LOG.error("Valintaperusteet palautti tyhjat hakukohdeviitteet haulle {}!", hakuOid);
            throw new NullPointerException("Valintaperusteet palautti tyhjat hakukohdeviitteet!");
        }
        final List<HakukohdeJaOrganisaatio> haunHakukohdeOids = hakukohdeViitteet.stream()
                .filter(Objects::nonNull)
                .filter(hakukohdeOid -> hakukohdeOid.getOid() != null)
                .filter(hakukohdeOid -> hakukohdeOid.getTila().equals("JULKAISTU"))
                .map(u -> new HakukohdeJaOrganisaatio(u.getOid(), u.getTarjoajaOid()))
                .collect(Collectors.toList());

        Collection<HakukohdeJaOrganisaatio> oids = maski.map(m -> m.maskaa(haunHakukohdeOids)).orElse(haunHakukohdeOids);
        if (oids.isEmpty()) {
            String msg = "Haulla " + hakuOid + " ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?";
            LOG.error(msg);
            callback.accept(errorResponse(msg));
            throw new RuntimeException(msg);
        } else {
            return oids;
        }
    }

    private void notifyWorkAvailable(final TunnisteDto uuid, final Consumer<Response> callbackResponse) {
        // ohitetaan ajossa olevan laskennan kaynnistaminen
        if(uuid.getLuotiinkoUusiLaskenta()) {
            valintalaskentaRoute.workAvailable();
        }
        callbackResponse.accept(redirectResponse(uuid));
    }

    private void createLaskenta(Collection<HakukohdeJaOrganisaatio> hakukohdeData, Consumer<TunnisteDto> laskennanAloitus, LaskentaParams laskentaParams, Consumer<Response> callbackResponse) {
        final List<HakukohdeDto> hakukohdeDtos = toHakukohdeDto(hakukohdeData);
        validateHakukohdeDtos(hakukohdeData, hakukohdeDtos, callbackResponse);
        seurantaAsyncResource.luoLaskenta(laskentaParams, hakukohdeDtos, (TunnisteDto uuid) -> laskennanAloitus.accept(uuid),
                (Throwable t) -> {
                    LOG.info("Seurannasta uuden laskennan haku paatyi virheeseen. Yritetään uudelleen.", t);
                    createLaskentaRetry(hakukohdeDtos, laskennanAloitus, laskentaParams, callbackResponse); //FIXME kill me OK-152!
                });
    }

    private void createLaskentaRetry(List<HakukohdeDto> hakukohdeDtos, Consumer<TunnisteDto> laskennanAloitus, LaskentaParams laskentaParams, Consumer<Response> callbackResponse) {
        seurantaAsyncResource.luoLaskenta(laskentaParams, hakukohdeDtos, (TunnisteDto uuid) -> laskennanAloitus.accept(uuid),
                (Throwable t) -> {
                    LOG.error("Seurannasta uuden laskennan haku paatyi virheeseen", t);
                    callbackResponse.accept(errorResponse(t.getMessage()));
                });
    }

    private static void validateHakukohdeDtos(Collection<HakukohdeJaOrganisaatio> hakukohdeData, List<HakukohdeDto> hakukohdeDtos, Consumer<Response> callbackResponse) {
        if (hakukohdeDtos.isEmpty()) {
            String msg = "Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!";
            LOG.error(msg);
            callbackResponse.accept(errorResponse(msg));
            throw new RuntimeException(msg);
        }
        if (hakukohdeDtos.size() < hakukohdeData.size()) {
            LOG.warn("Hakukohteita puuttuvien organisaatio-oidien vuoksi filtteroinnin jalkeen {}/{}!", hakukohdeDtos.size(), hakukohdeData.size());
        } else {
            LOG.info("Hakukohteita filtteroinnin jalkeen {}/{}!", hakukohdeDtos.size(), hakukohdeData.size());
        }
    }

    private static Response redirectResponse(final TunnisteDto target) {
        return Response.ok(Vastaus.laskennanSeuraus(target.getUuid(),target.getLuotiinkoUusiLaskenta())).build();
    }

    private static Response errorResponse(final String errorMessage) {
        return Response.serverError().entity(errorMessage).build();
    }

    private static List<HakukohdeDto> toHakukohdeDto(Collection<HakukohdeJaOrganisaatio> hakukohdeData) {
        return hakukohdeData.stream()
                .filter(Objects::nonNull)
                .filter(hk -> hk.getHakukohdeOid() != null)
                .filter(hk -> hk.getOrganisaatioOid() != null)
                .map(hk -> new HakukohdeDto(hk.getHakukohdeOid(), hk.getOrganisaatioOid()))
                .collect(Collectors.toList());
    }

    private Optional<String> uuidForExistingNonMaskedLaskenta(Optional<Maski> maski, String hakuOid) {
        final Optional<Laskenta> ajossaOlevaLaskentaHaulle = !maski.isPresent() || !maski.get().isMask() ? haeAjossaOlevaLaskentaHaulle(hakuOid) : Optional.<Laskenta>empty();
        return ajossaOlevaLaskentaHaulle.map(LaskentaInfo::getUuid);
    }
}
