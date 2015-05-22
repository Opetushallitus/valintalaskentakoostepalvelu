package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaStartParams;
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

    public void fetchLaskentaParams(final String uuid, final Consumer<LaskentaActorParams> actorParamsCallback) {
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
                                    hakuOid,
                                    hakukohdeViitteet,
                                    laskenta,
                                    actorParamsCallback
                            ),
                            (Throwable t) -> {
                                LOG.error("Haun kohteiden haku epäonnistui haulle: {}", hakuOid);
                                actorParamsCallback.accept(null);
                            }
                    );
                },
                (Throwable t) -> {
                    LOG.error("Laskennan haku epäonnistui {}:\r\n{}", t.getMessage(), Arrays.toString(t.getStackTrace()));
                    actorParamsCallback.accept(null);
                }
        );
    }

    private void haunOhjausParametrit(String hakuOid, List<HakukohdeViiteDTO> hakukohdeViitteet, LaskentaDto laskenta, Consumer<LaskentaActorParams> actorParamsCallback) {
        LOG.info("Tarkastellaan hakukohdeviitteita haulle {}", hakuOid);

        final List<HakukohdeJaOrganisaatio> haunHakukohdeOidit = hakukohdeViitteet != null ? publishedNonNulltoHakukohdeJaOrganisaatio(hakukohdeViitteet) : new ArrayList<>();
        if (haunHakukohdeOidit.isEmpty()) {
            LOG.error("Haulla {} ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?", hakuOid);
            seurantaAsyncResource.merkkaaLaskennanTila(laskenta.getUuid(), LaskentaTila.PERUUTETTU);
            actorParamsCallback.accept(null);
        }  else {
            ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid, parametrit -> {
                        actorParamsCallback.accept(
                                new LaskentaActorParams(
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
                                        parametrit)
                        );
                    },
                    poikkeus -> {
                        LOG.error("Ohjausparametrien luku epäonnistui: {} {}", poikkeus.getMessage(), Arrays.toString(poikkeus.getStackTrace()));
                        actorParamsCallback.accept(null);
                    });
        }
    }

    private List<HakukohdeJaOrganisaatio> publishedNonNulltoHakukohdeJaOrganisaatio(final List<HakukohdeViiteDTO> hakukohdeViitteet) {
        return hakukohdeViitteet.stream()
                .filter(Objects::nonNull)
                .filter(hakukohdeOid -> hakukohdeOid.getOid() != null)
                .filter(hakukohdeOid -> hakukohdeOid.getTila().equals("JULKAISTU"))
                .map(u -> new HakukohdeJaOrganisaatio(u.getOid(), u.getTarjoajaOid()))
                .collect(Collectors.toList());
    }
}
