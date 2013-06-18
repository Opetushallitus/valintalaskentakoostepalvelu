package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdeImportTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdekoodiTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.HakukohteenValintakoeTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.MonikielinenTekstiTyyppi;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import org.apache.camel.language.Simple;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

/**
 * User: wuoti
 * Date: 20.5.2013
 * Time: 10.46
 */
@Component("suoritaHakukohdeImportKomponentti")
public class SuoritaHakukohdeImportKomponentti {

    @Autowired
    private ValintaperusteService valintaperusteService;

    public void suoritaHakukohdeImport(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
                                       @Simple("${property.hakukohdeData}") String hakukohdeData,
                                       @Simple("${property.hakukohdeNimi}") String hakukohdeNimi) {


        HakukohdeImportTyyppi importTyyppi = new HakukohdeImportTyyppi();
        Gson gson = new Gson();
        HakukohdeDTO hakukohde = gson.fromJson(hakukohdeNimi,HakukohdeDTO.class);

        for(String s : hakukohde.getTarjoajaNimi().keySet()) {
            MonikielinenTekstiTyyppi m = new MonikielinenTekstiTyyppi();
            m.setLang(s);
            m.setText(hakukohde.getTarjoajaNimi().get(s));
            importTyyppi.getTarjoajaNimi().add(m);
        }
        for(String s : hakukohde.getHakukohdeNimi().keySet()) {
            MonikielinenTekstiTyyppi m = new MonikielinenTekstiTyyppi();
            m.setLang(s);
            m.setText(hakukohde.getHakukohdeNimi().get(s));
            importTyyppi.getHakukohdeNimi().add(m);
        }

        for(String s : hakukohde.getHakuKausi().keySet()) {
            MonikielinenTekstiTyyppi m = new MonikielinenTekstiTyyppi();
            m.setLang(s);
            m.setText(hakukohde.getHakuKausi().get(s));
            importTyyppi.getHakuKausi().add(m);
        }

        importTyyppi.setHakuVuosi(hakukohde.getHakuVuosi());

        hakukohde = gson.fromJson(hakukohdeData,HakukohdeDTO.class);

        for(String s : hakukohde.getOpetuskielet()) {
            importTyyppi.getOpetuskielet().add(s);
        }

        HakukohdekoodiTyyppi hkt = new HakukohdekoodiTyyppi();
        hkt.setKoodiUri(hakukohde.getHakukohdeNimiUri());
        importTyyppi.setHakukohdekoodi(hkt);

        importTyyppi.setHakukohdeOid(hakukohde.getHakukohdeOid());
        importTyyppi.setHakuOid(hakukohde.getHakuOid());
        importTyyppi.setValinnanAloituspaikat(hakukohde.getValintojenAloituspaikatLkm());

        for(ValintakoeDTO valinakoe : hakukohde.getValintakoes()) {
            HakukohteenValintakoeTyyppi v = new HakukohteenValintakoeTyyppi();
            v.setOid(valinakoe.getOid());
            v.setTyyppiUri(valinakoe.getTyyppiUri());
            importTyyppi.getValintakoe().add(v);
        }

       valintaperusteService.tuoHakukohde(importTyyppi);

    }

}
