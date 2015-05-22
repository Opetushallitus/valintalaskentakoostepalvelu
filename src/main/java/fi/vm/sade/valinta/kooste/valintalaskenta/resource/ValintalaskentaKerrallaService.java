package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.*;
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

    public ValintalaskentaKerrallaService() {}

    public void kaynnistaLaskentaHaulle(LaskentaParams laskentaParams, Consumer<Response> callback) {
        String hakuOid = laskentaParams.getHakuOid();
        Maski maski = laskentaParams.getMaski();

        if (StringUtils.isBlank(hakuOid)) {
            LOG.error("HakuOid on pakollinen");
            throw new RuntimeException("HakuOid on pakollinen");
        }

        Optional<String> uuidForExistingNonMaskedLaskenta = uuidForExistingNonMaskedLaskenta(laskentaParams.getMaski(), hakuOid);
        if (uuidForExistingNonMaskedLaskenta.isPresent()) {
            returnExistingLaskenta(uuidForExistingNonMaskedLaskenta.get(), callback);
        } else {
            LOG.info("Aloitetaan laskenta haulle {}", hakuOid);
            valintaperusteetAsyncResource.haunHakukohteet(
                    hakuOid,
                    (List<HakukohdeViiteDTO> hakukohdeViitteet) -> {
                        List<HakukohdeJaOrganisaatio> haunHakukohteetOids = kasitteleHakukohdeViitteet(
                                hakukohdeViitteet,
                                hakuOid,
                                callback
                        );
                        createLaskenta(
                                haunHakukohteetOids,
                                (String uuid) -> notifyWorkAvailable(
                                        haunHakukohteetOids,
                                        maski,
                                        uuid,
                                        callback
                                ),
                                laskentaParams,
                                callback
                        );
                    },
                    (Throwable poikkeus) -> callback.accept(errorResponse(poikkeus.getMessage()))
            );
        }
    }

    public void kaynnistaLaskentaUudelleen(final String uuid, final Consumer<Response> callbackResponse) {
        try {
            final Laskenta l = valintalaskentaValvomo.fetchLaskenta(uuid);
            if (l != null && !l.isValmis()) {
                LOG.warn("Laskenta {} on viela ajossa, joten palautetaan linkki siihen.", uuid);
                callbackResponse.accept(redirectResponse(uuid));
            }
            seurantaAsyncResource.resetoiTilat(
                    uuid,
                    (LaskentaDto laskenta) -> valintaperusteetAsyncResource.haunHakukohteet(
                            laskenta.getHakuOid(),
                            (List<HakukohdeViiteDTO> hakukohdeViitteet) -> {
                                List<HakukohdeJaOrganisaatio> haunHakukohteetOids = kasitteleHakukohdeViitteet(
                                        hakukohdeViitteet,
                                        laskenta.getHakuOid(),
                                        callbackResponse
                                );
                                notifyWorkAvailable(
                                        haunHakukohteetOids,
                                        createMaskiFrom(laskenta),
                                        laskenta.getUuid(),
                                        callbackResponse
                                );
                            },
                            (Throwable poikkeus) -> callbackResponse.accept(errorResponse(poikkeus.getMessage()))
                    ),
                    (Throwable t) -> {
                        LOG.error("Laskennan uudelleenajo epäonnistui. Uuid: " + uuid , t);
                        callbackResponse.accept(errorResponse("Uudelleen ajo laskennalle heitti poikkeuksen!"));
                    });
        } catch (Throwable t) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe", t);
            callbackResponse.accept(errorResponse("Odottamaton virhe laskennan kaynnistamisessa! " + t.getMessage()));
            throw t;
        }
    }

    private Optional<Laskenta> haeAjossaOlevaLaskentaHaulle(final String hakuOid) {
        return valintalaskentaValvomo
                .runningLaskentas()
                .stream()
                .filter(l -> hakuOid.equals(l.getHakuOid()) && !l.isOsittainenLaskenta())
                .findFirst();
    }

    private List<HakukohdeJaOrganisaatio> kasitteleHakukohdeViitteet(
            final List<HakukohdeViiteDTO> hakukohdeViitteet,
            final String hakuOid,
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
        if (haunHakukohdeOids.isEmpty()) {
            String msg = "Haulla " + hakuOid + " ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?";
            LOG.error(msg);
            callback.accept(errorResponse(msg));
            throw new RuntimeException(msg);
        } else {
            return haunHakukohdeOids;
        }
    }

    private void notifyWorkAvailable(
            final Collection<HakukohdeJaOrganisaatio> haunHakukohteetOids,
            final Maski maski,
            final String uuid,
            final Consumer<Response> callbackResponse
    ) {
        Collection<HakukohdeJaOrganisaatio> oids = maski.isMask() ? maski.maskaa(haunHakukohteetOids) : haunHakukohteetOids;
        if (!oids.isEmpty() && uuid != null) {
            valintalaskentaRoute.workAvailable();
            callbackResponse.accept(redirectResponse(uuid));
        } else {
            String cause = oids.isEmpty() ? "Haulla ei ole " + (maski.isMask() ? "maskin jalkeen " : "") + "hakukohteita." : "Uuid:ta ei ole";
            throw new RuntimeException("Laskentaa ei voida aloittaa: " + cause);
        }
    }

    private void createLaskenta(Collection<HakukohdeJaOrganisaatio> hakukohdeData, Consumer<String> laskennanAloitus, LaskentaParams laskentaParams, Consumer<Response> callbackResponse) {
        final List<HakukohdeDto> hakukohdeDtos = toHakukohdeDto(hakukohdeData);
        validateHakukohdeDtos(hakukohdeData, hakukohdeDtos, callbackResponse);

        seurantaAsyncResource.luoLaskenta(
                laskentaParams,
                hakukohdeDtos,
                (String uuid) -> laskennanAloitus.accept(uuid),
                (Throwable t) -> {
                    LOG.error("Seurannasta uuden laskennan haku paatyi virheeseen", t);
                    callbackResponse.accept(errorResponse(t.getMessage()));
                });
    }

    private void validateHakukohdeDtos(Collection<HakukohdeJaOrganisaatio> hakukohdeData, List<HakukohdeDto> hakukohdeDtos, Consumer<Response> callbackResponse) {
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

    private Response redirectResponse(final String target) {
        return Response.ok(Vastaus.uudelleenOhjaus(target)).build();
    }

    private Response errorResponse(final String errorMessage){
        return Response.serverError().entity(errorMessage).build();
    }

    private Maski createMaskiFrom(final LaskentaDto laskenta) {
        final List<String> hakukohdeOids = laskenta.getHakukohteet().stream()
                .filter(h -> !HakukohdeTila.VALMIS.equals(h.getTila()))
                .map(HakukohdeDto::getHakukohdeOid)
                .collect(Collectors.toList());
        return new Maski(true, hakukohdeOids);
    }

    private List<HakukohdeDto> toHakukohdeDto(Collection<HakukohdeJaOrganisaatio> hakukohdeData) {
        return hakukohdeData.stream()
                .filter(Objects::nonNull)
                .filter(hk -> hk.getHakukohdeOid() != null)
                .filter(hk -> hk.getOrganisaatioOid() != null)
                .map(hk -> new HakukohdeDto(hk.getHakukohdeOid(), hk.getOrganisaatioOid()))
                .collect(Collectors.toList());
    }

    private Optional<String> uuidForExistingNonMaskedLaskenta(Maski maski, String hakuOid) {
        final Optional<Laskenta> ajossaOlevaLaskentaHaulle = !maski.isMask() ? haeAjossaOlevaLaskentaHaulle(hakuOid) : Optional.<Laskenta>empty();
        return ajossaOlevaLaskentaHaulle.isPresent() ? Optional.of(ajossaOlevaLaskentaHaulle.get().getUuid()) : Optional.<String>empty();
    }

    private void returnExistingLaskenta(String uuid, Consumer<Response> callback) {
        LOG.warn("Laskenta on jo kaynnissa haulle {} joten palautetaan seurantatunnus({}) ajossa olevaan hakuun", uuid, uuid);
        callback.accept(redirectResponse(uuid));
    }
}
