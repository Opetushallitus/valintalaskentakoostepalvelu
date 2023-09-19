package fi.vm.sade.valinta.kooste.dokumentit;

import static fi.vm.sade.valinta.kooste.AuthorizationUtil.getCurrentUser;

import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectEntity;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@RestController("DokumenttienLatausResource")
@RequestMapping("/resources/dokumentit")
@PreAuthorize("isAuthenticated()")
@Tag(name = "/dokumentit", description = "Dokumenttien lataus")
public class DokumenttienLatausResource {
  private static final Logger LOG = LoggerFactory.getLogger(DokumenttienLatausResource.class);
  private final Dokumenttipalvelu dokumenttipalvelu;

  @Autowired
  public DokumenttienLatausResource(final Dokumenttipalvelu dokumenttipalvelu) {
    this.dokumenttipalvelu = dokumenttipalvelu;
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
  @GetMapping(value = "/osoitetarrat/{hakukohdeOid}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Lataa osoitetarrojen metatiedot hakukohteelle")
  public Collection<ObjectMetadata> osoitetarrat(
      @PathVariable("hakukohdeOid") final String hakukohdeOid) {
    try {
      LOG.info("{} haki osoitetarrat hakukohteelle {}", getCurrentUser(), hakukohdeOid);
      return dokumenttipalvelu.find(Set.of("osoitetarrat", "haulle", hakukohdeOid)).stream()
          .sorted(
              (ObjectMetadata o1, ObjectMetadata o2) -> o2.lastModified.compareTo(o1.lastModified))
          .collect(Collectors.toList());
    } catch (final Exception e) {
      LOG.warn("Virhe haettaessa osoitetarradokumentteja", e);
      return Collections.emptyList();
    }
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
  @GetMapping(
      value = "/hyvaksymiskirjeet/{hakukohdeOid}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Lataa hyväksymiskirjeiden metatiedot hakukohteelle")
  public Collection<ObjectMetadata> hyvaksymiskirjeet(
      @PathVariable("hakukohdeOid") final String hakukohdeOid) {
    try {
      LOG.info("{} haki hyväksymiskirjeet hakukohteelle {}", getCurrentUser(), hakukohdeOid);
      return dokumenttipalvelu.find(Set.of("viestintapalvelu")).stream()
          .filter(
              object -> {
                // FIXME: Tämä saattaa olla raskas operaatio, riippuen tallessa olevien
                // hyväksymiskirjeiden määrästä
                final ObjectEntity entity = dokumenttipalvelu.get(object.key);
                return entity.fileName.equals("hyvaksymiskirje_" + hakukohdeOid + ".pdf");
              })
          .sorted(
              (ObjectMetadata o1, ObjectMetadata o2) -> o2.lastModified.compareTo(o1.lastModified))
          .collect(Collectors.toList());
    } catch (final Exception e) {
      LOG.warn("Virhe haettaessa hyväksymiskirjedokumentteja", e);
      return Collections.emptyList();
    }
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_SIJOITTELU_READ','ROLE_APP_SIJOITTELU_READ_UPDATE','ROLE_APP_SIJOITTELU_CRUD')")
  @GetMapping(
      value = "/sijoitteluntulokset/{hakukohdeOid}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Lataa sijoittelun tulosten metatiedot hakukohteelle")
  public Collection<ObjectMetadata> sijoitteluntulokset(
      @PathVariable("hakukohdeOid") final String hakukohdeOid) {
    try {
      LOG.info("{} haki sijoitteluntulokset hakukohteelle {}", getCurrentUser(), hakukohdeOid);
      return dokumenttipalvelu.find(Set.of("taulukkolaskennat", "haulle", hakukohdeOid)).stream()
          .sorted(
              (ObjectMetadata o1, ObjectMetadata o2) -> o2.lastModified.compareTo(o1.lastModified))
          .collect(Collectors.toList());
    } catch (final Exception e) {
      LOG.warn("Virhe haettaessa sijoitteluntulosdokumentteja", e);
      return Collections.emptyList();
    }
  }

  @GetMapping(value = "/lataa/{documentId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @Operation(summary = "Lataa dokumentin")
  public ResponseEntity<byte[]> lataa(
      @PathVariable("documentId") final String documentId, final HttpServletResponse response) {
    try {
      final Collection<ObjectMetadata> objectMetadata =
          dokumenttipalvelu.find(Collections.singleton(documentId));
      if (objectMetadata.size() == 1) {
        final ObjectMetadata metadata = objectMetadata.stream().findFirst().get();
        final ObjectEntity objectEntity = dokumenttipalvelu.get(metadata.key);
        response.setHeader("Content-Type", objectEntity.contentType);
        response.setHeader(
            "Content-Disposition", "attachment; filename=\"" + objectEntity.fileName + "\"");
        response.setHeader("Content-Length", String.valueOf(objectEntity.contentLength));
        response.setHeader("Cache-Control", "private");
        LOG.info("{} haki dokumentin {}", getCurrentUser(), documentId);
        return ResponseEntity.ok(IOUtils.toByteArray(objectEntity.entity));
      } else {
        LOG.info("DocumentId:llä {} löytyi {} osumaa", documentId, objectMetadata.size());
        return ResponseEntity.notFound().build();
      }
    } catch (final Exception e) {
      LOG.warn("Virhe ladattaessa tiedostoa {}", documentId, e);
      if (e.getCause() != null && e.getCause() instanceof NoSuchKeyException) {
        return ResponseEntity.notFound().build();
      } else {
        return ResponseEntity.internalServerError().build();
      }
    }
  }
}
