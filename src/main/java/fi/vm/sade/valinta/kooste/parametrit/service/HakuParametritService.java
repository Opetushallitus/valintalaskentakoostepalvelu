package fi.vm.sade.valinta.kooste.parametrit.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import rx.Observable;

import java.util.concurrent.TimeUnit;

public class HakuParametritService {

    private static final Logger LOG = LoggerFactory.getLogger(HakuParametritService.class);

    private final String rootOrganisaatioOid;
    private OhjausparametritAsyncResource ohjausparametritAsyncResource;

    private final Cache<String, Observable<ParametritDTO>> haunOhjausParametritCache;

    @Autowired
    public HakuParametritService(OhjausparametritAsyncResource ohjausparametritAsyncResource, @Value("${root.organisaatio.oid:1.2.246.562.10.00000000001}") String rootOrganisaatioOid,
                                 @Value("${valintalaskentakoostepalvelu.ohjausparametrit.cache.ttl.minutes:30}") int cacheTtlMinutes,
                                 @Value("${valintalaskentakoostepalvelu.ohjausparametrit.request.timeout.seconds:20}") int requestTimeoutSeconds) {
        this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
        this.rootOrganisaatioOid = rootOrganisaatioOid;
        haunOhjausParametritCache = CacheBuilder.newBuilder().expireAfterWrite(cacheTtlMinutes, TimeUnit.MINUTES).build();
        LOG.info("Initialized with haunOhjausParametritCache ttl minutes " + cacheTtlMinutes + "and request timeout seconds " + requestTimeoutSeconds);
    }

    public ParametritParser getParametritForHaku(String hakuOid) {
        try {
            Observable<ParametritDTO> parametritDTOObservable = haunOhjausParametritCache.get(hakuOid,
                    () -> ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid).single());
            ParametritDTO paramDto = parametritDTOObservable
                .timeout(30, SECONDS)
                .toBlocking()
                .single();
            return new ParametritParser(paramDto, rootOrganisaatioOid);
        } catch (Exception e) {
            LOG.error(String.format("Error querying ParametritDTO for haku %s", hakuOid), e);
            throw new RuntimeException(e);
        }
    }
}
