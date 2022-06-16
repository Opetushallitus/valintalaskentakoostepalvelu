package fi.vm.sade.valinta.kooste.hakuimport;

import static org.mockito.Mockito.when;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSyntaxException;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.kouta.KoutaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakuImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.komponentti.SuoritaHakukohdeImportKomponentti;
import fi.vm.sade.valinta.kooste.hakuimport.route.impl.HakuImportRouteImpl;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.impl.PropertyPlaceholderDelegateRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public class HakuImportTest extends CamelTestSupport {
  private static final Logger LOG = LoggerFactory.getLogger(HakuImportTest.class);

  @Produce(uri = "direct:hakuimport")
  protected ProducerTemplate template;

  @Ignore
  @Test
  public void testData() throws JsonSyntaxException, IOException {
    // ObjectMapper mapper = new ObjectMapper(); // can reuse, share
    // globally
    // HakukohdeValintaperusteetV1RDTO obj = mapper.readValue(new File(
    // "user.json"), User.class);
    HakukohdeValintaperusteetV1RDTO obj = DateDeserializer.gsonBuilder().create().fromJson(IOUtils.toString(
        new ClassPathResource("hakukohdeimport/data2/1.2.246.562.20.27059719875.json").getInputStream()),
        HakukohdeValintaperusteetV1RDTO.class);
    LOG.error("\r\n###\r\n### {}\r\n###", obj);
    LOG.error("\r\n###\r\n### {}\r\n###", new GsonBuilder().setPrettyPrinting().create().toJson(obj));
  }

  @Test
  public void testRoute() {

    template.send(new Processor() {

      @Override
      public void process(Exchange exchange) {
        exchange.setProperty("hakuOid", "hakuOid");
        exchange.setProperty(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI, new HakuImportProsessi("", ""));
        exchange.getIn().setBody("hakuOid");
      }
      // )BodyAndProperty(new Object(), "hakuOid", "hakuOid");

    });
  }

  protected RouteBuilder createRouteBuilder() {
    final String VIALLINEN_HAKUKOHDE = "throws";
    PropertyPlaceholderDelegateRegistry registry = (PropertyPlaceholderDelegateRegistry) context().getRegistry();
    JndiRegistry jndiRegistry = (JndiRegistry) registry.getRegistry();
    jndiRegistry.bind("hakuImportValvomo", Mockito.mock(ValvomoAdminService.class));
    SuoritaHakuImportKomponentti suoritaHakuImportKomponentti = new SuoritaHakuImportKomponentti() {
      @Override
      public Collection<String> suoritaHakukohdeImport(String hakuOid) {
        return Arrays.asList(VIALLINEN_HAKUKOHDE, "1.2.246.562.20.27059719875", VIALLINEN_HAKUKOHDE,
            "1.2.246.562.20.27059719875");
      }
    };
    ValintaperusteetAsyncResource valintaperusteetRestResource = Mockito.mock(ValintaperusteetAsyncResource.class);
    TarjontaAsyncResource tarjontaAsyncResource = Mockito.mock(TarjontaAsyncResource.class);
    KoutaAsyncResource koutaAsyncResource = Mockito.mock(KoutaAsyncResource.class);
    OrganisaatioAsyncResource organisaatioAsyncResource = Mockito.mock(OrganisaatioAsyncResource.class);
    KoodistoCachedAsyncResource koodistoCachedAsyncResource = Mockito.mock(KoodistoCachedAsyncResource.class);
    try {
      HakukohdeValintaperusteetV1RDTO valintaperusteet = new GsonBuilder()
          .registerTypeAdapter(Date.class,
              (JsonDeserializer<Date>) (json, typeOfT,
                  context) -> new Date(json.getAsJsonPrimitive().getAsLong()))
          .create()
          .fromJson(IOUtils
              .toString(new ClassPathResource("hakukohdeimport/data2/1.2.246.562.20.27059719875.json")
                  .getInputStream()),
              HakukohdeValintaperusteetV1RDTO.class);
      when(tarjontaAsyncResource.findValintaperusteetByOid(Mockito.anyString()))
          .thenAnswer((Answer<CompletableFuture<HakukohdeValintaperusteetV1RDTO>>) invocationOnMock -> {
            if (invocationOnMock.getArgument(0).equals(VIALLINEN_HAKUKOHDE)) {
              return CompletableFuture.completedFuture(new HakukohdeValintaperusteetV1RDTO());
            } else {
              return CompletableFuture.completedFuture(valintaperusteet);
            }
          });
    } catch (IOException ignored) {
    }

    SuoritaHakukohdeImportKomponentti tarjontaJaKoodistoHakukohteenHakuKomponentti = new SuoritaHakukohdeImportKomponentti(
        tarjontaAsyncResource, koutaAsyncResource, organisaatioAsyncResource, koodistoCachedAsyncResource);

    return new HakuImportRouteImpl(1, 1, suoritaHakuImportKomponentti, valintaperusteetRestResource,
        tarjontaJaKoodistoHakukohteenHakuKomponentti);
  }
}
