package fi.vm.sade.valinta.kooste.parametrit.resource;

import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.ParametritParser;
import fi.vm.sade.valinta.kooste.parametrit.dto.ParametritUIDTO;
import fi.vm.sade.valinta.kooste.parametrit.service.HakuParametritService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController("ParametritResource")
@RequestMapping("/parametrit")
@PreAuthorize("isAuthenticated()")
@Api(value = "/parametrit", description = "Ohjausparametrit palveluiden aktiviteettipäivämäärille")
public class ParametritResource {

  private static final Logger LOG = LoggerFactory.getLogger(ParametritResource.class);

  @Autowired private HakuParametritService hakuParametritService;
  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;

  @GetMapping(value = "/{hakuOid}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Parametrien listaus", response = ParametritUIDTO.class)
  public ParametritUIDTO listParametrit(@PathVariable("hakuOid") String hakuOid) {

    ParametritUIDTO resp = new ParametritUIDTO();
    ParametritParser parser = hakuParametritService.getParametritForHaku(hakuOid);
    resp.pistesyotto = parser.pistesyottoEnabled();
    resp.hakeneet = parser.hakeneetEnabled();
    resp.harkinnanvaraiset = parser.harkinnanvaraisetEnabled();
    resp.valintakoekutsut = parser.valintakoekutsutEnabled();
    resp.valintalaskenta = parser.valintalaskentaEnabled();
    resp.valinnanhallinta = parser.valinnanhallintaEnabled();
    resp.hakijaryhmat = parser.hakijaryhmatEnabled();
    resp.koetulostentallennus = parser.koetulostenTallentaminenEnabled();
    resp.koekutsujenmuodostaminen = parser.koekutsujenMuodostaminenEnabled();
    resp.harkinnanvarainenpaatostallennus = parser.harkinnanvarainenPaatosTallennusEnabled();

    return resp;
  }

  @GetMapping(
      value = "/hakukohderyhmat/{hakukohdeOid}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Hakukohteen hakukohderyhmät", response = List.class)
  public ResponseEntity<List<String>> listHakukohderyhmat(
      @PathVariable("hakukohdeOid") String hakukohdeOid) {

    CompletableFuture<List<String>> resultF =
        tarjontaAsyncResource.hakukohdeRyhmasForHakukohde(hakukohdeOid);
    try {
      List<String> result = resultF.get(1, TimeUnit.MINUTES);
      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (Exception e) {
      LOG.error("Jotain meni vikaan hakukohderyhmien haussa", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
