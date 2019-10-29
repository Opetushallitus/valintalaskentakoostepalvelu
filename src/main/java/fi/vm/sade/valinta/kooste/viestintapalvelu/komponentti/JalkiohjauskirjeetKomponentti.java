package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
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
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetUtil;
import org.apache.camel.Body;
import org.apache.camel.Property;
import org.apache.camel.language.Simple;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class JalkiohjauskirjeetKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(JalkiohjauskirjeetKomponentti.class);

    private final KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    @Autowired
    public JalkiohjauskirjeetKomponentti(KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    }

    public LetterBatch teeJalkiohjauskirjeet(
            String ylikirjoitettuPreferoitukielikoodi,
            @Body final Collection<HakijaDTO> hyvaksymattomatHakijat,
            final Collection<HakemusWrapper> hakemukset,
            final Map<String, MetaHakukohde> jalkiohjauskirjeessaKaytetytHakukohteet,
            @Simple("${property.hakuOid}") String hakuOid,
            @Property("templateName") String templateName,
            @Property("sisalto") String sisalto,
            @Property("tag") String tag,
            boolean sahkoinenKorkeakoulunMassaposti
    ) {
        Map<String, Koodi> maatjavaltiot1 = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        Map<String, Koodi> postinumero = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
        return teeJalkiohjauskirjeet(
                maatjavaltiot1,
                postinumero,
                ylikirjoitettuPreferoitukielikoodi,
                hyvaksymattomatHakijat,
                hakemukset.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h)),
                jalkiohjauskirjeessaKaytetytHakukohteet,
                hakuOid,
                templateName,
                sisalto,
                tag,
                sahkoinenKorkeakoulunMassaposti
        );
    }

    public static LetterBatch teeJalkiohjauskirjeet(
            Map<String, Koodi> maatjavaltiot1,
            Map<String, Koodi> postinumerot,
            String ylikirjoitettuPreferoitukielikoodi,
            @Body final Collection<HakijaDTO> hyvaksymattomatHakijat,
            Map<String, HakemusWrapper> hakemusOidHakemukset,
            final Map<String, MetaHakukohde> jalkiohjauskirjeessaKaytetytHakukohteet,
            @Simple("${property.hakuOid}") String hakuOid,
            @Property("templateName") String templateName,
            @Property("sisalto") String sisalto,
            @Property("tag") String tag,
            boolean sahkoinenKorkeakoulunMassaposti
    ) {
        final int kaikkiHyvaksymattomat = hyvaksymattomatHakijat.size();
        if (kaikkiHyvaksymattomat == 0) {
            LOG.error("Jälkiohjauskirjeitä yritetään luoda haulle jolla kaikki hakijat on hyväksytty koulutukseen!");
            throw new SijoittelupalveluException("Sijoittelupalvelun mukaan kaikki hakijat on hyväksytty johonkin koulutukseen!");
        }
        LOG.info("Aloitetaan {} kpl jälkiohjauskirjeen luonti", kaikkiHyvaksymattomat);
        final List<Letter> kirjeet = new ArrayList<>();
        final boolean kaytetaanYlikirjoitettuKielikoodia = StringUtils.isNotBlank(ylikirjoitettuPreferoitukielikoodi);
        String preferoituKielikoodi = kaytetaanYlikirjoitettuKielikoodia ? ylikirjoitettuPreferoitukielikoodi : KieliUtil.SUOMI;
        int count = 0;
        for (HakijaDTO hakija : hyvaksymattomatHakijat) {
            final String hakemusOid = hakija.getHakemusOid();
            if (!hakemusOidHakemukset.containsKey(hakemusOid)) {
                continue;
            }
            final HakemusWrapper hakemus = hakemusOidHakemukset.get(hakemusOid);
            final Osoite osoite = OsoiteHakemukseltaUtil.osoiteHakemuksesta(hakemus, maatjavaltiot1, postinumerot, new TuloskirjeNimiPaattelyStrategy());
            final List<Map<String, Object>> tulosList = new ArrayList<>();
            if (!kaytetaanYlikirjoitettuKielikoodia) {
                preferoituKielikoodi = hakemus.getAsiointikieli();
            }

            for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
                String hakukohdeOid = hakutoive.getHakukohdeOid();
                Map<String, Object> tulokset = KirjeetUtil.getTuloksetMap(jalkiohjauskirjeessaKaytetytHakukohteet, hakukohdeOid, preferoituKielikoodi, hakutoive);

                StringBuilder omatPisteet = new StringBuilder();
                StringBuilder hyvaksytyt = new StringBuilder();
                //
                // VT-1036
                //
                List<Sijoitus> kkSijoitukset = Lists.newArrayList();
                Collections.sort(hakutoive.getHakutoiveenValintatapajonot(), KirjeetUtil.sortByPrioriteetti());
                KirjeetUtil.jononTulokset(osoite, hakutoive, omatPisteet, hyvaksytyt, kkSijoitukset, false, preferoituKielikoodi);
                tulokset.put("sijoitukset", kkSijoitukset);

                Collections.sort(hakutoive.getHakutoiveenValintatapajonot(), KirjeetUtil.sortByTila());
                List<HakutoiveenValintatapajonoDTO> hakutoiveenValintatapajonot = hakutoive.getHakutoiveenValintatapajonot();
                KirjeetUtil.putValinnanTulosHylkausPerusteAndVarasijaData(preferoituKielikoodi, tulokset, hakutoiveenValintatapajonot);
                tulokset.put("omatPisteet", omatPisteet.toString());
                tulokset.put("hyvaksytyt", hyvaksytyt.toString());
                tulosList.add(tulokset);
            }
            Map<String, Object> replacements = Maps.newHashMap();
            replacements.put("tulokset", tulosList);
            replacements.put("henkilotunnus", hakemus.getHenkilotunnus());
            replacements.put("syntymaaika", hakemus.getSyntymaaika());

            String sahkoposti = hakemus.getSahkopostiOsoite();
            boolean skipIPosti = sahkoinenKorkeakoulunMassaposti && !sendIPosti(hakemus);
            kirjeet.add(new Letter(osoite, templateName, preferoituKielikoodi, replacements,
                    hakija.getHakijaOid(), skipIPosti, sahkoposti, hakija.getHakemusOid()));
            count++;
            if(count % 10000 == 0) {
                LOG.info("Luotu {}/{} kirjettä", count, kaikkiHyvaksymattomat);
            }
        }

        LOG.info("Yritetään luoda viestintäpalvelulle jälkiohjauskirje-erä haulle {} asiointikielelä {}, jossa kirjeitä {} kappaletta!", hakuOid, preferoituKielikoodi, kirjeet.size());
        LetterBatch viesti = new LetterBatch(kirjeet);
        viesti.setApplicationPeriod(hakuOid);
        viesti.setFetchTarget(null);
        viesti.setLanguageCode(preferoituKielikoodi);
        viesti.setOrganizationOid(null);
        viesti.setTag(tag);
        viesti.setTemplateName(templateName);
        viesti.setIposti(true);
        viesti.setSkipDokumenttipalvelu(sahkoinenKorkeakoulunMassaposti);
        Map<String, Object> templateReplacements = Maps.newHashMap();
        templateReplacements.put("sisalto", sisalto);
        viesti.setTemplateReplacements(templateReplacements);
        LOG.debug("\r\n{}", new ViestiWrapper(viesti));
        return viesti;
    }

    private static boolean sendIPosti(HakemusWrapper hakemusWrapper) {
        return org.apache.commons.lang3.StringUtils.isBlank(hakemusWrapper.getSahkopostiOsoite()) ||
                !hakemusWrapper.getVainSahkoinenViestinta();
    }
}
