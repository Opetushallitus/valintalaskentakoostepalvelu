package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdeImportTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdekoodiTyyppi;
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
        //Type collectionType = new TypeToken<List<JsonObject>>(){}.getType();
        JsonObject hakukohdeJson = gson.fromJson(hakukohdeData,JsonObject.class);
        JsonArray tarjoajanimi = hakukohdeJson.get("tarjoajaNimi").getAsJsonArray();
        Iterator<JsonElement> it = tarjoajanimi.iterator();
        while(it.hasNext()) {
            JsonElement next = it.next();
            next.getAsString();
        }

    /*
        HakukohdeImportTyyppi hakukohdeImport = new HakukohdeImportTyyppi();
        HakukohdekoodiTyyppi hkt  = new HakukohdekoodiTyyppi();
        hakukohdeImport.setHakukohdekoodi(hkt);
        hakukohdeImport.setHakukohdeOid(hakukohde.getOid());
        hakukohdeImport.setHakuOid(hakukohdeImport.getHakuOid());
        hkt.setKoodiUri(hakukohde.getHakukohdeNimi());

      */


        /*
        HakukohdekoodiTyyppi koodi = new HakukohdekoodiTyyppi();

        koodi.setKoodiUri(hakukohde.getHakukohdeKoodiUri());
        koodi.setArvo(hakukohde.getHakukohdeKoodiArvo());
        koodi.setNimiFi(hakukohde.getNimiFi());
        koodi.setNimiSv(hakukohde.getNimiSv());
        koodi.setNimiEn(hakukohde.getNimiEn());



        if (StringUtils.isNotBlank(hakukohde.getNimiFi())) {
            hakukohdeImport.setNimi(hakukohde.getNimiFi());
        } else if (StringUtils.isNotBlank(hakukohde.getNimiSv())) {
            hakukohdeImport.setNimi(hakukohde.getNimiSv());
        } else if (StringUtils.isNotBlank(hakukohde.getNimiEn())) {
            hakukohdeImport.setNimi(hakukohde.getNimiEn());
        } else {
            hakukohdeImport.setNimi(hakukohde.getHakukohdeOid());
        }
          */
        /*
        valintaperusteService.tuoHakukohde(hakukohdeImport);


    */

        //    System.out.println("[" + hakukohdeOid + "]\n\n" +hakukohdeData + "\n\n" +hakukohdeNimi );
    }

}
