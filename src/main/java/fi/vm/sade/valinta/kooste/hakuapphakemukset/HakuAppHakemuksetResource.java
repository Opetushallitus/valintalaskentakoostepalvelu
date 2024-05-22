package fi.vm.sade.valinta.kooste.hakuapphakemukset;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

  @Autowired
  public HakuAppHakemuksetResource(@Qualifier("HakuAppCasClient") RestCasClient hakuAppClient) {
    this.hakuAppClient = hakuAppClient;
    this.urlConfiguration = UrlConfiguration.getInstance();
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @PostMapping(value = "/listfull", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Palauttaa hakuehtoihin sopivien hakemusten tiedot.")
  public CompletableFuture<List<Map<String, Object>>> findFullApplicationsPost(
      final List<Map<String, Object>> applicationSearchData) {

    LOG.info(
        "Haetaan hakemuksia haku-appista (listfull POST). Body on {}",
        applicationSearchData.toString());

    CompletableFuture<List<Map<String, Object>>> response =
        this.hakuAppClient.post(
            this.urlConfiguration.url("haku-app.applications.listfull"),
            new TypeToken<List<Map<String, Object>>>() {},
            applicationSearchData,
            Collections.emptyMap(),
            60 * 60 * 1000);

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
      @RequestParam(name = "sortResults", required = false) String sortResults) {

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

    CompletableFuture<List<Map<String, Object>>> response =
        this.hakuAppClient
            .get(
                this.urlConfiguration.url("haku-app.applications.listfull") + "?" + queryString,
                new TypeToken<List<Map<String, Object>>>() {},
                Collections.emptyMap(),
                60 * 60 * 1000)
            .thenApply(
                hakemukset -> {
                  return hakemukset.stream()
                      .filter(
                          hakemus -> {
                            String hakemusOid = (String) hakemus.get("oid");
                            Map<String, Object> answers =
                                (Map<String, Object>) hakemus.get("answers");
                            Map<String, String> hakutoiveet =
                                (Map<String, String>) answers.get("hakutoiveet");
                            List<String> hakukohdeOids =
                                hakutoiveet.entrySet().stream()
                                    .filter(
                                        entry ->
                                            entry.getKey().endsWith("-Koulutus-id")
                                                && StringUtils.isNotBlank(entry.getValue()))
                                    .map(entry -> entry.getValue())
                                    .collect(Collectors.toList());
                            LOG.info("Hakemuksen {} hakutoiveet: {}", hakemusOid, hakukohdeOids);
                            return authorityCheckService.checkAuthorizationForAnyHakukohde(
                                hakukohdeOids,
                                List.of(
                                    "ROLE_APP_HAKEMUS_READ_UPDATE",
                                    "ROLE_APP_HAKEMUS_READ",
                                    "ROLE_APP_HAKEMUS_CRUD",
                                    "ROLE_APP_HAKEMUS_LISATIETORU",
                                    "ROLE_APP_HAKEMUS_LISATIETOCRUD"));
                          })
                      .collect(Collectors.toList());
                });

    return response;
  }
}
