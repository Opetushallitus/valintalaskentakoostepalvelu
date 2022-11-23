package fi.vm.sade.valinta.kooste.valintatapajono.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.HakukohdeResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.AbstractHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoExcel;
import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoVientiRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.DokumenttiUtils.defaultExpirationDate;
import static fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.DokumenttiUtils.generateId;

@Component
public class ValintatapajonoVientiRouteImpl implements ValintatapajonoVientiRoute {
  private static final Logger LOG = LoggerFactory.getLogger(ValintatapajonoVientiRouteImpl.class);

  private final ApplicationResource applicationResource;
  private final AtaruAsyncResource ataruAsyncResource;
  private final DokumenttiAsyncResource dokumenttiAsyncResource;
  private final TarjontaAsyncResource tarjontaAsyncResource;
  private final HakukohdeResource hakukohdeResource;

  @Autowired
  public ValintatapajonoVientiRouteImpl(
      ApplicationResource applicationResource,
      AtaruAsyncResource ataruAsyncResource,
      DokumenttiAsyncResource dokumenttiAsyncResource,
      TarjontaAsyncResource tarjontaAsyncResource,
      HakukohdeResource hakukohdeResource) {
    this.applicationResource = applicationResource;
    this.ataruAsyncResource = ataruAsyncResource;
    this.dokumenttiAsyncResource = dokumenttiAsyncResource;
    this.tarjontaAsyncResource = tarjontaAsyncResource;
    this.hakukohdeResource = hakukohdeResource;
  }

  public void valintatapajonoVienti(String valintatapajonoOid, String hakukohdeOid, DokumenttiProsessi dokumenttiprosessi) throws ExecutionException, InterruptedException, TimeoutException {

    dokumenttiprosessi
      .setKokonaistyo(
        // haun nimi ja hakukohteen nimi
        1
          + 1
          +
          // osallistumistiedot + valintaperusteet +
          // hakemuspistetiedot
          1
          + 1
          // luonti
          + 1
          // dokumenttipalveluun vienti
          + 1);
    String hakuOid = dokumenttiprosessi.getHakuOid();
    Haku haku = tarjontaAsyncResource.haeHaku(hakuOid).get(5, TimeUnit.MINUTES);
    String hakuNimi = new Teksti(haku.nimi).getTeksti();
    dokumenttiprosessi.inkrementoiTehtyjaToita();
    AbstractHakukohde hakukohde =
      tarjontaAsyncResource.haeHakukohde(hakukohdeOid).get(5, TimeUnit.MINUTES);
    String hakukohdeNimi = new Teksti(hakukohde.nimi).getTeksti();
    dokumenttiprosessi.inkrementoiTehtyjaToita();
    if (hakukohdeOid == null || hakuOid == null || valintatapajonoOid == null) {
      LOG.error(
        "Pakolliset tiedot reitille puuttuu hakuOid = {}, hakukohdeOid = {}, valintatapajonoOid = {}",
        hakuOid,
        hakukohdeOid,
        valintatapajonoOid);
      dokumenttiprosessi
        .getPoikkeukset()
        .add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Puutteelliset lähtötiedot"));
      throw new RuntimeException(
        "Pakolliset tiedot reitille puuttuu hakuOid, hakukohdeOid, valintatapajonoOid");
    }
    final List<HakemusWrapper> hakemukset;
    try {
      if (haku.isHakemuspalvelu()) {
        hakemukset =
          ataruAsyncResource
            .getApplicationsByHakukohde(hakukohdeOid)
            .get(1, TimeUnit.MINUTES);
      } else {
        hakemukset =
          applicationResource
            .getApplicationsByOid(
              hakuOid,
              hakukohdeOid,
              ApplicationResource.ACTIVE_AND_INCOMPLETE,
              ApplicationResource.MAX)
            .stream()
            .map(HakuappHakemusWrapper::new)
            .collect(Collectors.toList());
      }
      LOG.debug("Saatiin hakemukset {}", hakemukset.size());
      dokumenttiprosessi.inkrementoiTehtyjaToita();
    } catch (Exception e) {
      LOG.error("Hakemuspalvelun virhe", e);
      dokumenttiprosessi
        .getPoikkeukset()
        .add(
          new Poikkeus(
            Poikkeus.HAKU,
            "Hakemuspalvelulta ei saatu hakemuksia hakukohteelle",
            e.getMessage()));
      throw e;
    }
    if (hakemukset.isEmpty()) {
      LOG.error("Nolla hakemusta!");
      dokumenttiprosessi
        .getPoikkeukset()
        .add(
          new Poikkeus(
            Poikkeus.HAKU,
            "Hakukohteella ei ole hakemuksia!",
            "Nolla hakemusta!"));
      throw new RuntimeException("Hakukohteelle saatiin tyhjä hakemusjoukko!");
    }
    final List<ValintatietoValinnanvaiheDTO> valinnanvaiheet;
    try {
      valinnanvaiheet = hakukohdeResource.hakukohde(hakukohdeOid);
      dokumenttiprosessi.inkrementoiTehtyjaToita();
    } catch (Exception e) {
      LOG.error("Valinnanvaiheiden haku virhe", e);
      dokumenttiprosessi
        .getPoikkeukset()
        .add(
          new Poikkeus(
            Poikkeus.VALINTALASKENTA,
            "Valintalaskennalta ei saatu valinnanvaiheita",
            e.getMessage()));
      throw e;
    }
    InputStream xlsx;
    try {
      ValintatapajonoExcel valintatapajonoExcel =
        new ValintatapajonoExcel(
          hakuOid,
          hakukohdeOid,
          valintatapajonoOid,
          hakuNimi,
          hakukohdeNimi,
          valinnanvaiheet,
          hakemukset);
      xlsx = valintatapajonoExcel.getExcel().vieXlsx();
      dokumenttiprosessi.inkrementoiTehtyjaToita();
    } catch (Exception e) {
      LOG.error("Valintatapajono excelin luonti virhe", e);
      dokumenttiprosessi
        .getPoikkeukset()
        .add(
          new Poikkeus(
            Poikkeus.KOOSTEPALVELU,
            "Valintatapajono exceliä ei saatu luotua!",
            e.getMessage()));
      throw e;
    }
    try {
      String id = generateId();
      Long expirationTime = defaultExpirationDate().getTime();
      List<String> tags = dokumenttiprosessi.getTags();
      dokumenttiAsyncResource
        .tallenna(
          id,
          "valintatapajono.xlsx",
          expirationTime,
          tags,
          "application/octet-stream",
          xlsx)
        .subscribe(
          ok -> {
            dokumenttiprosessi.setDokumenttiId(id);
            dokumenttiprosessi.inkrementoiTehtyjaToita();
          },
          poikkeus -> {
            LOG.error(
              "Valintatapajonoexcelin tallennus dokumenttipalveluun epäonnistui");
            throw new RuntimeException(poikkeus);
          });
    } catch (Exception e) {
      LOG.error("Dokumenttipalveluun vienti virhe", e);
      dokumenttiprosessi
        .getPoikkeukset()
        .add(
          new Poikkeus(
            Poikkeus.DOKUMENTTIPALVELU,
            "Dokumenttipalveluun ei saatu vietyä taulukkolaskentatiedostoa!",
            ""));
      throw e;
    }
  }

  @Override
  public void vie(DokumenttiProsessi prosessi, String hakuOid, String hakukohdeOid, String valintatapajonoOid) {
    try {
      valintatapajonoVienti(valintatapajonoOid, hakukohdeOid, prosessi);
    } catch (Exception e) {
      String syy;
      syy = e.getMessage();
      prosessi
        .getPoikkeukset()
        .add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Valintatapajonon vienti", syy));
    }
  }
}
