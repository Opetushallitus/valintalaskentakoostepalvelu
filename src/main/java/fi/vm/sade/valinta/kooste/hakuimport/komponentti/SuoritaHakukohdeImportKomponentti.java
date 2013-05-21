package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdeImportTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdekoodiTyyppi;
import fi.vm.sade.valinta.kooste.hakuimport.wrapper.Hakukohde;
import org.apache.camel.language.Simple;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * User: wuoti
 * Date: 20.5.2013
 * Time: 10.46
 */
@Component("suoritaHakukohdeImportKomponentti")
public class SuoritaHakukohdeImportKomponentti {

    @Autowired
    private ValintaperusteService valintaperusteService;

    public void suoritaHakukohdeImport(@Simple("${property.hakuOid}") String hakuOid,
                                       @Simple("${property.hakukohde}") Hakukohde hakukohde) {
        HakukohdekoodiTyyppi koodi = new HakukohdekoodiTyyppi();

        koodi.setKoodiUri(hakukohde.getHakukohdeKoodiUri());
        koodi.setArvo(hakukohde.getHakukohdeKoodiArvo());
        koodi.setNimiFi(hakukohde.getNimiFi());
        koodi.setNimiSv(hakukohde.getNimiSv());
        koodi.setNimiEn(hakukohde.getNimiEn());

        HakukohdeImportTyyppi hakukohdeImport = new HakukohdeImportTyyppi();
        hakukohdeImport.setHakukohdekoodi(koodi);

        if (StringUtils.isNotBlank(hakukohde.getNimiFi())) {
            hakukohdeImport.setNimi(hakukohde.getNimiFi());
        } else if (StringUtils.isNotBlank(hakukohde.getNimiSv())) {
            hakukohdeImport.setNimi(hakukohde.getNimiSv());
        } else if (StringUtils.isNotBlank(hakukohde.getNimiEn())) {
            hakukohdeImport.setNimi(hakukohde.getNimiEn());
        } else {
            hakukohdeImport.setNimi(hakukohde.getHakukohdeOid());
        }

        hakukohdeImport.setHakukohdeOid(hakukohde.getHakukohdeOid());
        hakukohdeImport.setHakuOid(hakuOid);

        valintaperusteService.tuoHakukohde(hakukohdeImport);
    }

}
