package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import fi.vm.sade.service.valintaperusteet.dto.AvainArvoDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdekoodiDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohteenValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.MonikielinenTekstiDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.koulutus.KoulutusV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Hakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Valintakoe;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
      Hakukohde hakukohde =
          tarjontaAsyncResource.haeHakukohde(hakukohdeOid).get(5, TimeUnit.MINUTES);
      Haku haku = tarjontaAsyncResource.haeHaku(hakukohde.hakuOid).get(5, TimeUnit.MINUTES);
      List<KoulutusV1RDTO> toteutukset =
          CompletableFutureUtil.sequence(
                  hakukohde.toteutusOids.stream()
                      .map(tarjontaAsyncResource::haeToteutus)
                      .collect(Collectors.toList()))
              .get(5, TimeUnit.MINUTES);
      Map<String, Koodi> kaudet = koodistoAsyncResource.haeKoodisto("kausi");
      HakukohdeImportDTO importTyyppi = new HakukohdeImportDTO();

      importTyyppi.setTarjoajaOid(hakukohde.tarjoajaOids.iterator().next());
      importTyyppi.setTarjoajaOids(hakukohde.tarjoajaOids);
      importTyyppi.setHaunkohdejoukkoUri(haku.kohdejoukkoUri);

      CompletableFutureUtil.sequence(
              hakukohde.tarjoajaOids.stream()
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

      hakukohde.nimi.forEach(
          (kieli, nimi) -> {
            MonikielinenTekstiDTO dto = new MonikielinenTekstiDTO();
            dto.setLang(kieli);
            dto.setText(nimi);
            importTyyppi.getHakukohdeNimi().add(dto);
          });

      if (haku.hakukausiUri != null) {
        kaudet.forEach(
            (arvo, koodi) -> {
              if (haku.hakukausiUri.startsWith(koodi.getKoodiUri())) {
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
      }

      if (haku.hakukausiVuosi != null) {
        importTyyppi.setHakuVuosi(Integer.toString(haku.hakukausiVuosi));
      }

      HakukohdekoodiDTO hkt = new HakukohdekoodiDTO();
      hkt.setKoodiUri(
          Objects.requireNonNullElse(
              hakukohde.hakukohteetUri, "hakukohteet_" + hakukohde.oid.replace(".", "")));
      importTyyppi.setHakukohdekoodi(hkt);

      importTyyppi.setHakukohdeOid(hakukohde.oid);
      importTyyppi.setHakuOid(hakukohde.hakuOid);
      importTyyppi.setTila(hakukohde.tila.name());
      importTyyppi.setValinnanAloituspaikat(
          Objects.requireNonNullElse(hakukohde.valintojenAloituspaikat, 0));
      for (Valintakoe valintakoe : hakukohde.valintakokeet) {
        HakukohteenValintakoeDTO v = new HakukohteenValintakoeDTO();
        v.setOid(valintakoe.oid);
        v.setTyyppiUri(valintakoe.valintakokeentyyppiUri);
        importTyyppi.getValintakoe().add(v);
      }

      String hakukohdeKoodiTunniste = hakukohde.oid.replace(".", "_");

      AvainArvoDTO avainArvo = new AvainArvoDTO();

      avainArvo.setAvain("hakukohde_oid");
      avainArvo.setArvo(hakukohde.oid);
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
