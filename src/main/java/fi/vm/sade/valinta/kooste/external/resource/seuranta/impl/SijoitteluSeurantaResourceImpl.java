package fi.vm.sade.valinta.kooste.external.resource.seuranta.impl;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.SijoitteluSeurantaResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.seuranta.sijoittelu.dto.SijoitteluDto;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

// TODO: clean up implementation
@Service
public class SijoitteluSeurantaResourceImpl implements SijoitteluSeurantaResource {

  private final RestCasClient restCasClient;

  private final UrlConfiguration urlConfiguration;

  @Autowired
  public SijoitteluSeurantaResourceImpl(
      @Qualifier("SeurantaCasClient") RestCasClient restCasClient) {
    this.restCasClient = restCasClient;
    this.urlConfiguration = UrlConfiguration.getInstance();
  }
  ;

  @Override
  public SijoitteluDto hae(String hakuOid) {
    try {
      return this.restCasClient
          .get(
              this.urlConfiguration.url("valintalaskentakoostepalvelu.seuranta.rest.url")
                  + "/sijoittelunseuranta/hae/"
                  + hakuOid,
              new TypeToken<SijoitteluDto>() {},
              Collections.emptyMap(),
              10 * 60 * 1000)
          .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Collection<SijoitteluDto> hae() {
    try {
      return this.restCasClient
          .get(
              this.urlConfiguration.url("valintalaskentakoostepalvelu.seuranta.rest.url")
                  + "/sijoittelunseuranta/hae",
              new TypeToken<Collection<SijoitteluDto>>() {},
              Collections.emptyMap(),
              10 * 60 * 1000)
          .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SijoitteluDto merkkaaSijoittelunAjossaTila(String hakuOid, boolean tila) {
    try {
      return this.restCasClient
          .put(
              this.urlConfiguration.url("valintalaskentakoostepalvelu.seuranta.rest.url")
                  + "/sijoittelunseuranta/sijoittelu/"
                  + hakuOid
                  + "/ajossa/"
                  + tila,
              new TypeToken<SijoitteluDto>() {},
              Optional.empty(),
              Collections.emptyMap(),
              10 * 60 * 1000)
          .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SijoitteluDto merkkaaSijoittelunAjetuksi(String hakuOid) {
    try {
      return this.restCasClient
          .put(
              this.urlConfiguration.url("valintalaskentakoostepalvelu.seuranta.rest.url")
                  + "/sijoittelunseuranta/sijoittelu/"
                  + hakuOid,
              new TypeToken<SijoitteluDto>() {},
              Optional.empty(),
              Collections.emptyMap(),
              10 * 60 * 1000)
          .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void poistaSijoittelu(String hakuOid) {
    try {
      this.restCasClient
          .delete(
              this.urlConfiguration.url("valintalaskentakoostepalvelu.seuranta.rest.url")
                  + "/sijoittelunseuranta/sijoittelu/"
                  + hakuOid,
              Collections.emptyMap(),
              10 * 60 * 1000)
          .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void paivitaSijoittelunAloitusajankohta(
      String hakuOid, long aloitusajankohta, int ajotiheys) {
    try {
      this.restCasClient
          .put(
              this.urlConfiguration.url("valintalaskentakoostepalvelu.seuranta.rest.url")
                  + "/sijoittelunseuranta/sijoittelu/"
                  + hakuOid
                  + "/paivita"
                  + "?aloitusajankohta="
                  + aloitusajankohta
                  + "&ajotiheys="
                  + ajotiheys,
              new TypeToken<String>() {},
              Optional.empty(),
              Collections.emptyMap(),
              10 * 60 * 1000)
          .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
