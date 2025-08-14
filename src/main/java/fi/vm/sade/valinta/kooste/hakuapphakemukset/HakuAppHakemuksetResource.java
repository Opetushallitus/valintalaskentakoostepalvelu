package fi.vm.sade.valinta.kooste.hakuapphakemukset;

import static fi.vm.sade.valinta.kooste.AuthorizationUtil.createAuditSession;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.pistesyotto.service.HakukohdeOIDAuthorityCheck;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import fi.vm.sade.valinta.sharedutils.ValintaResource;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController("HakuAppHakemuksetResource")
@RequestMapping("/resources/hakuAppHakemukset")
@PreAuthorize("isAuthenticated()")
@Tag(
    name = "/hakuAppHakemukset",
    description = "Proxy-rajapinnat vanhan haku-ppin hakemusten hakemiseen")
public class HakuAppHakemuksetResource {

  private static final Logger LOG = LoggerFactory.getLogger(HakuAppHakemuksetResource.class);

  @Autowired private AuthorityCheckService authorityCheckService;

  private final RestCasClient hakuAppClient;
  private final UrlConfiguration urlConfiguration;

  private final List<String> roles =
      List.of(
          "ROLE_APP_HAKEMUS_READ_UPDATE",
          "ROLE_APP_HAKEMUS_READ",
          "ROLE_APP_HAKEMUS_CRUD",
          "ROLE_APP_HAKEMUS_LISATIETORU",
          "ROLE_APP_HAKEMUS_LISATIETOCRUD");

  @Autowired
  public HakuAppHakemuksetResource(@Qualifier("HakuAppCasClient") RestCasClient hakuAppClient) {
    this.hakuAppClient = hakuAppClient;
    this.urlConfiguration = UrlConfiguration.getInstance();
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Palauttaa hakuehtoihin sopivien hakemusten perustiedot.")
  public CompletableFuture<Map<String, Object>> findApplicationsGet(
      @RequestParam(name = "q", required = false) String searchTerms,
      @RequestParam(name = "appState", required = false) List<String> state,
      @RequestParam(name = "paymentState", required = false) String paymentState,
      @RequestParam(name = "preferenceChecked", required = false) Boolean preferenceChecked,
      @RequestParam(name = "aoid", required = false) String aoid,
      @RequestParam(name = "aoOids", required = false) List<String> aoOids,
      @RequestParam(name = "groupOid", required = false) String groupOid,
      @RequestParam(name = "baseEducation", required = false) Set<String> baseEducation,
      @RequestParam(name = "lopoid", required = false) String lopoid,
      @RequestParam(name = "asId", required = false) String asId,
      @RequestParam(name = "organizationFilter", required = false) String organizationFilter,
      @RequestParam(name = "asSemester", required = false) String asSemester,
      @RequestParam(name = "asYear", required = false) String asYear,
      @RequestParam(name = "aoOid", required = false) String aoOid,
      @RequestParam(name = "discretionaryOnly", required = false) Boolean discretionaryOnly,
      @RequestParam(name = "primaryPreferenceOnly", required = false) Boolean primaryPreferenceOnly,
      @RequestParam(name = "sendingSchoolOid", required = false) String sendingSchoolOid,
      @RequestParam(name = "sendingClass", required = false) String sendingClass,
      @RequestParam(name = "updatedAfter", required = false) String updatedAfter,
      @RequestParam(name = "start", required = false) String start,
      @RequestParam(name = "rows", required = false) String rows,
      @RequestParam(name = "sortResults", required = false) String sortResults,
      HttpServletRequest request) {

    AuthorityCheckService.Context context = authorityCheckService.getContext();

    String queryString =
        UriComponentsBuilder.newInstance()
            .queryParamIfPresent("q", Optional.ofNullable(searchTerms))
            .queryParamIfPresent("appState", Optional.ofNullable(state))
            .queryParamIfPresent("paymentState", Optional.ofNullable(paymentState))
            .queryParamIfPresent("preferenceChecked", Optional.ofNullable(preferenceChecked))
            .queryParamIfPresent("aoid", Optional.ofNullable(aoid))
            .queryParamIfPresent("aoOids", Optional.ofNullable(aoOids))
            .queryParamIfPresent("groupOid", Optional.ofNullable(groupOid))
            .queryParamIfPresent("baseEducation", Optional.ofNullable(baseEducation))
            .queryParamIfPresent("lopoid", Optional.ofNullable(lopoid))
            .queryParamIfPresent("asId", Optional.ofNullable(asId))
            .queryParamIfPresent("organizationFilter", Optional.ofNullable(organizationFilter))
            .queryParamIfPresent("asSemester", Optional.ofNullable(asSemester))
            .queryParamIfPresent("asYear", Optional.ofNullable(asYear))
            .queryParamIfPresent("aoOid", Optional.ofNullable(aoOid))
            .queryParamIfPresent("discretionaryOnly", Optional.ofNullable(discretionaryOnly))
            .queryParamIfPresent(
                "primaryPreferenceOnly", Optional.ofNullable(primaryPreferenceOnly))
            .queryParamIfPresent("sendingSchoolOid", Optional.ofNullable(sendingSchoolOid))
            .queryParamIfPresent("sendingClass", Optional.ofNullable(sendingClass))
            .queryParamIfPresent("updatedAfter", Optional.ofNullable(updatedAfter))
            .queryParamIfPresent("start", Optional.ofNullable(start))
            .queryParamIfPresent("rows", Optional.ofNullable(rows))
            .queryParamIfPresent("sortResults", Optional.ofNullable(sortResults))
            .build()
            .toUri()
            .getQuery();

    LOG.info("Haetaan hakemuksia haku-appista (GET). Query on {}", queryString);

    final AuditSession auditSession = createAuditSession(request);

    Map<String, String> additionalInfo = new HashMap<>();
    additionalInfo.put("Username from call params", auditSession.getPersonOid());
    additionalInfo.put("queryString", queryString);

    AuditLog.log(
        KoosteAudit.AUDIT,
        auditSession.asAuditUser(),
        ValintaperusteetOperation.HAKEMUS,
        ValintaResource.HAKEMUS,
        auditSession.getPersonOid(),
        Changes.EMPTY,
        additionalInfo);

    CompletableFuture<Map<String, Object>> response =
        this.hakuAppClient
            .get(
                this.urlConfiguration.url("haku-app.applications.listfull") + "?" + queryString,
                new TypeToken<List<Map<String, Object>>>() {},
                Collections.emptyMap(),
                60 * 60 * 1000)
            .thenApply(
                hakemusList -> {
                  List<Map<String, Object>> authorizedHakemukset =
                      filterAuthorizedHakemukset(context, hakemusList);
                  List<Map<String, Object>> hakemukset =
                      authorizedHakemukset.stream()
                          .map(
                              origHakemus -> {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> answers =
                                    (Map<String, Object>)
                                        origHakemus.getOrDefault("answers", Map.of());
                                @SuppressWarnings("unchecked")
                                Map<String, Object> henkilotiedot =
                                    (Map<String, Object>)
                                        answers.getOrDefault("henkilotiedot", Map.of());

                                Map<String, Object> hakemus = new HashMap<>();
                                hakemus.put("oid", origHakemus.getOrDefault("oid", ""));
                                hakemus.put("state", origHakemus.getOrDefault("state", ""));
                                hakemus.put("received", origHakemus.getOrDefault("received", ""));
                                hakemus.put(
                                    "firstNames", henkilotiedot.getOrDefault("Etunimet", ""));
                                hakemus.put("lastName", henkilotiedot.getOrDefault("Sukunimi", ""));
                                hakemus.put("ssn", henkilotiedot.getOrDefault("Henkilotunnus", ""));
                                hakemus.put("personOid", origHakemus.getOrDefault("personOid", ""));
                                return hakemus;
                              })
                          .toList();

                  Map<String, Object> result = new HashMap<>();
                  result.put("", Integer.valueOf(hakemusList.size()));
                  result.put("results", hakemukset);

                  return result;
                });

    return response;
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @GetMapping(
      value = "eligibilities/{hakuOid}/{hakukohdeOid}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Hakee yhden hakemuksen tiedot oidilla.")
  public CompletableFuture<List<String>> findApplicationEligibilities(
      @PathVariable final String hakuOid,
      @PathVariable final String hakukohdeOid,
      HttpServletRequest request) {

    Preconditions.checkNotNull(hakuOid);
    Preconditions.checkNotNull(hakukohdeOid);

    LOG.info(
        "Haetaan haun {} hakemuksien maksuvelvollisuudet hakukohteelle {}", hakuOid, hakukohdeOid);

    authorityCheckService.checkAuthorizationForHakukohteet(List.of(hakukohdeOid), roles);

    final AuditSession auditSession = createAuditSession(request);

    Map<String, String> additionalInfo = new HashMap<>();
    additionalInfo.put("Username from call params", auditSession.getPersonOid());
    additionalInfo.put("hakuOid", hakuOid);
    additionalInfo.put("hakukohdeOid", hakukohdeOid);

    AuditLog.log(
        KoosteAudit.AUDIT,
        auditSession.asAuditUser(),
        ValintaperusteetOperation.HAKEMUS,
        ValintaResource.HAKEMUS,
        hakukohdeOid,
        Changes.EMPTY,
        additionalInfo);

    CompletableFuture<List<String>> response =
        this.hakuAppClient.get(
            this.urlConfiguration.url("haku-app.applications.eligibilities", hakuOid, hakukohdeOid),
            new TypeToken<List<String>>() {},
            Collections.emptyMap(),
            60 * 60 * 1000);

    return response;
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @GetMapping(value = "/{hakemusOid}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Hakee yhden hakemuksen tiedot oidilla.")
  public CompletableFuture<Map<String, Object>> findApplicationByOid(
      @PathVariable("hakemusOid") final String hakemusOid, HttpServletRequest request) {

    Preconditions.checkNotNull(hakemusOid);

    AuthorityCheckService.Context context = authorityCheckService.getContext();

    final AuditSession auditSession = createAuditSession(request);

    Map<String, String> additionalInfo = new HashMap<>();
    additionalInfo.put("Username from call params", auditSession.getPersonOid());
    additionalInfo.put("hakemusOid", hakemusOid);

    AuditLog.log(
        KoosteAudit.AUDIT,
        auditSession.asAuditUser(),
        ValintaperusteetOperation.HAKEMUS,
        ValintaResource.HAKEMUS,
        hakemusOid,
        Changes.EMPTY,
        additionalInfo);

    LOG.info("Haetaan yksi hakemus haku-appista. Hakemuksen oid on {}", hakemusOid);

    CompletableFuture<Map<String, Object>> response =
        this.hakuAppClient
            .get(
                this.urlConfiguration.url("haku-app.applications", hakemusOid),
                new TypeToken<Map<String, Object>>() {},
                Collections.emptyMap(),
                60 * 60 * 1000)
            .thenApply(
                hakemus -> {
                  List<String> hakukohdeOids = getHakutoiveet(hakemus);
                  authorityCheckService.withContext(
                      context,
                      () ->
                          authorityCheckService.checkAuthorizationForHakukohteet(
                              hakukohdeOids, roles));
                  return hakemus;
                });

    return response;
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Palauttaa hakuehtoihin sopivien hakemusten tiedot.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
  public CompletableFuture<List<Map<String, Object>>> findListApplicationsPost(
      @RequestParam(name = "keys", required = false) List<String> keys,
      @RequestParam(name = "state", required = false) List<String> state,
      @RequestParam(name = "asIds", required = false) List<String> asIds,
      @RequestBody final String applicationSearchData,
      HttpServletRequest request) {

    AuthorityCheckService.Context context = authorityCheckService.getContext();

    String queryString =
        UriComponentsBuilder.newInstance()
            .queryParamIfPresent("keys", Optional.ofNullable(keys))
            .queryParamIfPresent("appState", Optional.ofNullable(state))
            .queryParamIfPresent("asIds", Optional.ofNullable(asIds))
            .build()
            .toUri()
            .getQuery();

    LOG.info(
        "Haetaan hakemuksia haku-appista (listfull POST). Query on {} ja body on {}",
        queryString,
        applicationSearchData.toString());

    final AuditSession auditSession = createAuditSession(request);

    Map<String, String> additionalInfo = new HashMap<>();
    additionalInfo.put("Username from call params", auditSession.getPersonOid());
    additionalInfo.put("queryString", queryString);
    additionalInfo.put("applicationSearchData", applicationSearchData);

    AuditLog.log(
        KoosteAudit.AUDIT,
        auditSession.asAuditUser(),
        ValintaperusteetOperation.HAKEMUS,
        ValintaResource.HAKEMUS,
        auditSession.getPersonOid(),
        Changes.EMPTY,
        additionalInfo);

    CompletableFuture<List<Map<String, Object>>> response =
        this.hakuAppClient
            .postPlaintext(
                this.urlConfiguration.url("haku-app.applications.list") + "?" + queryString,
                new TypeToken<List<Map<String, Object>>>() {},
                applicationSearchData,
                Collections.emptyMap(),
                60 * 60 * 1000)
            .thenApply(hakemukset -> filterAuthorizedHakemukset(context, hakemukset));

    return response;
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @PostMapping(value = "/listfull", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Palauttaa hakuehtoihin sopivien hakemusten tiedot.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)))
  public CompletableFuture<List<Map<String, Object>>> findFullApplicationsPost(
      @RequestBody final String applicationSearchData, HttpServletRequest request) {

    AuthorityCheckService.Context context = authorityCheckService.getContext();

    LOG.info(
        "Haetaan hakemuksia haku-appista (listfull POST). Body on {}",
        applicationSearchData.toString());

    final AuditSession auditSession = createAuditSession(request);

    Map<String, String> additionalInfo = new HashMap<>();
    additionalInfo.put("Username from call params", auditSession.getPersonOid());
    additionalInfo.put("applicationSearchData", applicationSearchData);

    AuditLog.log(
        KoosteAudit.AUDIT,
        auditSession.asAuditUser(),
        ValintaperusteetOperation.HAKEMUS,
        ValintaResource.HAKEMUS,
        auditSession.getPersonOid(),
        Changes.EMPTY,
        additionalInfo);

    CompletableFuture<List<Map<String, Object>>> response =
        this.hakuAppClient
            .postPlaintext(
                this.urlConfiguration.url("haku-app.applications.listfull"),
                new TypeToken<List<Map<String, Object>>>() {},
                applicationSearchData,
                Collections.emptyMap(),
                60 * 60 * 1000)
            .thenApply(hakemukset -> filterAuthorizedHakemukset(context, hakemukset));

    return response;
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @GetMapping(value = "/listfull", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Palauttaa hakuehtoihin sopivien hakemusten tiedot.")
  public CompletableFuture<List<Map<String, Object>>> findFullApplicationsGet(
      @RequestParam(name = "q", required = false) String searchTerms,
      @RequestParam(name = "appState", required = false) List<String> state,
      @RequestParam(name = "paymentState", required = false) String paymentState,
      @RequestParam(name = "preferenceChecked", required = false) Boolean preferenceChecked,
      @RequestParam(name = "aoid", required = false) String aoid,
      @RequestParam(name = "aoOids", required = false) List<String> aoOids,
      @RequestParam(name = "groupOid", required = false) String groupOid,
      @RequestParam(name = "baseEducation", required = false) Set<String> baseEducation,
      @RequestParam(name = "lopoid", required = false) String lopoid,
      @RequestParam(name = "asId", required = false) String asId,
      @RequestParam(name = "organizationFilter", required = false) String organizationFilter,
      @RequestParam(name = "asSemester", required = false) String asSemester,
      @RequestParam(name = "asYear", required = false) String asYear,
      @RequestParam(name = "aoOid", required = false) String aoOid,
      @RequestParam(name = "discretionaryOnly", required = false) Boolean discretionaryOnly,
      @RequestParam(name = "primaryPreferenceOnly", required = false) Boolean primaryPreferenceOnly,
      @RequestParam(name = "sendingSchoolOid", required = false) String sendingSchoolOid,
      @RequestParam(name = "sendingClass", required = false) String sendingClass,
      @RequestParam(name = "updatedAfter", required = false) String updatedAfter,
      @RequestParam(name = "start", required = false) String start,
      @RequestParam(name = "rows", required = false) String rows,
      @RequestParam(name = "sortResults", required = false) String sortResults,
      HttpServletRequest request) {

    AuthorityCheckService.Context context = authorityCheckService.getContext();

    String queryString =
        UriComponentsBuilder.newInstance()
            .queryParamIfPresent("q", Optional.ofNullable(searchTerms))
            .queryParamIfPresent("appState", Optional.ofNullable(state))
            .queryParamIfPresent("paymentState", Optional.ofNullable(paymentState))
            .queryParamIfPresent("preferenceChecked", Optional.ofNullable(preferenceChecked))
            .queryParamIfPresent("aoid", Optional.ofNullable(aoid))
            .queryParamIfPresent("aoOids", Optional.ofNullable(aoOids))
            .queryParamIfPresent("groupOid", Optional.ofNullable(groupOid))
            .queryParamIfPresent("baseEducation", Optional.ofNullable(baseEducation))
            .queryParamIfPresent("lopoid", Optional.ofNullable(lopoid))
            .queryParamIfPresent("asId", Optional.ofNullable(asId))
            .queryParamIfPresent("organizationFilter", Optional.ofNullable(organizationFilter))
            .queryParamIfPresent("asSemester", Optional.ofNullable(asSemester))
            .queryParamIfPresent("asYear", Optional.ofNullable(asYear))
            .queryParamIfPresent("aoOid", Optional.ofNullable(aoOid))
            .queryParamIfPresent("discretionaryOnly", Optional.ofNullable(discretionaryOnly))
            .queryParamIfPresent(
                "primaryPreferenceOnly", Optional.ofNullable(primaryPreferenceOnly))
            .queryParamIfPresent("sendingSchoolOid", Optional.ofNullable(sendingSchoolOid))
            .queryParamIfPresent("sendingClass", Optional.ofNullable(sendingClass))
            .queryParamIfPresent("updatedAfter", Optional.ofNullable(updatedAfter))
            .queryParamIfPresent("start", Optional.ofNullable(start))
            .queryParamIfPresent("rows", Optional.ofNullable(rows))
            .queryParamIfPresent("sortResults", Optional.ofNullable(sortResults))
            .build()
            .toUri()
            .getQuery();

    LOG.info("Haetaan hakemuksia haku-appista (listfull GET). Query on {}", queryString);

    final AuditSession auditSession = createAuditSession(request);

    Map<String, String> additionalInfo = new HashMap<>();
    additionalInfo.put("Username from call params", auditSession.getPersonOid());
    additionalInfo.put("queryString", queryString);

    AuditLog.log(
        KoosteAudit.AUDIT,
        auditSession.asAuditUser(),
        ValintaperusteetOperation.HAKEMUS,
        ValintaResource.HAKEMUS,
        auditSession.getPersonOid(),
        Changes.EMPTY,
        additionalInfo);

    CompletableFuture<List<Map<String, Object>>> response =
        this.hakuAppClient
            .get(
                this.urlConfiguration.url("haku-app.applications.listfull") + "?" + queryString,
                new TypeToken<List<Map<String, Object>>>() {},
                Collections.emptyMap(),
                60 * 60 * 1000)
            .thenApply(hakemukset -> filterAuthorizedHakemukset(context, hakemukset));

    return response;
  }

  private List<Map<String, Object>> filterAuthorizedHakemukset(
      AuthorityCheckService.Context context, List<Map<String, Object>> hakemukset) {
    HakukohdeOIDAuthorityCheck authorityCheck =
        authorityCheckService.getHakukohdeOidBasedAuthCheck(context, roles);
    return hakemukset.stream()
        .filter(
            hakemus -> {
              String hakemusOid = (String) hakemus.get("oid");
              List<String> hakukohdeOids = getHakutoiveet(hakemus);
              LOG.info("Hakemuksen {} hakutoiveet: {}", hakemusOid, hakukohdeOids);
              return hakukohdeOids.stream().anyMatch(authorityCheck);
            })
        .collect(Collectors.toList());
  }

  private List<String> getHakutoiveet(Map<String, Object> hakemus) {
    @SuppressWarnings("unchecked")
    Map<String, Object> answers = (Map<String, Object>) hakemus.getOrDefault("answers", Map.of());
    @SuppressWarnings("unchecked")
    Map<String, String> hakutoiveet =
        (Map<String, String>) answers.getOrDefault("hakutoiveet", Map.of());
    return hakutoiveet.entrySet().stream()
        .filter(
            entry ->
                entry.getKey().endsWith("-Koulutus-id") && StringUtils.isNotBlank(entry.getValue()))
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }
}
