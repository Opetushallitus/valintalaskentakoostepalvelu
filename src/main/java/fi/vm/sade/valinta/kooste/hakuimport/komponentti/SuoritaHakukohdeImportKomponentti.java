package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import fi.vm.sade.service.valintaperusteet.dto.AvainArvoDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdekoodiDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohteenValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.MonikielinenTekstiDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ValintakoeV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.koulutus.KoulutusV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.camel.Body;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SuoritaHakukohdeImportKomponentti {
  private static final Logger LOG =
      LoggerFactory.getLogger(SuoritaHakukohdeImportKomponentti.class);

  private TarjontaAsyncResource tarjontaAsyncResource;
  private OrganisaatioAsyncResource organisaatioAsyncResource;
  private KoodistoCachedAsyncResource koodistoAsyncResource;

  @Autowired
  public SuoritaHakukohdeImportKomponentti(
      TarjontaAsyncResource tarjontaAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource,
      KoodistoCachedAsyncResource koodistoAsyncResource) {
    this.tarjontaAsyncResource = tarjontaAsyncResource;
    this.organisaatioAsyncResource = organisaatioAsyncResource;
    this.koodistoAsyncResource = koodistoAsyncResource;
  }

  public HakukohdeImportDTO suoritaHakukohdeImport(
      @Body // @Property(OPH.HAKUKOHDEOID)
          String hakukohdeOid) {
    try {
      HakukohdeV1RDTO hakukohde =
          tarjontaAsyncResource.haeHakukohde(hakukohdeOid).get(5, TimeUnit.MINUTES);
      HakuV1RDTO haku =
          tarjontaAsyncResource.haeHaku(hakukohde.getHakuOid()).get(5, TimeUnit.MINUTES);
      List<KoulutusV1RDTO> toteutukset =
          CompletableFutureUtil.sequence(
                  hakukohde.getHakukohdeKoulutusOids().stream()
                      .map(tarjontaAsyncResource::haeToteutus)
                      .collect(Collectors.toList()))
              .get(5, TimeUnit.MINUTES);
      Map<String, Koodi> kaudet = koodistoAsyncResource.haeKoodisto("kausi");
      HakukohdeImportDTO importTyyppi = new HakukohdeImportDTO();

      Iterator<String> tarjoajaOids = hakukohde.getTarjoajaOids().iterator();
      importTyyppi.setTarjoajaOid(tarjoajaOids.hasNext() ? tarjoajaOids.next() : null);
      importTyyppi.setTarjoajaOids(hakukohde.getTarjoajaOids());
      importTyyppi.setHaunkohdejoukkoUri(haku.getKohdejoukkoUri());

      CompletableFutureUtil.sequence(
              hakukohde.getTarjoajaOids().stream()
                  .map(organisaatioAsyncResource::haeOrganisaatio)
                  .collect(Collectors.toList()))
          .get(5, TimeUnit.MINUTES)
          .forEach(
              tarjoaja ->
                  tarjoaja
                      .getNimi()
                      .forEach(
                          (kieli, nimi) -> {
                            MonikielinenTekstiDTO dto = new MonikielinenTekstiDTO();
                            dto.setLang("kieli_" + kieli);
                            dto.setText(nimi);
                            importTyyppi.getTarjoajaNimi().add(dto);
                          }));

      hakukohde
          .getHakukohteenNimet()
          .forEach(
              (key, value) -> {
                MonikielinenTekstiDTO dto = new MonikielinenTekstiDTO();
                dto.setLang(key);
                dto.setText(value);
                importTyyppi.getHakukohdeNimi().add(dto);
              });

      kaudet.forEach(
          (arvo, koodi) -> {
            if (haku.getHakukausiUri().startsWith(koodi.getKoodiUri())) {
              koodi
                  .getMetadata()
                  .forEach(
                      metadata -> {
                        MonikielinenTekstiDTO dto = new MonikielinenTekstiDTO();
                        dto.setLang("kieli_" + metadata.getKieli().toLowerCase());
                        dto.setText(metadata.getNimi());
                        importTyyppi.getHakuKausi().add(dto);
                      });
            }
          });

      importTyyppi.setHakuVuosi(Integer.toString(haku.getHakukausiVuosi()));

      HakukohdekoodiDTO hkt = new HakukohdekoodiDTO();
      if (hakukohde.getHakukohteenNimiUri() != null) {
        hkt.setKoodiUri(hakukohde.getHakukohteenNimiUri());
      } else {
        hkt.setKoodiUri("hakukohteet_" + hakukohde.getOid().replace(".", ""));
      }
      importTyyppi.setHakukohdekoodi(hkt);

      importTyyppi.setHakukohdeOid(hakukohde.getOid());
      importTyyppi.setHakuOid(hakukohde.getHakuOid());
      importTyyppi.setValinnanAloituspaikat(
          hakukohde.getValintojenAloituspaikatLkm() == null
              ? 0
              : hakukohde.getValintojenAloituspaikatLkm());
      importTyyppi.setTila(hakukohde.getTila().name());
      if (hakukohde.getValintakokeet() != null) {
        for (ValintakoeV1RDTO valintakoe : hakukohde.getValintakokeet()) {
          HakukohteenValintakoeDTO v = new HakukohteenValintakoeDTO();
          v.setOid(valintakoe.getOid());
          v.setTyyppiUri(valintakoe.getValintakoetyyppi());
          importTyyppi.getValintakoe().add(v);
        }
      }

      String hakukohdeKoodiTunniste = hakukohde.getOid().replace(".", "_");

      AvainArvoDTO avainArvo = new AvainArvoDTO();

      avainArvo.setAvain("hakukohde_oid");
      avainArvo.setArvo(hakukohde.getOid());
      importTyyppi.getValintaperuste().add(avainArvo);

      avainArvo = new AvainArvoDTO();
      avainArvo.setAvain("riittava_kielitaito_tunniste");
      avainArvo.setArvo(hakukohdeKoodiTunniste + "_riittava_kielitaito");
      importTyyppi.getValintaperuste().add(avainArvo);

      String opetuskieli =
          toteutukset.stream()
              .flatMap(toteutus -> toteutus.getOpetuskielis().getUris().keySet().stream())
              .map(uri -> uri.replace("kieli_", ""))
              .findAny()
              .orElse(null);
      avainArvo = new AvainArvoDTO();
      avainArvo.setAvain("opetuskieli");
      avainArvo.setArvo(opetuskieli);
      importTyyppi.getValintaperuste().add(avainArvo);

      // Kielikoetunnisteen selvittäminen
      String kielikoetunniste;
      if (StringUtils.isNotBlank(opetuskieli)) {
        kielikoetunniste = "kielikoe_" + opetuskieli;
      } else {
        kielikoetunniste = hakukohdeKoodiTunniste + "_kielikoe";
      }

      avainArvo = new AvainArvoDTO();
      avainArvo.setAvain("kielikoe_tunniste");
      avainArvo.setArvo(kielikoetunniste);
      importTyyppi.getValintaperuste().add(avainArvo);

      HakukohdeValintaperusteetV1RDTO valintaperusteet =
          tarjontaAsyncResource.findValintaperusteetByOid(hakukohdeOid).get(60, TimeUnit.SECONDS);

      if (valintaperusteet != null) {
        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_min");
        avainArvo.setArvo(valintaperusteet.getPaasykoeMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_max");
        avainArvo.setArvo(valintaperusteet.getPaasykoeMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_hylkays_min");
        avainArvo.setArvo(valintaperusteet.getPaasykoeHylkaysMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_hylkays_max");
        avainArvo.setArvo(valintaperusteet.getPaasykoeHylkaysMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("lisanaytto_min");
        avainArvo.setArvo(valintaperusteet.getLisanayttoMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("lisanaytto_max");
        avainArvo.setArvo(valintaperusteet.getLisanayttoMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("lisanaytto_hylkays_min");
        avainArvo.setArvo(valintaperusteet.getLisanayttoHylkaysMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("lisanaytto_hylkays_max");
        avainArvo.setArvo(valintaperusteet.getLisanayttoHylkaysMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_ja_lisanaytto_hylkays_min");
        avainArvo.setArvo(valintaperusteet.getHylkaysMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_ja_lisanaytto_hylkays_max");
        avainArvo.setArvo(valintaperusteet.getHylkaysMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("painotettu_keskiarvo_hylkays_min");
        avainArvo.setArvo(valintaperusteet.getPainotettuKeskiarvoHylkaysMin().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("painotettu_keskiarvo_hylkays_max");
        avainArvo.setArvo(valintaperusteet.getPainotettuKeskiarvoHylkaysMax().toString());
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("paasykoe_tunniste");
        avainArvo.setArvo(
            valintaperusteet.getPaasykoeTunniste() != null
                ? valintaperusteet.getPaasykoeTunniste()
                : hakukohdeKoodiTunniste + "_paasykoe");
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("lisanaytto_tunniste");
        avainArvo.setArvo(
            valintaperusteet.getLisanayttoTunniste() != null
                ? valintaperusteet.getLisanayttoTunniste()
                : hakukohdeKoodiTunniste + "_lisanaytto");
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("lisapiste_tunniste");
        avainArvo.setArvo(
            valintaperusteet.getLisapisteTunniste() != null
                ? valintaperusteet.getLisapisteTunniste()
                : hakukohdeKoodiTunniste + "_lisapiste");
        importTyyppi.getValintaperuste().add(avainArvo);

        avainArvo = new AvainArvoDTO();
        avainArvo.setAvain("urheilija_lisapiste_tunniste");
        avainArvo.setArvo(
            valintaperusteet.getUrheilijaLisapisteTunniste() != null
                ? valintaperusteet.getUrheilijaLisapisteTunniste()
                : hakukohdeKoodiTunniste + "_urheilija_lisapiste");
        importTyyppi.getValintaperuste().add(avainArvo);

        for (String avain : valintaperusteet.getPainokertoimet().keySet()) {
          avainArvo = new AvainArvoDTO();
          avainArvo.setAvain(avain);
          avainArvo.setArvo(valintaperusteet.getPainokertoimet().get(avain));
          importTyyppi.getValintaperuste().add(avainArvo);
        }
      }

      return importTyyppi;
    } catch (Exception e) {
      String msg = String.format("Importointi hakukohteelle %s epaonnistui!", hakukohdeOid);
      LOG.error(msg, e);
      throw new RuntimeException(msg, e);
    }
  }
}
