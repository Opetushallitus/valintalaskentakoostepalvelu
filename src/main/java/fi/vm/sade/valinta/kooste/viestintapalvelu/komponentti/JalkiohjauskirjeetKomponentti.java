package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import static fi.vm.sade.valinta.kooste.util.Formatter.suomennaNumero;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetUtil;
import org.apache.camel.Body;
import org.apache.camel.Property;
import org.apache.camel.language.Simple;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Letter;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Pisteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Sijoitus;

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
            final Collection<Hakemus> hakemukset,
            final Map<String, MetaHakukohde> jalkiohjauskirjeessaKaytetytHakukohteet,
            @Simple("${property.hakuOid}") String hakuOid,
            @Property("templateName") String templateName,
            @Property("sisalto") String sisalto, @Property("tag") String tag,
            boolean sahkoinenKorkeakoulunMassaposti
    ) {
        final int kaikkiHyvaksymattomat = hyvaksymattomatHakijat.size();
        if (kaikkiHyvaksymattomat == 0) {
            LOG.error("Jälkiohjauskirjeitä yritetään luoda haulle jolla kaikki hakijat on hyväksytty koulutukseen!");
            throw new SijoittelupalveluException("Sijoittelupalvelun mukaan kaikki hakijat on hyväksytty johonkin koulutukseen!");
        }
        final Map<String, Hakemus> hakemusOidHakemukset = hakemukset.stream().collect(Collectors.toMap(Hakemus::getOid, h -> h));
        final List<Letter> kirjeet = new ArrayList<>();
        final boolean kaytetaanYlikirjoitettuKielikoodia = StringUtils.isNotBlank(ylikirjoitettuPreferoitukielikoodi);
        String preferoituKielikoodi = kaytetaanYlikirjoitettuKielikoodia ? ylikirjoitettuPreferoitukielikoodi : KieliUtil.SUOMI;
        Map<String, Koodi> maajavaltio = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        Map<String, Koodi> posti = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
        for (HakijaDTO hakija : hyvaksymattomatHakijat) {
            final String hakemusOid = hakija.getHakemusOid();
            if (!hakemusOidHakemukset.containsKey(hakemusOid)) {
                continue;
            }
            final Hakemus hakemus = hakemusOidHakemukset.get(hakemusOid);
            final Osoite osoite = HaeOsoiteKomponentti.haeOsoite(maajavaltio, posti, hakemus);
            final List<Map<String, Object>> tulosList = new ArrayList<>();
            if (!kaytetaanYlikirjoitettuKielikoodia) {
                preferoituKielikoodi = new HakemusWrapper(hakemus).getAsiointikieli();
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
                KirjeetUtil.jononTulokset(osoite, hakutoive, omatPisteet, hyvaksytyt, kkSijoitukset, false);
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
            replacements.put("henkilotunnus", new HakemusWrapper(hakemus).getHenkilotunnus());

            HakemusWrapper hakemusWrapper = new HakemusWrapper(hakemus);
            String sahkoposti = hakemusWrapper.getSahkopostiOsoite();
            boolean skipIPosti = sahkoinenKorkeakoulunMassaposti ? !sendIPosti(hakemusWrapper) : false;
            kirjeet.add(new Letter(osoite, templateName, preferoituKielikoodi, replacements,
                    hakija.getHakijaOid(), skipIPosti, sahkoposti, hakija.getHakemusOid()));
        }

        LOG.info("Yritetään luoda viestintapalvelulta jälkiohjauskirjeitä {} kappaletta!", kirjeet.size());
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
