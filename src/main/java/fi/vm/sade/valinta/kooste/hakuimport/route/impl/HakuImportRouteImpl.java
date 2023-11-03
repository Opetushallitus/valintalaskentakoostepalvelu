package fi.vm.sade.valinta.kooste.hakuimport.route.impl;

import static java.util.concurrent.CompletableFuture.*;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakuImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakukohdeImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakuImportRoute;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakukohdeImportRoute;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class HakuImportRouteImpl implements HakuImportRoute, HakukohdeImportRoute {
  private static final Logger LOG = LoggerFactory.getLogger(HakuImportRouteImpl.class);

  private final SuoritaHakuImportKomponentti suoritaHakuImportKomponentti;
  private final SuoritaHakukohdeImportKomponentti tarjontaJaKoodistoHakukohteenHakuKomponentti;
  private final ValintaperusteetAsyncResource valintaperusteetRestResource;
  private final ExecutorService hakuImportThreadPool;
  private final ExecutorService hakukohdeImportThreadPool;
  private final ValvomoAdminService<HakuImportProsessi> hakuImportValvomo;

  @Autowired
  public HakuImportRouteImpl(
      @Value("${valintalaskentakoostepalvelu.hakuimport.threadpoolsize:10}")
          Integer hakuImportThreadpoolSize,
      @Value("${valintalaskentakoostepalvelu.hakukohdeimport.threadpoolsize:10}")
          Integer hakukohdeImportThreadpoolSize,
      @Autowired(required = false) @Qualifier(value = "hakuImportValvomo")
          ValvomoServiceImpl<HakuImportProsessi> hakuImportValvomo,
      SuoritaHakuImportKomponentti suoritaHakuImportKomponentti,
      ValintaperusteetAsyncResource valintaperusteetRestResource,
      SuoritaHakukohdeImportKomponentti tarjontaJaKoodistoHakukohteenHakuKomponentti) {
    this.hakuImportValvomo = hakuImportValvomo;
    this.suoritaHakuImportKomponentti = suoritaHakuImportKomponentti;
    this.tarjontaJaKoodistoHakukohteenHakuKomponentti =
        tarjontaJaKoodistoHakukohteenHakuKomponentti;
    this.valintaperusteetRestResource = valintaperusteetRestResource;
    this.hakuImportThreadPool = Executors.newFixedThreadPool(hakuImportThreadpoolSize);
    LOG.info("Using hakuImportThreadPool thread pool size " + hakuImportThreadpoolSize);
    this.hakukohdeImportThreadPool = Executors.newFixedThreadPool(hakukohdeImportThreadpoolSize);
    LOG.info("Using hakukohdeImportThreadPool thread pool size " + hakukohdeImportThreadpoolSize);
  }

  @Override
  public Future<?> asyncAktivoiHakuImport(String hakuOid) {
    HakuImportProsessi prosessi = new HakuImportProsessi("Haun importointi", hakuOid);
    hakuImportValvomo.start(prosessi);
    Collection<String> hakukohdeOids = suoritaHakuImportKomponentti.suoritaHakukohdeImport(hakuOid);
    prosessi.setHakukohteita(hakukohdeOids.size());
    LOG.info("Hakukohteita importoitavana {}", hakukohdeOids.size());

    CompletableFuture<Void> allDone =
        allOf(
            hakukohdeOids.stream()
                .map(
                    hakukohdeOid ->
                        runAsync(importHakukohdeJob(prosessi, hakukohdeOid), hakuImportThreadPool))
                .toArray(CompletableFuture[]::new));

    allDone.whenComplete((a, b) -> hakuImportValvomo.finish(prosessi));
    return allDone;
  }

  private Runnable importHakukohdeJob(HakuImportProsessi prosessi, String hakukohdeOid) {
    return () -> {
      try {
        HakukohdeImportDTO hki =
            tarjontaJaKoodistoHakukohteenHakuKomponentti.suoritaHakukohdeImport(hakukohdeOid);
        ResponseEntity response = valintaperusteetRestResource.tuoHakukohde(hki).blockingFirst();
        LOG.debug("Hakukohde " + hakukohdeOid + " importoitu! " + response.getStatusCodeValue());
        int t = prosessi.lisaaTuonti();
        if (t % 25 == 0 || t == prosessi.getHakukohteita()) {
          LOG.info("Hakukohde on tuotu onnistuneesti ({}/{}).", t, prosessi.getHakukohteita());
        }
      } catch (Exception e) {
        LOG.error(
            "Epaonnistuneita hakukohdeOideja tahan mennessa {}",
            Arrays.toString(prosessi.getEpaonnistuneetHakukohteet()));
        String message = hakukohdeOid + "_KONVERSIOSSA";
        prosessi.lisaaVirhe(message);
        hakuImportValvomo.fail(prosessi, e, message);
      }
    };
  }

  @Override
  public Future<?> asyncAktivoiHakukohdeImport(
      String hakukohdeOid, HakuImportProsessi prosessi, Authentication auth) {
    return hakukohdeImportThreadPool.submit(importHakukohdeJob(prosessi, hakukohdeOid));
  }
}
