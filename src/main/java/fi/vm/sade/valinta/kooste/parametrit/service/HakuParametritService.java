package fi.vm.sade.valinta.kooste.parametrit.service;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HakuParametritService {

  private static final Logger LOG = LoggerFactory.getLogger(HakuParametritService.class);

  private final String rootOrganisaatioOid;
  private OhjausparametritAsyncResource ohjausparametritAsyncResource;

  private final Cache<String, CompletableFuture<ParametritParser>> haunOhjausParametritCache;

  @Autowired
  public HakuParametritService(
      OhjausparametritAsyncResource ohjausparametritAsyncResource,
      @Value("${root.organisaatio.oid:1.2.246.562.10.00000000001}") String rootOrganisaatioOid,
      @Value("${valintalaskentakoostepalvelu.ohjausparametrit.cache.ttl.minutes:30}")
          int cacheTtlMinutes,
      @Value("${valintalaskentakoostepalvelu.ohjausparametrit.request.timeout.seconds:20}")
          int requestTimeoutSeconds) {
    this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
    this.rootOrganisaatioOid = rootOrganisaatioOid;
    this.haunOhjausParametritCache =
        CacheBuilder.newBuilder().expireAfterWrite(cacheTtlMinutes, TimeUnit.MINUTES).build();
    LOG.info(
        "Initialized with haunOhjausParametritCache ttl minutes "
            + cacheTtlMinutes
            + "and request timeout seconds "
            + requestTimeoutSeconds);
  }

  public ParametritParser getParametritForHaku(String hakuOid) {
    try {
      return getParametritForHakuAsync(hakuOid).get(30, SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public CompletableFuture<ParametritParser> getParametritForHakuAsync(String hakuOid) {
    try {
      CompletableFuture<ParametritParser> f =
          haunOhjausParametritCache.get(hakuOid, () -> fetchParametrit(hakuOid));
      if (f.isCompletedExceptionally()) {
        haunOhjausParametritCache.invalidate(hakuOid);
        return haunOhjausParametritCache.get(hakuOid, () -> this.fetchParametrit(hakuOid));
      }
      return f;
    } catch (ExecutionException e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  private CompletableFuture<ParametritParser> fetchParametrit(String hakuOid) {
    return ohjausparametritAsyncResource
        .haeHaunOhjausparametrit(hakuOid)
        .thenApplyAsync(parametrit -> new ParametritParser(parametrit, this.rootOrganisaatioOid));
  }
}
