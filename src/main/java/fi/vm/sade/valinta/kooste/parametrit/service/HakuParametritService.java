package fi.vm.sade.valinta.kooste.parametrit.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import fi.vm.sade.sijoittelu.domain.Hakukohde;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HakuParametritService {

    private static final Logger LOG = LoggerFactory.getLogger(HakuParametritService.class);

    private String rootOrganisaatioOid;
    private OhjausparametritAsyncResource ohjausparametritAsyncResource;

    private final Cache<String, ParametritParser> haunOhjausParametritCache;
    private final int requestTimeoutSeconds;

    @Autowired
    public HakuParametritService(OhjausparametritAsyncResource ohjausparametritAsyncResource, @Value("${root.organisaatio.oid:1.2.246.562.10.00000000001}") String rootOrganisaatioOid,
                                 @Value("${valintalaskentakoostepalvelu.ohjausparametrit.cache.ttl.minutes:30}") int cacheTtlMinutes,
                                 @Value("${valintalaskentakoostepalvelu.ohjausparametrit.request.timeout.seconds:20}") int requestTimeoutSeconds) {
        this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
        this.rootOrganisaatioOid = rootOrganisaatioOid;
        haunOhjausParametritCache = CacheBuilder.newBuilder().expireAfterWrite(cacheTtlMinutes, TimeUnit.MINUTES).build();
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        LOG.info("Initialized with haunOhjausParametritCache ttl minutes " + cacheTtlMinutes + "and request timeout seconds " + requestTimeoutSeconds);
    }

    public ParametritParser getParametritForHaku(String hakuOid) {
        try {
            return haunOhjausParametritCache.get(hakuOid, () -> fetchParametrit(hakuOid));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private ParametritParser fetchParametrit(String hakuOid) {
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

        try {
            ParametritParser ret = new ParametritParser(promise.get(requestTimeoutSeconds, TimeUnit.SECONDS), this.rootOrganisaatioOid);
            return ret;
        } catch (Throwable e) {
            LOG.error("Ohjausparametrien luku epäonnistui haulle {}", hakuOid, e);
        }
        return null;
    }

}
