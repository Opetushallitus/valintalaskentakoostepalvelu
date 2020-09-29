package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.koulutus.KoulutusV1RDTO;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.Kieli;
import fi.vm.sade.valinta.kooste.util.NimiPaattelyStrategy;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Letter;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.model.types.ContentStructureType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.camel.Body;
import org.apache.camel.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KoekutsukirjeetKomponentti {
  private static final Logger LOG = LoggerFactory.getLogger(KoekutsukirjeetKomponentti.class);

  private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;
  private final TarjontaAsyncResource tarjontaAsyncResource;
  private final OrganisaatioAsyncResource organisaatioAsyncResource;

  @Autowired
  public KoekutsukirjeetKomponentti(
      KoodistoCachedAsyncResource koodistoCachedAsyncResource,
      TarjontaAsyncResource tarjontaAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource) {
    this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    this.tarjontaAsyncResource = tarjontaAsyncResource;
    this.organisaatioAsyncResource = organisaatioAsyncResource;
  }

  public LetterBatch valmistaKoekutsukirjeet(
      @Body List<HakemusWrapper> hakemukset,
      @Property(OPH.HAKUOID) String hakuOid,
      @Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
      Map<String, Collection<String>> hakemusOidJaMuutHakukohdeOids,
      @Property(OPH.LETTER_BODY_TEXT) String letterBodyText,
      @Property(OPH.TARJOAJAOID) String tarjoajaOid,
      @Property("tag") String tag,
      @Property("templateName") String templateName)
      throws Exception {
    try {
      LOG.info(
          "Luodaan koekutsukirjeet {} hakemukselle. Hakukohde({})",
          hakemukset.size(),
          hakukohdeOid);
      final List<Letter> kirjeet = Lists.newArrayList();
      // Custom contents?
      final List<Map<String, String>> customLetterContents = Collections.emptyList();

      // final HakukohdeNimiRDTO nimi;
      Map<String, HakukohdeV1RDTO> hakukohteet;
      try {
        Set<String> kaikkiMuutHakutoiveetOids =
            hakemusOidJaMuutHakukohdeOids.entrySet().stream()
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toSet());
        kaikkiMuutHakutoiveetOids.add(hakukohdeOid);
        hakukohteet =
            CompletableFutureUtil.sequence(
                    kaikkiMuutHakutoiveetOids.stream()
                        .collect(
                            Collectors.toMap(
                                h -> h, h -> tarjontaAsyncResource.haeHakukohde(hakukohdeOid))))
                .get(5, TimeUnit.MINUTES);
      } catch (Exception e) {
        LOG.error("Tarjonnalta ei saatu hakukohteelle({}) nimea!", hakukohdeOid);
        throw e;
      }

      HakukohdeV1RDTO hakukohde = hakukohteet.get(hakukohdeOid);
      List<Organisaatio> tarjoajat =
          CompletableFutureUtil.sequence(
                  hakukohde.getTarjoajaOids().stream()
                      .map(organisaatioAsyncResource::haeOrganisaatio)
                      .collect(Collectors.toList()))
              .get(5, TimeUnit.MINUTES);
      List<KoulutusV1RDTO> toteutukset =
          CompletableFutureUtil.sequence(
                  hakukohde.getHakukohdeKoulutusOids().stream()
                      .map(tarjontaAsyncResource::haeToteutus)
                      .collect(Collectors.toList()))
              .get(5, TimeUnit.MINUTES);
      String opetuskieli =
          new Kieli(
                  toteutukset.stream()
                      .flatMap(toteutus -> toteutus.getOpetuskielis().getUris().keySet().stream())
                      .map(TarjontaUriToKoodistoUtil::cleanUri)
                      .collect(Collectors.toList()))
              .getKieli();
      String hakukohdeNimiTietyllaKielella =
          Teksti.getTeksti(hakukohde.getHakukohteenNimet(), opetuskieli);
      String tarjoajaNimiTietyllaKielella =
          Teksti.getTeksti(
              tarjoajat.stream().map(Organisaatio::getNimi).collect(Collectors.toList()),
              " - ",
              opetuskieli);
      Map<String, Koodi> maajavaltio =
          koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
      Map<String, Koodi> posti =
          koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
      for (HakemusWrapper hakemus : hakemukset) {
        Osoite addressLabel =
            OsoiteHakemukseltaUtil.osoiteHakemuksesta(
                hakemus, maajavaltio, posti, new NimiPaattelyStrategy());
        List<String> muutHakukohteet = Collections.emptyList();
        try {
          muutHakukohteet =
              hakemusOidJaMuutHakukohdeOids.get(hakemus.getOid()).stream()
                  .map(h -> Teksti.getTeksti(hakukohteet.get(h).getHakukohteenNimet(), opetuskieli))
                  .collect(Collectors.toList());
        } catch (Exception e) {
          LOG.error("valmistaKoekutsukirjeet throws", e);
        }
        Map<String, Object> replacements = Maps.newHashMap();
        replacements.put("koulu", hakukohdeNimiTietyllaKielella);
        replacements.put("koulutus", tarjoajaNimiTietyllaKielella);
        replacements.put("tulokset", customLetterContents);
        replacements.put("muut_hakukohteet", muutHakukohteet);
        kirjeet.add(
            new Letter(
                addressLabel,
                templateName,
                opetuskieli,
                replacements,
                hakemus.getSahkopostiOsoite()));
      }
      LOG.info("Luodaan koekutsukirjeet {} henkilolle", kirjeet.size());
      LetterBatch viesti =
          new LetterBatch(kirjeet, Collections.singletonList(ContentStructureType.letter));
      viesti.setApplicationPeriod(hakuOid);
      viesti.setFetchTarget(hakukohdeOid);
      viesti.setLanguageCode(opetuskieli);
      viesti.setOrganizationOid(tarjoajaOid);
      viesti.setTag(tag);
      viesti.setTemplateName(templateName);
      Map<String, Object> templateReplacements = Maps.newHashMap();
      templateReplacements.put("sisalto", letterBodyText);
      templateReplacements.put("hakukohde", hakukohdeNimiTietyllaKielella);
      templateReplacements.put("tarjoaja", tarjoajaNimiTietyllaKielella);
      viesti.setTemplateReplacements(templateReplacements);
      return viesti;
    } catch (Exception e) {
      LOG.error("Koekutsukirjeiden luonti epäonnistui!", e);
      throw e;
    }
  }
}
