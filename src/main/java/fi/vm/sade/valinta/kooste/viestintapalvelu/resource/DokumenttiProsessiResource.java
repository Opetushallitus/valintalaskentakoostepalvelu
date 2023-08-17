package fi.vm.sade.valinta.kooste.viestintapalvelu.resource;

import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Palauttaa prosessoidut dokumentit resursseina */
@RestController("DokumenttiProsessiResource")
@RequestMapping("/resources/dokumenttiprosessi")
@PreAuthorize("isAuthenticated()")
@Api(
    value = "/dokumenttiprosessi",
    description = "Dokumenttien luontiin liittyvää palautetta käyttäjälle")
public class DokumenttiProsessiResource {

  @Autowired private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ApiOperation(
      value = "Palauttaa dokumenttiprosessin id:lle jos sellainen on muistissa",
      response = DokumenttiProsessi.class)
  public DokumenttiProsessi hae(@PathVariable("id") String id) {
    return dokumenttiProsessiKomponentti.haeProsessi(id);
  }

  @PostMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Ilmoittaa poikkeuksen prosessiin", response = ResponseEntity.class)
  public ResponseEntity poikkeus(
      @PathVariable("id") String id,
      @RequestParam(value = "poikkeus", required = false) String poikkeus) {
    DokumenttiProsessi d = dokumenttiProsessiKomponentti.haeProsessi(id);
    if (d != null) {
      d.getPoikkeukset()
          .add(new Poikkeus("Dokumenttiprosessiresurssi", "Poikkeuksen ilmoitus", poikkeus));
    }
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
