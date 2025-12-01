package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.ValintatulosUpdateStatus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Valinnantulos;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice.VastaanottoAikarajaMennytDTO;
import fi.vm.sade.valinta.kooste.testapp.MockServicesApp;
import io.reactivex.Observable;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

public class ValintaTulosServiceAsyncResourceImplTest {

  private static final String PERSON_OID = "1.1.1.1";
  private static final String USER_AGENT = "user-agent";
  private static final String INET_ADDRESS = "127.0.0.1";
  private static final String HAKEMUS_OID = "hakemus-oid";
  private static final String JONO_OID = "jono-oid";
  public static final String HAKUKOHDE_OID = "hakukohde-oid";

  private final HttpClient httpClient = Mockito.mock(HttpClient.class);
  private final RestCasClient casClient = Mockito.mock(RestCasClient.class);
  private final ValintaTulosServiceAsyncResource service =
      new ValintaTulosServiceAsyncResourceImpl(httpClient, casClient);

  @BeforeEach
  void setUp() {
    Mockito.reset(httpClient, casClient);
    MockServicesApp.start();
  }

  @Nested
  @ExtendWith(OutputCaptureExtension.class)
  class GetErillishaunValinnantulokset {
    @Test
    public void writesToAuditLog(CapturedOutput output) {
      List<String> roles = List.of("role1", "role2", "role3");
      AuditSession auditSession = new AuditSession(PERSON_OID, roles, USER_AGENT, INET_ADDRESS);
      auditSession.setIfUnmodifiedSince(
          Optional.of(OffsetDateTime.now().format(RFC_1123_DATE_TIME)));
      Valinnantulos valinnantulos = new Valinnantulos();
      valinnantulos.setHenkiloOid(PERSON_OID);
      valinnantulos.setHakemusOid(HAKEMUS_OID);
      valinnantulos.setHakukohdeOid(HAKUKOHDE_OID);
      valinnantulos.setValintatapajonoOid(JONO_OID);
      when(casClient.get(any(), any(), any(), anyInt()))
          .thenReturn(CompletableFuture.completedFuture(List.of(valinnantulos)));

      Observable<List<Valinnantulos>> response =
          service.getErillishaunValinnantulokset(auditSession, JONO_OID);

      assertThat(response.blockingSingle()).singleElement().isEqualTo(valinnantulos);
      List<String> auditLines =
          output
              .getOut()
              .lines()
              .filter(line -> line.contains("ERILLISHAUN_VALINTATULOSTEN_TUONTI"))
              .flatMap(line -> Arrays.stream(line.split(" ")))
              .dropWhile(s -> !s.startsWith("{"))
              .toList();
      assertThat(auditLines).hasSize(1);
      JsonObject json = JsonParser.parseString(auditLines.get(0)).getAsJsonObject();
      assertThat(json.get("serviceName").getAsString()).isEqualTo("valintalaskentakoostepalvelu");
      assertThat(json.get("operation").getAsString())
          .isEqualTo("ERILLISHAUN_VALINTATULOSTEN_TUONTI");
      JsonObject user = json.getAsJsonObject("user");
      assertThat(user.get("oid").getAsString()).isEqualTo(PERSON_OID);
      assertThat(user.get("ip").getAsString()).isEqualTo(INET_ADDRESS);
      assertThat(user.get("userAgent").getAsString()).isEqualTo(USER_AGENT);
      JsonObject target = json.getAsJsonObject("target");
      assertThat(target.get("type").getAsString()).isEqualTo("ERILLISHAUN_VALINTATULOS");
      assertThat(target.get("valintatapajonoOid").getAsString()).isEqualTo(JONO_OID);
      assertThat(json.get("changes").getAsJsonArray()).isEmpty();
      verify(casClient)
          .get(
              contains("/valinta-tulos-service/erillishaku/valinnan-tulos/jono-oid"),
              any(),
              any(),
              eq(60 * 1000));
      verifyNoMoreInteractions(casClient);
    }
  }

  @Nested
  @ExtendWith(OutputCaptureExtension.class)
  class PostErillishaunValinnantulokset {
    @Test
    public void writesToAuditLog(CapturedOutput output) {
      List<String> roles = List.of("role1", "role2", "role3");
      AuditSession auditSession = new AuditSession(PERSON_OID, roles, USER_AGENT, INET_ADDRESS);
      auditSession.setIfUnmodifiedSince(
          Optional.of(OffsetDateTime.now().format(RFC_1123_DATE_TIME)));
      Valinnantulos valinnantulos = new Valinnantulos();
      valinnantulos.setHenkiloOid(PERSON_OID);
      valinnantulos.setHakemusOid(HAKEMUS_OID);
      valinnantulos.setHakukohdeOid(HAKUKOHDE_OID);
      valinnantulos.setValintatapajonoOid(JONO_OID);
      when(casClient.post(any(), any(), any(), any(), anyInt()))
          .thenReturn(CompletableFuture.completedFuture(List.of()));

      Observable<List<ValintatulosUpdateStatus>> response =
          service.postErillishaunValinnantulokset(auditSession, JONO_OID, List.of(valinnantulos));

      assertThat(response.blockingSingle()).isEmpty();
      List<String> auditLines =
          output
              .getOut()
              .lines()
              .filter(line -> line.contains("ERILLISHAUN_VALINTATULOSTEN_VIENTI"))
              .flatMap(line -> Arrays.stream(line.split(" ")))
              .dropWhile(s -> !s.startsWith("{"))
              .toList();
      assertThat(auditLines).hasSize(1);
      JsonObject json = JsonParser.parseString(auditLines.get(0)).getAsJsonObject();
      assertThat(json.get("serviceName").getAsString()).isEqualTo("valintalaskentakoostepalvelu");
      assertThat(json.get("operation").getAsString())
          .isEqualTo("ERILLISHAUN_VALINTATULOSTEN_VIENTI");
      JsonObject user = json.getAsJsonObject("user");
      assertThat(user.get("oid").getAsString()).isEqualTo(PERSON_OID);
      assertThat(user.get("ip").getAsString()).isEqualTo(INET_ADDRESS);
      assertThat(user.get("userAgent").getAsString()).isEqualTo(USER_AGENT);
      JsonObject target = json.getAsJsonObject("target");
      assertThat(target.get("type").getAsString()).isEqualTo("ERILLISHAUN_VALINTATULOS");
      assertThat(target.get("henkiloOid").getAsString()).isEqualTo(PERSON_OID);
      assertThat(target.get("hakemusOid").getAsString()).isEqualTo(HAKEMUS_OID);
      assertThat(target.get("valintatapajonoOid").getAsString()).isEqualTo(JONO_OID);
      assertThat(target.get("hakukohdeOid").getAsString()).isEqualTo(HAKUKOHDE_OID);
      assertThat(json.get("changes").getAsJsonArray()).isEmpty();
      verify(casClient)
          .post(
              contains("/valinta-tulos-service/erillishaku/valinnan-tulos/jono-oid"),
              any(),
              any(),
              any(),
              eq(30 * 60 * 1000));
      verifyNoMoreInteractions(casClient);
    }
  }

  @Test
  public void vastaanottoAikarajaMennytDTOsCanBeParsed() {
    String hakemusOid = "1.2.246.562.11.00004697189";
    String vastaanottoDeadline = "2016-07-15T12:00:00Z";
    VastaanottoAikarajaMennytDTO parsedDto =
        ValintaTulosServiceAsyncResourceImpl.getGson()
            .fromJson(
                " {\n"
                    + "        \"hakemusOid\": \""
                    + hakemusOid
                    + "\",\n"
                    + "        \"mennyt\": true,\n"
                    + "        \"vastaanottoDeadline\": \""
                    + vastaanottoDeadline
                    + "\"\n"
                    + "    }",
                VastaanottoAikarajaMennytDTO.class);
    assertEquals(hakemusOid, parsedDto.getHakemusOid());
    assertEquals(
        new DateTime(2016, 7, 15, 12, 0, 0, DateTimeZone.UTC), parsedDto.getVastaanottoDeadline());
    assertEquals(true, parsedDto.isMennyt());
  }
}
