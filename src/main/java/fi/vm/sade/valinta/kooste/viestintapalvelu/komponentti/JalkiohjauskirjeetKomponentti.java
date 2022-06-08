package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.util.TuloskirjeNimiPaattelyStrategy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Letter;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Sijoitus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.model.types.ContentStructureType;
import fi.vm.sade.valinta.kooste.viestintapalvelu.model.types.KirjeenVastaanottaja;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetUtil;
import fi.vm.sade.valintalaskenta.domain.dto.SyotettyArvoDTO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.camel.Body;
import org.apache.camel.Property;
import org.apache.camel.language.Simple;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JalkiohjauskirjeetKomponentti {
  private static final Logger LOG = LoggerFactory.getLogger(JalkiohjauskirjeetKomponentti.class);

  public static LetterBatch teeJalkiohjauskirjeet(
      Map<String, Koodi> maatjavaltiot1,
      Map<String, Koodi> postinumerot,
      String ylikirjoitettuPreferoitukielikoodi,
      @Body final Collection<HakijaDTO> hyvaksymattomatHakijat,
      Map<String, HakemusWrapper> hakemusOidHakemukset,
      Map<String, Map<String, List<SyotettyArvoDTO>>> syotetytArvot,
      final Map<String, MetaHakukohde> jalkiohjauskirjeessaKaytetytHakukohteet,
      @Simple("${property.hakuOid}") String hakuOid,
      @Property("templateName") String templateName,
      @Property("sisalto") String sisalto,
      @Property("tag") String tag,
      boolean sahkoinenMassaposti,
      boolean isKkHaku,
      List<ContentStructureType> sisaltotyypit,
      KirjeenVastaanottaja kirjeenVastaanottaja) {
    final int kaikkiHyvaksymattomat = hyvaksymattomatHakijat.size();
    if (kaikkiHyvaksymattomat == 0) {
      LOG.error(
          "Jälkiohjauskirjeitä yritetään luoda haulle jolla kaikki hakijat on hyväksytty koulutukseen!");
      throw new SijoittelupalveluException(
          "Sijoittelupalvelun mukaan kaikki hakijat on hyväksytty johonkin koulutukseen!");
    }
    LOG.info("Aloitetaan {} kpl jälkiohjauskirjeen luonti", kaikkiHyvaksymattomat);
    final List<Letter> kirjeet = new ArrayList<>();
    final boolean kaytetaanYlikirjoitettuKielikoodia =
        StringUtils.isNotBlank(ylikirjoitettuPreferoitukielikoodi);
    String preferoituKielikoodi =
        kaytetaanYlikirjoitettuKielikoodia ? ylikirjoitettuPreferoitukielikoodi : KieliUtil.SUOMI;
    int count = 0;
    for (HakijaDTO hakija : hyvaksymattomatHakijat) {
      try {
        final String hakemusOid = hakija.getHakemusOid();
        if (!hakemusOidHakemukset.containsKey(hakemusOid)) {
          continue;
        }
        final HakemusWrapper hakemus = hakemusOidHakemukset.get(hakemusOid);
        final Osoite osoite =
            OsoiteHakemukseltaUtil.osoiteHakemuksesta(
                hakemus, maatjavaltiot1, postinumerot, new TuloskirjeNimiPaattelyStrategy());
        final List<Map<String, Object>> tulosList = new ArrayList<>();
        if (!kaytetaanYlikirjoitettuKielikoodia) {
          preferoituKielikoodi = hakemus.getAsiointikieli();
        }

        for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
          String hakukohdeOid = hakutoive.getHakukohdeOid();
          List<SyotettyArvoDTO> arvot =
              syotetytArvot
                  .get(hakukohdeOid)
                  .getOrDefault(hakija.getHakemusOid(), Collections.emptyList());
          Map<String, Object> tulokset =
              KirjeetUtil.getTuloksetMap(
                  jalkiohjauskirjeessaKaytetytHakukohteet,
                  hakukohdeOid,
                  preferoituKielikoodi,
                  hakutoive,
                  arvot);

          StringBuilder omatPisteet = new StringBuilder();
          StringBuilder hyvaksytyt = new StringBuilder();
          //
          // VT-1036
          //
          List<Sijoitus> sijoitukset = Lists.newArrayList();
          Collections.sort(
              hakutoive.getHakutoiveenValintatapajonot(), KirjeetUtil.sortByPrioriteetti());
          KirjeetUtil.jononTulokset(
              osoite, hakutoive, omatPisteet, hyvaksytyt, sijoitukset, preferoituKielikoodi);
          tulokset.put("sijoitukset", sijoitukset);

          Collections.sort(hakutoive.getHakutoiveenValintatapajonot(), KirjeetUtil.sortByTila());
          List<HakutoiveenValintatapajonoDTO> hakutoiveenValintatapajonot =
              hakutoive.getHakutoiveenValintatapajonot();
          KirjeetUtil.putValinnanTulosHylkausPerusteAndVarasijaData(
              preferoituKielikoodi, tulokset, hakutoiveenValintatapajonot);
          tulokset.put("omatPisteet", omatPisteet.toString());
          tulokset.put("hyvaksytyt", hyvaksytyt.toString());
          tulosList.add(tulokset);
        }
        Map<String, Object> replacements = Maps.newHashMap();
        replacements.put("tulokset", tulosList);
        replacements.put("hakemusOid", hakemus.getOid());
        replacements.put("henkilotunnus", hakemus.getHenkilotunnus());
        replacements.put("syntymaaika", hakemus.getSyntymaaika());
        replacements.put("hakijaOid", hakija.getHakijaOid());

        List<String> sahkopostit = new ArrayList<>();
        if (kirjeenVastaanottaja.equals(KirjeenVastaanottaja.HUOLTAJAT)) {
          List<String> huoltajienSahkopostit = hakemus.getHuoltajienSahkopostiosoitteet();
          sahkopostit.addAll(huoltajienSahkopostit);
        } else {
          sahkopostit.add(hakemus.getSahkopostiOsoite());
        }
        boolean skipIPosti = sahkoinenMassaposti && !sendIPosti(hakemus);
        for (String sahkoposti : sahkopostit) {
          kirjeet.add(
              new Letter(
                  osoite,
                  templateName,
                  preferoituKielikoodi,
                  replacements,
                  hakija.getHakijaOid(),
                  skipIPosti,
                  sahkoposti,
                  hakija.getHakemusOid()));
        }

        count++;
        if (count % 10000 == 0) {
          LOG.info("Luotu {}/{} kirjettä", count, kaikkiHyvaksymattomat);
        }
      } catch (Exception e) {
        throw new RuntimeException(
            String.format("Hakemuksen %s kirjedatan käsittely epäonnistui", hakija.getHakemusOid()),
            e);
      }
    }

    LOG.info(
        "Yritetään luoda viestintäpalvelulle jälkiohjauskirje-erä haulle {} asiointikielelä {}, jossa kirjeitä {} kappaletta!",
        hakuOid,
        preferoituKielikoodi,
        kirjeet.size());
    LetterBatch viesti = new LetterBatch(kirjeet, sisaltotyypit);
    viesti.setApplicationPeriod(hakuOid);
    viesti.setFetchTarget(null);
    viesti.setLanguageCode(preferoituKielikoodi);
    viesti.setOrganizationOid(null);
    viesti.setTag(tag);
    viesti.setTemplateName(templateName);
    viesti.setIposti(sahkoinenMassaposti && !isKkHaku ? true : false);
    viesti.setSkipDokumenttipalvelu(sahkoinenMassaposti);
    Map<String, Object> templateReplacements = Maps.newHashMap();
    templateReplacements.put("sisalto", sisalto);
    viesti.setTemplateReplacements(templateReplacements);
    LOG.debug("\r\n{}", new ViestiWrapper(viesti));
    return viesti;
  }

  private static boolean sendIPosti(HakemusWrapper hakemusWrapper) {
    return org.apache.commons.lang3.StringUtils.isBlank(hakemusWrapper.getSahkopostiOsoite())
        || !hakemusWrapper.getVainSahkoinenViestinta();
  }
}
