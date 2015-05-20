package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;

import javax.ws.rs.core.Response;
import java.util.function.Consumer;

public class LaskentaParams {
    private final LaskentaTyyppi laskentatyyppi;
    private final Boolean isValintakoelaskenta;
    private final Integer valinnanvaihe;
    private final String hakuOid;
    private final Maski maski;
    private final boolean isErillishaku;

    public LaskentaParams(LaskentaTyyppi laskentatyyppi, Boolean isValintakoelaskenta, Integer valinnanvaihe, String hakuOid, Maski maski, boolean isErillishaku) {
        this.laskentatyyppi = laskentatyyppi;
        this.isValintakoelaskenta = isValintakoelaskenta;
        this.valinnanvaihe = valinnanvaihe;
        this.hakuOid = hakuOid;
        this.maski = maski;
        this.isErillishaku = isErillishaku;
    }

    public LaskentaTyyppi getLaskentatyyppi() {
        return laskentatyyppi;
    }

    public Boolean getIsValintakoelaskenta() {
        return isValintakoelaskenta;
    }

    public Integer getValinnanvaihe() {
        return valinnanvaihe;
    }

    public String getHakuOid() {
        return hakuOid;
    }

    public Maski getMaski() {
        return maski;
    }

    public boolean isErillishaku() {
        return isErillishaku;
    }
}
