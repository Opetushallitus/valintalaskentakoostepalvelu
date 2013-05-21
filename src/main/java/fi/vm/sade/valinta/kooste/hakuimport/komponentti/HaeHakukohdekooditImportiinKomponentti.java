package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.common.KieliType;
import fi.vm.sade.koodisto.service.types.common.KoodiMetadataType;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.koodisto.util.KoodiServiceSearchCriteriaBuilder;
import fi.vm.sade.koodisto.util.KoodistoHelper;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.valinta.kooste.hakuimport.wrapper.Hakukohde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * User: wuoti
 * Date: 21.5.2013
 * Time: 12.51
 */
@Component("haeHakukohdekooditImportiinKomponentti")
public class HaeHakukohdekooditImportiinKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HaeHakukohdekooditImportiinKomponentti.class);

    @Autowired
    private KoodiService koodiService;

    public final String URI_DELIMITER = "#";

    public List<Hakukohde> haeHakukohdekoodit(List<HakukohdeTyyppi> hakukohteet) {
        List<Hakukohde> importit = new ArrayList<Hakukohde>();

        // TODO: Tämä luukuttaa ikävästi koodistoa jokaiselle koodille erikseen. Fiksumpi ratkaisu olis kova sana.
        for (HakukohdeTyyppi h : hakukohteet) {
            String koodiUri = null;
            Integer koodiVersio = null;

            // Otetaan koodin uri ja versio erilleen
            if (h.getHakukohdeNimi().contains(URI_DELIMITER)) {
                String[] uriJaVersio = h.getHakukohdeNimi().split(URI_DELIMITER);
                koodiUri = uriJaVersio[0];
                try {
                    koodiVersio = Integer.valueOf(uriJaVersio[1]);
                } catch (NumberFormatException e) {
                    koodiVersio = null;
                }
            } else {
                koodiUri = h.getHakukohdeNimi();
            }

            Hakukohde hk = new Hakukohde();
            try {
                // Haetaan hakukohdekoodi
                KoodiType koodi = null;
                if (koodiVersio != null) {
                    koodi = koodiService.searchKoodis(KoodiServiceSearchCriteriaBuilder.koodiByUriAndVersion(koodiUri, koodiVersio)).get(0);
                } else {
                    koodi = koodiService.searchKoodis(KoodiServiceSearchCriteriaBuilder.latestKoodisByUris(koodiUri)).get(0);
                }

                hk.setHakukohdeOid(h.getOid());
                hk.setHakukohdeKoodiArvo(koodi.getKoodiArvo());
                hk.setHakukohdeKoodiUri(koodi.getKoodiUri());

                KoodiMetadataType metaFi = KoodistoHelper.getKoodiMetadataForLanguage(koodi, KieliType.FI);
                KoodiMetadataType metaSv = KoodistoHelper.getKoodiMetadataForLanguage(koodi, KieliType.SV);
                KoodiMetadataType metaEn = KoodistoHelper.getKoodiMetadataForLanguage(koodi, KieliType.EN);

                hk.setNimiFi(metaFi != null ? metaFi.getNimi() : null);
                hk.setNimiSv(metaSv != null ? metaSv.getNimi() : null);
                hk.setNimiEn(metaEn != null ? metaEn.getNimi() : null);

                importit.add(hk);
            } catch (Exception e) {
                LOG.warn("Koodin hakeminen hakukohteelle {} heitti poikkeuksen: {} - {}. " +
                        "Käytetään hakukohteen nimenä koodin URI-tunnistetta {}.",
                        new Object[]{h.getOid(), e.getCause(), e.getMessage(), h.getHakukohdeNimi()});

                importit.add(new Hakukohde(h.getHakukohdeNimi(), h.getHakukohdeNimi(),
                        h.getOid(), h.getHakukohdeNimi(),
                        h.getHakukohdeNimi(), h.getHakukohdeNimi()));
            }
        }

        return importit;
    }
}
