package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.List;

import org.apache.camel.language.Simple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluajoResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.Hakukohde;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Komponentti luo sijoittelun tulokset excel tiedostoksi!
 */
@Component("sijoittelunTulosXlsKomponentti")
public class SijoittelunTulosExcelKomponentti {

    @Autowired
    private SijoitteluajoResource sijoitteluajoResource;

    public InputStream luoXls(@Simple("${property.sijoitteluajoId}") Long sijoitteluajoId,
            @Simple("${property.hakukohdeOid}") String hakukohdeOid,
            @Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset) {
        Hakukohde hakukohde = sijoitteluajoResource.getHakukohdeBySijoitteluajo(sijoitteluajoId, hakukohdeOid);

        return null;
    }
}
