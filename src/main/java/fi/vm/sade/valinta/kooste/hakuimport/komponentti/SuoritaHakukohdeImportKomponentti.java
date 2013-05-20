package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdeImportTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdekoodiTyyppi;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import org.apache.camel.language.Simple;
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
                                       @Simple("${property.hakukohde}") HakukohdeTyyppi hakukohde) {
        HakukohdekoodiTyyppi koodi = new HakukohdekoodiTyyppi();

        // Hakukohdenimi on todellisuudessa hakukohteen määrittävän koodin URI. Tarjonta ei jostakin syystä
        // tallenna hakukohteen oikeaa nimeä kantaansa. Pitää kaivella tämä koodistosta jossakin vaiheessa.
        koodi.setKoodiUri(hakukohde.getHakukohdeNimi());
        koodi.setArvo(hakukohde.getHakukohdeNimi());
        koodi.setNimiFi(hakukohde.getHakukohdeNimi());
        koodi.setNimiSv(hakukohde.getHakukohdeNimi());
        koodi.setNimiEn(hakukohde.getHakukohdeNimi());

        HakukohdeImportTyyppi hakukohdeImport = new HakukohdeImportTyyppi();
        hakukohdeImport.setHakukohdekoodi(koodi);
        hakukohdeImport.setHakukohdeOid(hakukohde.getOid());
        hakukohdeImport.setHakuOid(hakuOid);

        valintaperusteService.tuoHakukohde(hakukohdeImport);
    }

}
