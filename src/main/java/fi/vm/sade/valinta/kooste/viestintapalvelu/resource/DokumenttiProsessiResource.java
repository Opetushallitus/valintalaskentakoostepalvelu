package fi.vm.sade.valinta.kooste.viestintapalvelu.resource;

import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
    name = "/dokumenttiprosessi",
    description = "Dokumenttien luontiin liittyvää palautetta käyttäjälle")
public class DokumenttiProsessiResource {

  @Autowired private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Palauttaa dokumenttiprosessin id:lle jos sellainen on muistissa",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = DokumenttiProsessi.class)))
      })
  public DokumenttiProsessi hae(@PathVariable("id") String id) {
    return dokumenttiProsessiKomponentti.haeProsessi(id);
  }

  @PostMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Ilmoittaa poikkeuksen prosessiin",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = Void.class)))
      })
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
