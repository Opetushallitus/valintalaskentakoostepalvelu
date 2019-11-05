package fi.vm.sade.valinta.kooste.external.resource.koski;

import com.google.gson.JsonArray;

public class KoskiOppija {
    private KoskiHenkilö henkilö;
    private JsonArray opiskeluoikeudet;

    public String getOppijanumero() {
        return henkilö.oid;
    }

    public JsonArray getOpiskeluoikeudet() {
        return opiskeluoikeudet;
    }

    public static class KoskiHenkilö {
        public String oid;
    }
}
