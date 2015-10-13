package fi.vm.sade.valinta.kooste.parametrit.service;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.http.HttpExceptionWithStatus;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import rx.Observable;
import rx.observables.BlockingObservable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class HakuParametritService {

    private static final Logger LOG = LoggerFactory.getLogger(HakuParametritService.class);

    private String rootOrganisaatioOid;
    private OhjausparametritAsyncResource ohjausparametritAsyncResource;
    private TarjontaAsyncResource tarjontaAsyncResource;

    @Autowired
    public HakuParametritService(OhjausparametritAsyncResource ohjausparametritAsyncResource, TarjontaAsyncResource tarjontaAsyncResource, @Value("${root.organisaatio.oid:1.2.246.562.10.00000000001}") String rootOrganisaatioOid) {
        this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
        this.rootOrganisaatioOid = rootOrganisaatioOid;
        this.tarjontaAsyncResource = tarjontaAsyncResource;
    }

    public ParametritParser getParametritForHaku(String hakuOid) {

        final CompletableFuture<ParametritDTO> promise = new CompletableFuture<>();
        // ohjausparametrit-service returns 404 for haku without parameters
        ohjausparametritAsyncResource.haeHaunOhjausparametrit(
                hakuOid,
                parametrit -> {
                    promise.complete(parametrit);
                },
                (Throwable t) -> {
                    if(t instanceof HttpExceptionWithStatus && ((HttpExceptionWithStatus)t).status == 404) {
                        promise.complete(new ParametritDTO());
                    } else {
                        promise.completeExceptionally(t);
                    }
                }
        );

        Observable<HakuV1RDTO> hakuFuture = tarjontaAsyncResource.haeHaku(hakuOid);

        try {
            ParametritParser ret = new ParametritParser(promise.get(), BlockingObservable.from(hakuFuture).first(), this.rootOrganisaatioOid);
            return ret;
        } catch (InterruptedException e) {
            LOG.error("Ohjausparametrien luku epäonnistui", e);
        } catch (ExecutionException e) {
            LOG.error("Ohjausparametrien luku epäonnistui", e);
        }
        return null;
    }

}
