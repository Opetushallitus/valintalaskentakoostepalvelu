package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LaskentaActorParams {
    private static final Integer HAE_KAIKKI_VALINNANVAIHEET = -1;

    private final LaskentaAloitus laskentaAloitus;
    private final ParametritDTO parametritDTO;
    private final Collection<HakukohdeJaOrganisaatio> hakukohdeOids;

    public LaskentaActorParams(LaskentaAloitus laskentaAloitus, ParametritDTO parametritDTO) {
        this.laskentaAloitus = laskentaAloitus;
        this.parametritDTO = parametritDTO;
        this.hakukohdeOids = laskentaAloitus.getHakukohdeDtos().stream()
                .map(hk -> new HakukohdeJaOrganisaatio(hk.getHakukohdeOid(), hk.getOrganisaatioOid()))
                .collect(Collectors.toList());
    }

    public String getUuid() {
        return laskentaAloitus.getUuid();
    }

    public String getHakuOid() {
        return laskentaAloitus.getHakuOid();
    }

    public ParametritDTO getParametritDTO() {
        return parametritDTO;
    }

    public boolean isErillishaku() {
        return laskentaAloitus.isErillishaku();
    }

    public Boolean isValintakoelaskenta() {
        return laskentaAloitus.getValintakoelaskenta();
    }

    /**
     * Tilapainen workaround resurssin valinnanvaiheen normalisointiin.
     */
    public Integer getValinnanvaihe() {
        return HAE_KAIKKI_VALINNANVAIHEET.equals(laskentaAloitus.getValinnanvaihe()) ? null : laskentaAloitus.getValinnanvaihe();
    }

    public Collection<HakukohdeJaOrganisaatio> getHakukohdeOids() {
        return hakukohdeOids;
    }

    /**
     * Tilapainen workaround resurssin valinnanvaiheen normalisointiin.
     */
    public LaskentaTyyppi getLaskentaTyyppi() {
        if (fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi.VALINTARYHMA.equals(laskentaAloitus.getTyyppi())) {
            return LaskentaTyyppi.VALINTARYHMALASKENTA;
        }
        if (Boolean.TRUE.equals(laskentaAloitus.getValintakoelaskenta())) {
            return LaskentaTyyppi.VALINTAKOELASKENTA;
        } else {
            if (laskentaAloitus.getValinnanvaihe() == null) {
                return LaskentaTyyppi.KAIKKI;
            } else {
                return LaskentaTyyppi.VALINTALASKENTA;
            }
        }
    }
}
