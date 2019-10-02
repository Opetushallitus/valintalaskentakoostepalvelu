package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.util.TuloskirjeNimiPaattelyStrategy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Letter;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Sijoitus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * OLETTAA ETTA KAIKILLE VALINTATAPAJONOILLE TEHDAAN HYVAKSYMISKIRJE JOS
 * HAKEMUS ON HYVAKSYTTY YHDESSAKIN!
 * Nykyisellaan hakemukset haetaan tassa komponentissa. Taytyisi
 * refaktoroida niin etta hakemukset tuodaan komponentille.
 */
@Component
public class HyvaksymiskirjeetKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetKomponentti.class);

    public static LetterBatch teeHyvaksymiskirjeet(
            Function<String, Map<String, Koodi>> haeKoodisto,
            Map<String, Optional<Osoite>> hakukohdeJaHakijapalveluidenOsoite,
            Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet,
            Collection<HakijaDTO> hakukohteenHakijat,
            Map<String, HakemusWrapper> hakemukset,
            String hakukohdeOidFromRequest,
            String hakuOid,
            Optional<String> asiointikieli,
            String sisalto,
            String tag,
            String templateName,
            String palautusPvm,
            String palautusAika,
            boolean sahkoinenKorkeakoulunMassaposti) {
        try {
            assert (hakuOid != null);
            int kaikkiHyvaksytyt = hakukohteenHakijat.size();
            LOG.info("Aloitetaan {} kpl hyväksymiskirjeen luonti. Asetetaan kaikille skipIPosti=true.", kaikkiHyvaksytyt);
            final List<Letter> kirjeet = new ArrayList<>();
            Map<String, Koodi> maajavaltio = haeKoodisto.apply(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
            Map<String, Koodi> posti = haeKoodisto.apply(KoodistoCachedAsyncResource.POSTI);
            LetterBatch viesti = new LetterBatch(kirjeet);
            if(asiointikieli.isPresent()) {
                viesti.setLanguageCode(asiointikieli.get());
            }
            int count = 0;
            for (HakijaDTO hakija : hakukohteenHakijat) {
                final String hakukohdeOid = StringUtils.isEmpty(hakukohdeOidFromRequest) ? hyvaksytynHakutoiveenHakukohdeOid(hakija) : hakukohdeOidFromRequest;
                MetaHakukohde hyvaksyttyMeta = hyvaksymiskirjeessaKaytetytHakukohteet.get(hakukohdeOid);
                Teksti koulu = hyvaksyttyMeta.getTarjoajaNimi();
                Teksti koulutus = hyvaksyttyMeta.getHakukohdeNimi();
                String preferoituKielikoodi = asiointikieli.orElse(hyvaksyttyMeta.getOpetuskieli());
                String tarjoajaOid = hyvaksyttyMeta.getTarjoajaOid();
                final String hakemusOid = hakija.getHakemusOid();
                final HakemusWrapper hakemus = Objects.requireNonNull(hakemukset.get(hakemusOid), "Hakemusta " + hakemusOid + " ei löydy");
                final Osoite osoite = OsoiteHakemukseltaUtil.osoiteHakemuksesta(hakemus, maajavaltio, posti, new TuloskirjeNimiPaattelyStrategy());
                final List<Map<String, Object>> tulosList = new ArrayList<>();

                for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
                    Map<String, Object> tulokset = KirjeetUtil.getTuloksetMap(hyvaksymiskirjeessaKaytetytHakukohteet, hakukohdeOid, preferoituKielikoodi, hakutoive);

                    StringBuilder omatPisteet = new StringBuilder();
                    StringBuilder hyvaksytyt = new StringBuilder();
                    //
                    // VT-1036
                    //
                    List<Sijoitus> kkSijoitukset = Lists.newArrayList();
                    tulokset.put("sijoitukset", kkSijoitukset);

                    final boolean valittuHakukohteeseen = hakutoive.getHakutoiveenValintatapajonot().stream().anyMatch((jono) -> jono.getTila().isHyvaksytty());
                    tulokset.put("hyvaksytty", valittuHakukohteeseen);

                    Collections.sort(hakutoive.getHakutoiveenValintatapajonot(), KirjeetUtil.sortByPrioriteetti());
                    KirjeetUtil.jononTulokset(osoite, hakutoive, omatPisteet, hyvaksytyt, kkSijoitukset, valittuHakukohteeseen, preferoituKielikoodi);

                    Collections.sort(hakutoive.getHakutoiveenValintatapajonot(), KirjeetUtil.sortByTila());
                    List<HakutoiveenValintatapajonoDTO> hakutoiveenValintatapajonot = hakutoive.getHakutoiveenValintatapajonot();
                    KirjeetUtil.putValinnanTulosHylkausPerusteAndVarasijaData(preferoituKielikoodi, tulokset, hakutoiveenValintatapajonot);
                    tulokset.put("omatPisteet", omatPisteet.toString());
                    tulokset.put("hyvaksytyt", hyvaksytyt.toString());
                    tulosList.add(tulokset);
                }
                Map<String, Object> replacements = Maps.newHashMap();
                replacements.put("palautusAika", StringUtils.trimToNull(palautusAika));
                replacements.put("palautusPvm", StringUtils.trimToNull(palautusPvm));
                replacements.put("tulokset", tulosList);
                replacements.put("koulu", koulu.getTeksti(preferoituKielikoodi, KirjeetUtil.vakioTarjoajanNimi(hakukohdeOid)));
                Optional<Osoite> hakijapalveluidenOsoite = hakukohdeJaHakijapalveluidenOsoite.get(hakukohdeOid);
                if (hakijapalveluidenOsoite != null) {
                    hakijapalveluidenOsoite.ifPresent(h -> replacements.put("hakijapalveluidenOsoite", h));
                } else {
                    LOG.error("Hakijalle (hakemusOid={},hakijaOid={}) hakutoiveessa={} ei saatu hakijapalveluiden osoitetta tarjoajalle {}", hakija.getHakemusOid(), hakija.getHakijaOid(), hakukohdeOid, tarjoajaOid);
                }
                replacements.put("henkilotunnus", hakemus.getHenkilotunnus());
                replacements.put("koulutus", koulutus.getTeksti(preferoituKielikoodi, KirjeetUtil.vakioHakukohteenNimi(hakukohdeOid)));
                replacements.put("hakemusOid", hakemus.getOid());
                replacements.put("hakijaOid", hakija.getHakijaOid());

                replacements.put("hakukohde", koulutus.getTeksti(preferoituKielikoodi, KirjeetUtil.vakioHakukohteenNimi(hakukohdeOid)));
                replacements.put("tarjoaja", koulu.getTeksti(preferoituKielikoodi, KirjeetUtil.vakioTarjoajanNimi(tarjoajaOid)));
                replacements.put("ohjeetUudelleOpiskelijalle", hyvaksyttyMeta.getOhjeetUudelleOpiskelijalle());
                replacements.put("syntymaaika", hakemus.getSyntymaaika());

                String sahkoposti = hakemus.getSahkopostiOsoite();
                //boolean skipIPosti = sahkoinenKorkeakoulunMassaposti ? !sendIPosti(hakemusWrapper) : !iPosti;
                boolean skipIPosti = true;
                kirjeet.add(new Letter(osoite, templateName, preferoituKielikoodi, replacements, hakija.getHakijaOid(),
                        skipIPosti, sahkoposti, hakija.getHakemusOid()));

                viesti.setFetchTarget(hakukohdeOid);
                viesti.setOrganizationOid(tarjoajaOid);
                viesti.setLanguageCode(preferoituKielikoodi);
                count++;
                if(count % 10000 == 0) {
                    LOG.info("Luotu {}/{} kirjettä", count, kaikkiHyvaksytyt);
                }
            }

            LOG.info("Yritetään luodaviestintapalvelulle hyvaksymiskirje-erä haulle {} asiointikielellä {} , jossa kirjeitä {} kappaletta!", hakuOid, asiointikieli, kirjeet.size());
            Collections.sort(kirjeet, (o1, o2) -> {
                try {
                    return o1.getAddressLabel().getLastName().compareTo(o2.getAddressLabel().getLastName());
                } catch (Exception e) {
                    return 0;
                }
            });
            viesti.setApplicationPeriod(hakuOid);
            viesti.setTag(tag);
            viesti.setTemplateName(templateName);
            //viesti.setIposti(iPosti);
            viesti.setIposti(false);
            viesti.setSkipDokumenttipalvelu(sahkoinenKorkeakoulunMassaposti);
            Map<String, Object> templateReplacements = Maps.newHashMap();
            templateReplacements.put("sisalto", sisalto);
            viesti.setTemplateReplacements(templateReplacements);
            return viesti;
        } catch (Throwable t) {
            throw t;
        }
    }



    private static boolean sendIPosti(HakemusWrapper hakemusWrapper) {
        return org.apache.commons.lang3.StringUtils.isBlank(hakemusWrapper.getSahkopostiOsoite()) ||
           !hakemusWrapper.getVainSahkoinenViestinta();
    }

    private static String hyvaksytynHakutoiveenHakukohdeOid(HakijaDTO hakija) {
        return hakija.getHakutoiveet().stream()
                .filter(h -> h.getHakutoiveenValintatapajonot().stream()
                        .filter(j -> j.getTila().isHyvaksytty())
                        .findAny().isPresent())
                .findAny().get().getHakukohdeOid();
    }
}
