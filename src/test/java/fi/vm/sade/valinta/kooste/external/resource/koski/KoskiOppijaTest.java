package fi.vm.sade.valinta.kooste.external.resource.koski;

import static org.junit.Assert.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.LocalDateTime;

@RunWith(JUnit4.class)
public class KoskiOppijaTest {
    private static final Gson GSON = new Gson();

    @Test
    public void opiskeluoikeenAikaleimassaVoiOllaEriMääräDesimaaleja() {
        assertEquals(2018, aikaleima("{\"aikaleima\": \"2018-11-05T09:47:33.610999\"}").getYear());
        assertEquals(33, aikaleima("{\"aikaleima\": \"2018-11-05T09:47:33.610999\"}").getSecond());

        assertEquals(2018, aikaleima("{\"aikaleima\": \"2018-11-05T09:47:33.610\"}").getYear());
        assertEquals(33, aikaleima("{\"aikaleima\": \"2018-11-05T09:47:33.610\"}").getSecond());

        assertEquals(2018, aikaleima("{\"aikaleima\": \"2018-11-05T09:47:33\"}").getYear());
        assertEquals(33, aikaleima("{\"aikaleima\": \"2018-11-05T09:47:33\"}").getSecond());
    }

    public LocalDateTime aikaleima(String opiskeluoikeusJson) {
        return KoskiOppija.OpiskeluoikeusJsonUtil.aikaleima(GSON.fromJson(opiskeluoikeusJson, JsonElement.class));
    }
}
