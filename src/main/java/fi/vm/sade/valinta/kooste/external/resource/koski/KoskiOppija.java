package fi.vm.sade.valinta.kooste.external.resource.koski;

import com.google.gson.JsonArray;

import org.apache.commons.lang3.builder.ToStringBuilder;

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
