package fi.vm.sade.valinta.kooste.external.resource.koski;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public void setOpiskeluoikeudet(JsonArray opiskeluoikeudet) {
        this.opiskeluoikeudet = opiskeluoikeudet;
    }

    public void setHenkilö(KoskiHenkilö henkilö) {
        this.henkilö = henkilö;
    }

    public void poistaMuuntyyppisetOpiskeluoikeudetKuin(Set<String> kiinnostavatOpiskeluoikeusTyypit) {
        setOpiskeluoikeudet(haeOpiskeluoikeudet(kiinnostavatOpiskeluoikeusTyypit));
    }

    private JsonArray haeOpiskeluoikeudet(Set<String> koskenOpiskeluoikeusTyypit) {
        if (opiskeluoikeudet == null) {
            return null;
        }
        JsonArray tulos = new JsonArray();
        opiskeluoikeudet.forEach(opiskeluoikeus -> {
            if (koskenOpiskeluoikeusTyypit.contains(OpiskeluoikeusJsonUtil.tyypinKoodiarvo(opiskeluoikeus))) {
                tulos.add(opiskeluoikeus);
            }
        });
        return tulos;
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

    public static class OpiskeluoikeusJsonUtil {
        private static final DateTimeFormatter OPISKELUOIKEUDEN_AIKALEIMA_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

        public static String tyypinKoodiarvo(JsonElement opiskeluoikeus) {
            return opiskeluoikeus.getAsJsonObject().get("tyyppi").getAsJsonObject().get("koodiarvo").getAsString();
        }

        public static boolean onUudempiKuin(LocalDate leikkuriPvm, JsonElement opiskeluoikeus) {
            return onUudempiKuin(leikkuriPvm, aikaleima(opiskeluoikeus));
        }

        public static boolean onUudempiKuin(LocalDate leikkuriPvm, LocalDateTime aikaleima) {
            return aikaleima.isAfter(leikkuriPvm.plusDays(1).atStartOfDay());
        }

        public static LocalDateTime aikaleima(JsonElement opiskeluoikeus) {
            String aikaleima = opiskeluoikeus.getAsJsonObject().get("aikaleima").getAsString();
            return LocalDateTime.parse(aikaleima, OPISKELUOIKEUDEN_AIKALEIMA_FORMAT);
        }

        public static String oid(JsonElement opiskeluoikeus) {
            return opiskeluoikeus.getAsJsonObject().get("oid").getAsString();
        }

        public static int versionumero(JsonElement opiskeluoikeus) {
            return opiskeluoikeus.getAsJsonObject().get("versionumero").getAsInt();
        }
    }
}
