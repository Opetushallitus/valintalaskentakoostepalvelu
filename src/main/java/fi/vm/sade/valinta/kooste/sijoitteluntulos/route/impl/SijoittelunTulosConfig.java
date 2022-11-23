package fi.vm.sade.valinta.kooste.sijoitteluntulos.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohteetTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.SijoittelunTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class SijoittelunTulosConfig {

  @Bean
  public SijoittelunTulosRouteImpl getSijoittelunTulosTaulukkolaskentaRoute(
    @Value(
      "${valintalaskentakoostepalvelu.sijoittelunTulosRouteImpl.pakkaaTiedostotTarriin:false}")
      boolean pakkaaTiedostotTarriin,
    @Value("${valintalaskentakoostepalvelu.dokumenttipalvelu.rest.url}/dokumentit/lataa/")
      String dokumenttipalveluUrl,
    KoodistoCachedAsyncResource koodistoCachedAsyncResource,
    HaeHakukohteetTarjonnaltaKomponentti hakukohteetTarjonnalta,
    SijoittelunTulosExcelKomponentti sijoittelunTulosExcel,
    TarjontaAsyncResource tarjontaAsyncResource,
    OrganisaatioAsyncResource organisaatioAsyncResource,
    ViestintapalveluResource viestintapalveluResource,
    ApplicationResource applicationResource,
    AtaruAsyncResource ataruAsyncResource,
    DokumenttiAsyncResource dokumenttiAsyncResource,
    ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
    ValintalaskentaAsyncResource valintalaskentaResource)
      throws Exception {
    return
      new SijoittelunTulosRouteImpl(pakkaaTiedostotTarriin, dokumenttipalveluUrl,
        koodistoCachedAsyncResource, hakukohteetTarjonnalta, sijoittelunTulosExcel, tarjontaAsyncResource,
        organisaatioAsyncResource, viestintapalveluResource, applicationResource, ataruAsyncResource,
        dokumenttiAsyncResource, valintaTulosServiceAsyncResource, valintalaskentaResource);
  }
}
