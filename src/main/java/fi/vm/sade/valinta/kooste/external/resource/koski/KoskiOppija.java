package fi.vm.sade.valinta.kooste.external.resource.koski;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Set;

public class KoskiOppija {
    private KoskiHenkilö henkilö;
    private JsonArray opiskeluoikeudet;

    public String getOppijanumero() {
        return henkilö.oid;
    }

    public JsonArray getOpiskeluoikeudet() {
        return opiskeluoikeudet;
    }

    public void setHenkilö(KoskiHenkilö henkilö) {
        this.henkilö = henkilö;
    }

    public JsonArray haeOpiskeluoikeudet(Set<String> koskenOpiskeluoikeusTyypit) {
        if (opiskeluoikeudet == null) {
            return null;
        }
        JsonArray tulos = new JsonArray();
        opiskeluoikeudet.forEach(opiskeluoikeus -> {
            if (koskenOpiskeluoikeusTyypit.contains(tyypinKoodiarvo(opiskeluoikeus))) {
                tulos.add(opiskeluoikeus);
            }
        });
        return tulos;
    }

    private String tyypinKoodiarvo(JsonElement opiskeluoikeus) {
        return opiskeluoikeus.getAsJsonObject().get("tyyppi").getAsJsonObject().get("koodiarvo").getAsString();
    }

    public static class KoskiHenkilö {
        public String oid;

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
