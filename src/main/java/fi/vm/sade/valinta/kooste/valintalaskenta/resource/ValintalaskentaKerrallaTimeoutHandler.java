package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

public class ValintalaskentaKerrallaTimeoutHandler implements TimeoutHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaKerrallaTimeoutHandler.class);

    private final String hakuOid;
    private final Integer valinnanvaihe;
    private final Boolean valintakoelaskenta;
    private final LaskentaTyyppi tyyppi;
    private final boolean whitelist;
    private final List<String> maski;
    private final AsyncResponse asyncResponse;

    public ValintalaskentaKerrallaTimeoutHandler(final String hakuOid, final Integer valinnanvaihe, final Boolean valintakoelaskenta, final LaskentaTyyppi tyyppi, final boolean whitelist, final List<String> maski, AsyncResponse asyncResponse) {
        this.hakuOid = hakuOid;
        this.valinnanvaihe = valinnanvaihe;
        this.valintakoelaskenta = valintakoelaskenta;
        this.tyyppi = tyyppi;
        this.whitelist = whitelist;
        this.maski = maski;
        this.asyncResponse = asyncResponse;
    }

    public void handleTimeout(AsyncResponse asyncResponse) {
        String hakukohdeOids = null;
        if (maski != null && !maski.isEmpty()) {
            try {
                Object[] hakukohdeOidArray = maski.toArray();
                StringBuilder sb = new StringBuilder();
                sb.append(Arrays.toString(Arrays.copyOfRange(
                        hakukohdeOidArray,
                        0,
                        Math.min(hakukohdeOidArray.length, 10))));
                if (hakukohdeOidArray.length > 10) {
                    sb.append(" ensimmaiset 10 hakukohdetta maskissa jossa on yhteensa hakukohteita ")
                            .append(hakukohdeOidArray.length);
                } else {
                    sb.append(" maskin hakukohteet");
                }
                hakukohdeOids = sb.toString();
            } catch (Exception e) {
                hakukohdeOids = e.getMessage();
            }
        }

        LOG.error("Laskennan kaynnistys timeuottasi kutsulle /haku/{}/tyyppi/{}/whitelist/{}?valinnanvaihe={}&valintakoelaskenta={}\r\n{}",
                hakuOid, tyyppi, whitelist, valinnanvaihe, valintakoelaskenta, hakukohdeOids, hakukohdeOids);
        asyncResponse.resume(Response
                .serverError()
                .entity("Uudelleen ajo laskennalle aikakatkaistu!")
                .build());
    }
}
