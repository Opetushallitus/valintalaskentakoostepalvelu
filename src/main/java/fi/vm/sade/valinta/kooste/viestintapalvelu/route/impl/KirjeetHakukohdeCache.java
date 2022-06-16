package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KirjeetHakukohdeCache {
  private final Cache<String, CompletableFuture<MetaHakukohde>> metaHakukohdeCache;
  private final TarjontaAsyncResource tarjontaAsyncResource;
  private final OrganisaatioAsyncResource organisaatioAsyncResource;

  @Autowired
  public KirjeetHakukohdeCache(TarjontaAsyncResource tarjontaAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource) {
    metaHakukohdeCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();
    this.tarjontaAsyncResource = tarjontaAsyncResource;
    this.organisaatioAsyncResource = organisaatioAsyncResource;
  }

  public MetaHakukohde haeHakukohde(String hakukohdeOid) {
    try {
      return haeHakukohdeAsync(hakukohdeOid).get(30, SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public CompletableFuture<MetaHakukohde> haeHakukohdeAsync(String hakukohdeOid) {
    try {
      CompletableFuture<MetaHakukohde> f = this.metaHakukohdeCache.get(hakukohdeOid,
          () -> fetchMetaHakukohde(hakukohdeOid));
      if (f.isCompletedExceptionally()) {
        this.metaHakukohdeCache.invalidate(hakukohdeOid);
        return this.metaHakukohdeCache.get(hakukohdeOid, () -> fetchMetaHakukohde(hakukohdeOid));
      }
      return f;
    } catch (ExecutionException e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  public static String getOpetuskieli(Collection<String> opetuskielet) {
    TreeSet<String> preferoitukieli = Sets.newTreeSet();
    for (String opetuskieli : opetuskielet) {
      preferoitukieli.add(KieliUtil.normalisoiKielikoodi(opetuskieli));
    }
    if (preferoitukieli.contains(KieliUtil.SUOMI)) {
      return KieliUtil.SUOMI;
    } else if (preferoitukieli.contains(KieliUtil.RUOTSI)) {
      return KieliUtil.RUOTSI;
    } else if (preferoitukieli.contains(KieliUtil.ENGLANTI)) {
      return KieliUtil.ENGLANTI;
    }
    return KieliUtil.SUOMI;
  }

  private CompletableFuture<MetaHakukohde> fetchMetaHakukohde(String hakukohdeOid) {
    return this.tarjontaAsyncResource.haeHakukohde(hakukohdeOid)
        .thenComposeAsync(hakukohde -> CompletableFutureUtil
            .sequence(hakukohde.toteutusOids.stream().map(tarjontaAsyncResource::haeToteutus)
                .collect(Collectors.toList()))
            .thenComposeAsync(toteutukset -> CompletableFutureUtil
                .sequence(hakukohde.tarjoajaOids.stream()
                    .map(organisaatioAsyncResource::haeOrganisaatio).collect(Collectors.toList()))
                .thenApplyAsync(tarjoajat -> {
                  Teksti hakukohdeNimi = new Teksti(hakukohde.nimi);
                  Teksti ohjeetUudelleOpiskelijalle = new Teksti(
                      hakukohde.ohjeetUudelleOpiskelijalle);
                  return new MetaHakukohde(
                      hakukohde.tarjoajaOids.iterator().next(), hakukohdeNimi, tarjoajat.stream()
                          .map(t -> new Teksti(t.getNimi())).collect(Collectors.toList()),
                      hakukohdeNimi.getKieli(),
                      getOpetuskieli(toteutukset.stream()
                          .flatMap(toteutus -> toteutus.opetuskielet.stream())
                          .collect(Collectors.toList())),
                      ohjeetUudelleOpiskelijalle);
                })));
  }
}
