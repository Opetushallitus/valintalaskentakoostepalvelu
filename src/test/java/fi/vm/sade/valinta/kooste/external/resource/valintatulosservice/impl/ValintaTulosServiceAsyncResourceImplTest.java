package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
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
import java.util.Map;
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
  private static final String HAKUKOHDE_OID = "hakukohde-oid";

  private final HttpClient httpClient = Mockito.mock(HttpClient.class);
  private final RestCasClient casClient = Mockito.mock(RestCasClient.class);
  private final ValintaTulosServiceAsyncResource service =
      new ValintaTulosServiceAsyncResourceImpl(httpClient, casClient);

  private final Gson gson = ValintaTulosServiceAsyncResourceImpl.getGson();

  @BeforeEach
  void setUp() {
    Mockito.reset(httpClient, casClient);
    MockServicesApp.start();
  }

  private record AuditUser(String oid, String ip, String userAgent) {
    public static AuditUser expected() {
      return new AuditUser(PERSON_OID, INET_ADDRESS, USER_AGENT);
    }
  }

  private record AuditLogRow(
      String serviceName,
      String operation,
      AuditUser user,
      Map<String, String> target,
      List<String> changes) {
    public static AuditLogRow expected(String operation, Map<String, String> target) {
      return new AuditLogRow(
          "valintalaskentakoostepalvelu", operation, AuditUser.expected(), target, List.of());
    }
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
      AuditLogRow row = gson.fromJson(auditLines.get(0), AuditLogRow.class);
      AuditLogRow expectedRow =
          AuditLogRow.expected(
              "ERILLISHAUN_VALINTATULOSTEN_TUONTI",
              Map.of("valintatapajonoOid", JONO_OID, "type", "ERILLISHAUN_VALINTATULOS"));
      assertThat(row).isEqualTo(expectedRow);
      String url =
          "^http://127.0.0.1:\\d+/valinta-tulos-service/erillishaku/valinnan-tulos/jono-oid\\?hyvaksymiskirjeet=true$";
      verify(casClient).get(matches(url), any(), any(), eq(60 * 1000));
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
      AuditLogRow row = gson.fromJson(auditLines.get(0), AuditLogRow.class);
      AuditLogRow expectedRow =
          AuditLogRow.expected(
              "ERILLISHAUN_VALINTATULOSTEN_VIENTI",
              Map.of(
                  "valintatapajonoOid", JONO_OID,
                  "type", "ERILLISHAUN_VALINTATULOS",
                  "henkiloOid", PERSON_OID,
                  "hakemusOid", HAKEMUS_OID,
                  "hakukohdeOid", HAKUKOHDE_OID));
      assertThat(row).isEqualTo(expectedRow);
      String url =
          "^http://127.0.0.1:\\d+/valinta-tulos-service/erillishaku/valinnan-tulos/jono-oid$";
      verify(casClient).post(matches(url), any(), any(), any(), eq(30 * 60 * 1000));
      verifyNoMoreInteractions(casClient);
    }
  }

  @Test
  public void vastaanottoAikarajaMennytDTOsCanBeParsed() {
    String hakemusOid = "1.2.246.562.11.00004697189";
    String vastaanottoDeadline = "2016-07-15T12:00:00Z";
    VastaanottoAikarajaMennytDTO parsedDto =
        gson.fromJson(
            """
               {
                 "hakemusOid": "%s",
                 "mennyt": true,
                 "vastaanottoDeadline": "%s"
               }"""
                .formatted(hakemusOid, vastaanottoDeadline),
            VastaanottoAikarajaMennytDTO.class);
    assertEquals(hakemusOid, parsedDto.getHakemusOid());
    assertEquals(
        new DateTime(2016, 7, 15, 12, 0, 0, DateTimeZone.UTC), parsedDto.getVastaanottoDeadline());
    assertTrue(parsedDto.isMennyt());
  }
}
