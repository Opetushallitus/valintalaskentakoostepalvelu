package fi.vm.sade.valinta.kooste.kela.komponentti;

import static fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource.HENKILOTUNNUS;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.camel.Property;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

@Component("TKUVAYHVAKomponentti")
public class TKUVAYHVAExportKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(TKUVAYHVAExportKomponentti.class);
    private static final Integer KESAKUU = 6;

    @Autowired
    private SijoitteluResource sijoitteluResource;

    @Autowired
    private ApplicationResource applicationResource;

    @Value("${valintalaskentakoostepalvelu.hakemus.rest.url}")
    private String applicationResourceUrl;

    public InputStream luoTKUVAYHVA(@Property("hakuOid") String hakuOid, @Property("hakukohdeOid") String hakukohdeOid,
            @Property("lukuvuosi") Date lukuvuosi, @Property("poimintapaivamaara") Date poimintapaivamaara,
            @Property("oppilaitos") String oppilaitos, @Property("linjakoodi") String linjakoodi) {
        List<HakijaDTO> hakijat = sijoitteluResource.koulutuspaikalliset(hakuOid, SijoitteluResource.LATEST);
        if (hakijat == null || hakijat.isEmpty()) {
            throw new SijoittelupalveluException(
                    "Haku ei sisällä koulutuspaikallisia hakijoita! Tarkista että sijoittelu on suoritettu haulle!");
        }
        List<InputStream> streams = new ArrayList<InputStream>();
        for (HakijaDTO hakija : hakijat) {
            TKUVAYHVA.Builder builder = new TKUVAYHVA.Builder();
            builder.setLinjakoodi(linjakoodi);
            builder.setOppilaitos(oppilaitos);
            // builder.setValintapaivamaara(valintapaivamaara);
            builder.setSukunimi(hakija.getSukunimi());
            builder.setEtunimet(hakija.getEtunimi());
            try {
                Hakemus hakemus = applicationResource.getApplicationByOid(hakija.getHakemusOid());
                String standardinMukainenHenkilotunnus = hakemus.getAnswers().getHenkilotiedot().get(HENKILOTUNNUS);
                // KELA ei halua vuosisata merkkia henkilotunnukseen!
                StringBuilder kelanVaatimaHenkilotunnus = new StringBuilder();
                kelanVaatimaHenkilotunnus.append(standardinMukainenHenkilotunnus.substring(0, 6)).append(
                        standardinMukainenHenkilotunnus.substring(7, 11));
                builder.setHenkilotunnus(kelanVaatimaHenkilotunnus.toString());
            } catch (Exception e) {
                LOG.error("Henkilötunnuksen hakeminen hakemuspalvelulta hakemukselle {} epäonnistui!",
                        hakija.getHakemusOid());
                e.printStackTrace();
                builder.setHenkilotunnus("XXXXXXXXXX");
            }
            builder.setLukuvuosi(lukuvuosi);
            builder.setPoimintapaivamaara(poimintapaivamaara);
            DateTime dateTime = new DateTime(lukuvuosi);
            if (dateTime.getMonthOfYear() > KESAKUU) { // myohemmin kuin
                                                       // kesakuussa!
                builder.setSyksyllaAlkavaKoulutus();
            } else {
                builder.setKevaallaAlkavaKoulutus();
            }
            streams.add(new ByteArrayInputStream(builder.build().toByteArray()));
        }
        return new SequenceInputStream(Collections.enumeration(streams));
    }
}
