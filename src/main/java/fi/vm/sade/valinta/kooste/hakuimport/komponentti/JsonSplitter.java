package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kkammone
 * Date: 14.6.2013
 * Time: 14:44
 * To change this template use File | Settings | File Templates.
 */
@Component("jsonSplitter")
public class JsonSplitter {
        public List<String> split(String hakukohteet) {
            Gson gson = new Gson();
            Type collectionType = new TypeToken<List<JsonObject>>(){}.getType();
            List<JsonObject> details = gson.fromJson(hakukohteet, collectionType);
            List<String> oids = new ArrayList<String>();
            for(JsonObject o : details) {
                oids.add(o.get("oid").getAsString());
            }
            return oids;
        }
}
