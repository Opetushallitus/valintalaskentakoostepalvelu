package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import com.google.common.util.concurrent.Futures;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintatietoResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.ValintalaskentaTulosExcelKomponentti;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import org.apache.camel.Property;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author Jussi Jartamo
 */
public class ValintalaskentaTulosExcelKomponenttiTest {
    public static final Gson GSON= new GsonBuilder()
            .registerTypeAdapter(Date.class, new JsonDeserializer() {
                @Override
                public Object deserialize(JsonElement json, Type typeOfT,
                                          JsonDeserializationContext context)
                        throws JsonParseException {
                    return new Date(json.getAsJsonPrimitive().getAsLong());
                }
            })
            .create();

    private String resource(String path) {

        try {
            return IOUtils.toString(new ClassPathResource("valintalaskentatulos/"+path).getInputStream());
        } catch(Throwable t) {
            return null;
        }
    }

    @Test
    public void testaaExcelinLuonti() throws Exception{
        List<HakemusOsallistuminenDTO> valintatiedot = GSON.fromJson(
                resource("Valintatieto.json"),
                new TypeToken<List<HakemusOsallistuminenDTO>>() {
                }.getType());


        Future<List<Hakemus>> hakemukset = Futures.immediateFuture(GSON.fromJson(
                resource("Listfull.json"),
                new TypeToken<List<Hakemus>>() {
                }.getType()));

        Future<List<ValintakoeDTO>> valintakoe = Futures.immediateFuture(GSON.fromJson(
                resource("Valintakoe.json"),
                new TypeToken<List<ValintakoeDTO>>() {
                }.getType()));

        ValintatietoResource valintatietoService = Mockito.mock(ValintatietoResource.class);
        ApplicationAsyncResource applicationResource = Mockito.mock(ApplicationAsyncResource.class);
        ValintaperusteetAsyncResource valintaperusteetValintakoeResource = Mockito.mock(ValintaperusteetAsyncResource.class);


        Mockito.when(valintatietoService.haeValintatiedotHakukohteelle(Mockito.anyString(), Mockito.anyList())).thenReturn(valintatiedot);
        Mockito.when(applicationResource.getApplicationsByOid(Mockito.anyString(), Mockito.anyString())).thenReturn(hakemukset);
        Mockito.when(valintaperusteetValintakoeResource.haeValintakokeet(Mockito.anyList())).thenReturn(valintakoe);

        ValintalaskentaTulosExcelKomponentti v = new ValintalaskentaTulosExcelKomponentti(
                valintatietoService,applicationResource,valintaperusteetValintakoeResource
        );

        InputStream inp = v.luoTuloksetXlsMuodossa("","",
                "",
                "",
                Arrays.asList("14249692473429084309772490040414"),
                null);

        if(false) {
            IOUtils.copy(inp, new FileOutputStream("koekutsut.xls"));
        }
    }
}
