package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Komponentti tulosten kasaamiseen Excel-muodossa
 */
@Component("luoValintalaskennanTuloksetXlsMuodossa")
public class ValintalaskennanTulosExcelKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskennanTulosExcelKomponentti.class);

    // private ValintalaskentaTulosService valintatietoService;

    public InputStream luoTuloksetXlsMuodossa(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
            @Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset) {
        Map<String, String> oidToName = new HashMap<String, String>();
        for (HakemusTyyppi hakemus : hakemukset) {
            StringBuilder b = new StringBuilder();
            b.append(hakemus.getHakijanSukunimi()).append(", ").append(hakemus.getHakijanEtunimi());
            oidToName.put(hakemus.getHakemusOid(), b.toString());
        }

        return null;
    }

}
