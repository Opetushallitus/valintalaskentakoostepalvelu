package fi.vm.sade.valinta.kooste.valintatapajono.resource;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import fi.vm.sade.integrationtest.tomcat.SharedTomcat;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteTomcat;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuJson;
import fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource;
import fi.vm.sade.valinta.kooste.erillishaku.resource.dto.Prosessi;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockHenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.valintatapajono.dto.ValintatapajonoRivit;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRivi;
import fi.vm.sade.valinta.kooste.valintatapajono.service.ValintatapajonoTuontiConverter;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tunniste;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi.KORKEAKOULU;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.*;
import static fi.vm.sade.valinta.kooste.erillishaku.util.ErillishakuRiviTestUtil.laillinenRivi;
import static fi.vm.sade.valinta.kooste.erillishaku.util.ErillishakuRiviTestUtil.viallinenRiviPuuttuvillaTunnisteilla;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Jussi Jartamo
 */
public class ValintatapajonoTest {
    final static Logger LOG = LoggerFactory.getLogger(ValintatapajonoTest.class);



    @Test
    public void testaaValintatapajononTuontia() throws Exception {
        String valintatapajonoOid = "14229501603804360431186491391519";
        /*
        (String oid, String jonosija, String nimi,
                //
                String tila, String fi, String sv, String en)
        */
        ValintatapajonoRivi rivi = new ValintatapajonoRivi(
                "1.2.246.562.11.00000000181",
                "1","Ilman laskentaa","Hyväksyttävissä","","",""

        );
        ValintatapajonoRivit rivit = new ValintatapajonoRivit(Arrays.asList(rivi));
        LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(rivit));
    /*
    public static ValinnanvaiheDTO konvertoi(
            String hakuOid,
            String hakukohdeOid,
            String valintatapajonoOid,
            List<ValinnanVaiheJonoillaDTO> valintaperusteet,
            List<Hakemus> hakemukset,
            List<ValintatietoValinnanvaiheDTO> valinnanvaiheet,
            Collection<ValintatapajonoRivi> rivit) {

    }*/
        List<ValinnanVaiheJonoillaDTO> valintaperusteet =
                GSON.fromJson(classpathResourceAsString("/valintatapajono/json_tuonti_valinnanvaihe.json"), new TypeToken<List<ValinnanVaiheJonoillaDTO>>() {
                }.getType());
        List<Hakemus> hakemukset = GSON.fromJson(classpathResourceAsString("/valintatapajono/json_tuonti_listfull.json"), new TypeToken<List<Hakemus>>() {
        }.getType());
        List<ValintatietoValinnanvaiheDTO> valinnanvaihe =
                GSON.fromJson(classpathResourceAsString("/valintatapajono/json_tuonti_laskenta_valinnanvaihe.json"), new TypeToken<List<ValintatietoValinnanvaiheDTO>>() {
                }.getType());

        ValinnanvaiheDTO generoitu_valinnanvaihe =
        ValintatapajonoTuontiConverter.konvertoi("hakuOid","1.2.246.562.20.85029108298",valintatapajonoOid,valintaperusteet,hakemukset,valinnanvaihe,rivit.getRivit());

        LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(generoitu_valinnanvaihe));
        Assert.assertFalse(generoitu_valinnanvaihe.getValintatapajonot().isEmpty());
        for(ValintatietoValintatapajonoDTO jono : generoitu_valinnanvaihe.getValintatapajonot()) {
            Assert.assertEquals(jono.getOid(), valintatapajonoOid);
        }

    }
    final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new JsonDeserializer() {
                @Override
                public Object deserialize(JsonElement json, Type typeOfT,
                                          JsonDeserializationContext context)
                        throws JsonParseException {
                    return new Date(json.getAsJsonPrimitive().getAsLong());
                }
            })
            .create();
    private static String classpathResourceAsString(String path) throws Exception {
        return IOUtils.toString(new ClassPathResource(path).getInputStream());
    }
}
