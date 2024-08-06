package fi.vm.sade.valinta.kooste.valintatapajono.service;

import com.google.gson.GsonBuilder;
import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.auditlog.User;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.dokumentit.dao.DokumenttiRepository;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.PoikkeusKasittelijaSovitin;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRivi;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import fi.vm.sade.valinta.sharedutils.ValintaResource;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import io.reactivex.Observable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

@Service
public class ValintatapajonoTuontiService {
  private static final Logger LOG = LoggerFactory.getLogger(ValintatapajonoTuontiService.class);
  private static final String VALMIS = "valmis";
  @Autowired private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;
  @Autowired private ApplicationAsyncResource applicationAsyncResource;
  @Autowired private AtaruAsyncResource ataruAsyncResource;
  @Autowired private ValintalaskentaAsyncResource valintalaskentaAsyncResource;
  @Autowired private DokumenttiRepository dokumenttiRepository;

  public void tuo(
      BiFunction<
              List<ValintatietoValinnanvaiheDTO>,
              List<HakemusWrapper>,
              Collection<ValintatapajonoRivi>>
          riviFunction,
      final String hakuOid,
      final String hakukohdeOid,
      final String tarjoajaOid,
      final String valintatapajonoOid,
      DeferredResult<ResponseEntity<String>> result,
      User user) {
    AtomicReference<String> keyRef = new AtomicReference<>(null);
    AtomicInteger counter =
        new AtomicInteger(
            1 // valinnanvaiheet
                + 1 // valintaperusteet
                + 1 // hakemukset
                + 1 // dokumentti
            // +1 // org oikeuksien tarkistus
            );
    AtomicReference<List<ValintatietoValinnanvaiheDTO>> valinnanvaiheetRef =
        new AtomicReference<>();
    AtomicReference<List<ValinnanVaiheJonoillaDTO>> valintaperusteetRef = new AtomicReference<>();
    AtomicReference<List<HakemusWrapper>> hakemuksetRef = new AtomicReference<>();

    final Supplier<Void> mergeSuplier =
        () -> {
          if (counter.decrementAndGet() == 0) {
            Collection<ValintatapajonoRivi> rivit;
            try {
              rivit = riviFunction.apply(valinnanvaiheetRef.get(), hakemuksetRef.get());
            } catch (Throwable t) {
              poikkeusKasittelija(
                      "Rivien lukeminen annetuista tiedoista epäonnistui", result, keyRef)
                  .accept(t);
              return null;
            }
            try {
              ValinnanvaiheDTO valinnanvaihe =
                  ValintatapajonoTuontiConverter.konvertoi(
                      hakuOid,
                      hakukohdeOid,
                      valintatapajonoOid,
                      valintaperusteetRef.get(),
                      hakemuksetRef.get(),
                      valinnanvaiheetRef.get(),
                      rivit);
              LOG.info("{}", new GsonBuilder().setPrettyPrinting().create().toJson(valinnanvaihe));
              Observable.fromFuture(
                      valintalaskentaAsyncResource.lisaaTuloksia(
                          hakuOid, hakukohdeOid, tarjoajaOid, valinnanvaihe))
                  .subscribe(
                      ok -> {
                        try {
                          valinnanvaihe
                              .getValintatapajonot()
                              .forEach(
                                  v ->
                                      v.getJonosijat()
                                          .forEach(
                                              jonosija -> {
                                                Map<String, String> additionalAuditFields =
                                                    new HashMap<>();
                                                additionalAuditFields.put("hakuOid", hakuOid);
                                                additionalAuditFields.put(
                                                    "hakukohdeOid", hakukohdeOid);
                                                additionalAuditFields.put(
                                                    "valinnanvaiheOid",
                                                    valinnanvaihe.getValinnanvaiheoid());
                                                additionalAuditFields.put(
                                                    "valintatapajonoOid",
                                                    v.getValintatapajonooid());
                                                AuditLog.log(
                                                    KoosteAudit.AUDIT,
                                                    user,
                                                    ValintaperusteetOperation
                                                        .VALINNANVAIHE_TUONTI_EXCEL,
                                                    ValintaResource.VALINTATAPAJONOSERVICE,
                                                    jonosija.getHakijaOid(),
                                                    Changes.addedDto(jonosija),
                                                    additionalAuditFields);
                                              }));
                        } catch (Throwable t) {
                          LOG.error("Audit logitus epäonnistui", t);
                        }
                        dokumenttiRepository.merkkaaValmiiksi(UUID.fromString(keyRef.get()));
                      },
                      poikkeusKasittelija(
                          "Tallennus valintapalveluun epäonnistui", result, keyRef));
              LOG.info(
                  "Saatiin vastaus muodostettua hakukohteelle {} haussa {}. Palautetaan se asynkronisena paluuarvona.",
                  hakukohdeOid,
                  hakuOid);
              dokumenttiRepository.paivitaKuvaus(
                  UUID.fromString(keyRef.get()),
                  "Tuonnin esitiedot haettu onnistuneesti. Tallennetaan kantaan...");
            } catch (Throwable t) {
              poikkeusKasittelija("Tallennus valintapalveluun epäonnistui", result, keyRef)
                  .accept(t);
              return null;
            }
          }
          return null;
        };
    valintalaskentaAsyncResource
        .laskennantulokset(hakukohdeOid)
        .whenComplete(
            (valinnanvaiheet, t) -> {
              if (t != null) {
                poikkeusKasittelija("Valinnanvaiheiden hakeminen epäonnistui", result, keyRef)
                    .accept(t);
              } else {
                valinnanvaiheetRef.set(valinnanvaiheet);
                mergeSuplier.get();
              }
            });
    valintaperusteetAsyncResource
        .haeIlmanlaskentaa(hakukohdeOid)
        .subscribe(
            valintaperusteet -> {
              valintaperusteetRef.set(valintaperusteet);
              mergeSuplier.get();
            },
            poikkeusKasittelija("Hakemusten hakeminen epäonnistui", result, keyRef));
    Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid))
        .flatMap(
            haku -> {
              if (haku.isHakemuspalvelu()) {
                return Observable.fromFuture(
                    ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid));
              } else {
                return Observable.fromFuture(
                    applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid));
              }
            })
        .subscribe(
            hakemukset -> {
              if (hakemukset == null || hakemukset.isEmpty()) {
                poikkeusKasittelija("Ei yhtään hakemusta hakukohteessa", result, keyRef)
                    .accept(null);
              } else {
                hakemuksetRef.set(hakemukset);
                mergeSuplier.get();
              }
            },
            poikkeusKasittelija("Hakemusten hakeminen epäonnistui", result, keyRef));

    UUID key = dokumenttiRepository.luoDokumentti("Valintatapajonon tuonti");
    result.setResult(
        ResponseEntity.status(HttpStatus.OK)
            .header("Content-Type", "text/plain")
            .body(key.toString()));
    keyRef.set(key.toString());
    mergeSuplier.get();
  }

  private PoikkeusKasittelijaSovitin poikkeusKasittelija(
      String viesti,
      DeferredResult<ResponseEntity<String>> result,
      AtomicReference<String> dokumenttiIdRef) {
    return new PoikkeusKasittelijaSovitin(
        poikkeus -> {
          if (poikkeus == null) {
            LOG.error("###Poikkeus tuonnissa {}\r\n###", viesti);
          } else {
            LOG.error("###Poikkeus tuonnissa :" + viesti + "###", poikkeus);
          }
          try {
            result.setErrorResult(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                        viesti + (poikkeus != null ? " poikkeus: " + poikkeus.getMessage() : "")));
          } catch (Throwable t) {
            // ei väliä vaikka response jos tehty
          }
          try {
            String dokumenttiId = dokumenttiIdRef.get();
            if (dokumenttiId != null) {
              dokumenttiRepository.lisaaVirheilmoitus(UUID.fromString(dokumenttiId), viesti);
            }
          } catch (Throwable t) {
            LOG.error("Odottamaton virhe", t);
          }
        });
  }
}
