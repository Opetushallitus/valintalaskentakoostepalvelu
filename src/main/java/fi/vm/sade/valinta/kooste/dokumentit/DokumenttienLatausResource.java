package fi.vm.sade.valinta.kooste.dokumentit;

import static fi.vm.sade.valinta.kooste.AuthorizationUtil.getCurrentUser;

import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectEntity;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectHead;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
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
      return dokumenttipalvelu.find(Set.of("osoitetarrat", hakukohdeOid)).stream()
          .sorted(
              (ObjectMetadata o1, ObjectMetadata o2) -> o2.lastModified.compareTo(o1.lastModified))
          .collect(Collectors.toList());
    } catch (final Exception e) {
      LOG.warn("Virhe haettaessa osoitetarradokumentteja hakukohteelle {}", hakukohdeOid, e);
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
      return dokumenttipalvelu.find(Set.of("viestintapalvelu", hakukohdeOid)).stream()
          .filter(
              object -> {
                // Tämä saattaa olla hidas operaatio, riippuen "voimassa olevien"
                // hyväksymiskirjeiden määrästä per
                // hakukohde
                final ObjectHead entity = dokumenttipalvelu.head(object.key);
                return entity.fileName.equals("hyvaksymiskirje_" + hakukohdeOid + ".pdf");
              })
          .sorted(
              (ObjectMetadata o1, ObjectMetadata o2) -> o2.lastModified.compareTo(o1.lastModified))
          .collect(Collectors.toList());
    } catch (final Exception e) {
      LOG.warn("Virhe haettaessa hyväksymiskirjedokumentteja hakukohteelle {}", hakukohdeOid, e);
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
      return dokumenttipalvelu.find(Set.of("taulukkolaskennat", hakukohdeOid)).stream()
          .sorted(
              (ObjectMetadata o1, ObjectMetadata o2) -> o2.lastModified.compareTo(o1.lastModified))
          .collect(Collectors.toList());
    } catch (final Exception e) {
      LOG.warn("Virhe haettaessa sijoitteluntulosdokumentteja hakukohteelle {}", hakukohdeOid, e);
      return Collections.emptyList();
    }
  }

  @GetMapping(value = "/lataa/{documentId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @Operation(
      summary = "Lataa dokumentin",
      responses = {
        @ApiResponse(
            description = "Dokumentin sisältö",
            responseCode = "200",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
        @ApiResponse(description = "Dokumenttia ei löytynyt", responseCode = "404"),
        @ApiResponse(
            description = "DocumentId:llä löytyi useampia dokumentteja",
            responseCode = "409",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiVirhe.class))),
        @ApiResponse(
            description = "Odottamaton virhetilanne dokumenttia ladattaessa",
            responseCode = "500")
      })
  public ResponseEntity<Object> lataa(
      @PathVariable("documentId") final String documentId, final HttpServletResponse response) {
    try {
      final Collection<ObjectMetadata> objectMetadata =
          dokumenttipalvelu.find(Collections.singleton(documentId));
      if (objectMetadata.isEmpty()) {
        LOG.info("DocumentId:llä {} ei löytynyt dokumentteja", documentId);
        return ResponseEntity.notFound().build();
      } else if (objectMetadata.size() > 1) {
        final List<String> keys =
            objectMetadata.stream().map(o -> o.key).collect(Collectors.toList());
        LOG.info(
            "DocumentId:llä {} löytyi {} dokumenttia: avaimet {}",
            documentId,
            objectMetadata.size(),
            keys);
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(
                new ApiVirhe(String.format("löytyi %d dokumenttia", objectMetadata.size()), keys));
      }
      final ObjectMetadata metadata = objectMetadata.stream().findFirst().get();
      final ObjectEntity objectEntity = dokumenttipalvelu.get(metadata.key);
      response.setHeader("Content-Type", objectEntity.contentType);
      response.setHeader(
          "Content-Disposition", "attachment; filename=\"" + objectEntity.fileName + "\"");
      response.setHeader("Content-Length", String.valueOf(objectEntity.contentLength));
      response.setHeader("Cache-Control", "private");
      LOG.info("{} haki dokumentin {}", getCurrentUser(), documentId);
      return ResponseEntity.ok(IOUtils.toByteArray(objectEntity.entity));
    } catch (final Exception e) {
      LOG.warn("Virhe ladattaessa dokumenttia {}", documentId, e);
      if (e.getCause() != null && e.getCause() instanceof NoSuchKeyException) {
        return ResponseEntity.notFound().build();
      } else {
        return ResponseEntity.internalServerError().build();
      }
    }
  }
}
