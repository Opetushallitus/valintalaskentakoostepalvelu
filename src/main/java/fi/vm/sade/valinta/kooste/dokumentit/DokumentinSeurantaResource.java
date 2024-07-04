package fi.vm.sade.valinta.kooste.dokumentit;

import static org.apache.commons.lang.StringUtils.trimToNull;

import fi.vm.sade.valinta.kooste.dokumentit.dao.DokumenttiRepository;
import fi.vm.sade.valinta.kooste.dokumentit.dto.DokumenttiDto;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping(value = "/resources/dokumentinseuranta")
public class DokumentinSeurantaResource {
  private static final Logger LOG = LoggerFactory.getLogger(DokumentinSeurantaResource.class);

  @Autowired private DokumenttiRepository dokumenttiRepository;

  @GetMapping(value = "/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<DokumenttiDto> dokumentti(@PathVariable("key") String key) {
    try {
      if (trimToNull(key) == null) {
        LOG.error("key({}) ei saa olla tyhjä!", key);
        throw new RuntimeException("Key ei saa olla tyhjä!");
      }
      return ResponseEntity.ok(dokumenttiRepository.hae(UUID.fromString(key)));
    } catch (Throwable t) {
      LOG.error("Poikkeus dokumentinseurannassa dokumenttia luettauessa key=" + key, t);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
