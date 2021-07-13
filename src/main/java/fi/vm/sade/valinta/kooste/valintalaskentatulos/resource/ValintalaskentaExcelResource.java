package fi.vm.sade.valinta.kooste.valintalaskentatulos.resource;

import com.google.common.collect.Sets;
import fi.vm.sade.valinta.kooste.AuthorizationUtil;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Hakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.VastaanottoFilterUtil;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.excel.ValintalaskennanTulosExcel;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.SijoittelunTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.service.ValintakoekutsutExcelService;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetHakukohdeCache;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import io.reactivex.Observable;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/** @Autowired(required = false) Camel-reitit valinnaisiksi poisrefaktorointia odotellessa. */
@Controller("ValintalaskentaExcelResource")
@Path("valintalaskentaexcel")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintalaskentaexcel", description = "Excel-raportteja")
public class ValintalaskentaExcelResource {
  private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaExcelResource.class);
  public static final MediaType APPLICATION_VND_MS_EXCEL =
      new MediaType("application", "vnd.ms-excel");

  @Autowired private ValintakoekutsutExcelService valintakoekutsutExcelService;
  @Autowired private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;
  @Autowired private ValintalaskentaAsyncResource valintalaskentaResource;
  @Autowired private TarjontaAsyncResource tarjontaResource;
  @Autowired private OrganisaatioAsyncResource organisaatioAsyncResource;
  @Autowired private ApplicationAsyncResource applicationResource;
  @Autowired private AtaruAsyncResource ataruAsyncResource;
  @Context private HttpServletRequest httpServletRequestJaxRS;

  @POST
  @Path("/valintakoekutsut/aktivoi")
  @Consumes("application/json")
  @Produces("application/json")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(value = "Hakukohteen hyväksytyt Excel-raporttina", response = Response.class)
  public ProsessiId haeTuloksetExcelMuodossa(
      DokumentinLisatiedot lisatiedot,
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("hakukohdeOid") String hakukohdeOid) {
    if (lisatiedot == null) {
      throw new RuntimeException("ValintakoeOid on pakollinen!");
    }
    try {
      DokumenttiProsessi p =
          new DokumenttiProsessi(
              "Valintalaskentaexcel",
              "Valintakoekutsut taulukkolaskenta tiedosto",
              "",
              Arrays.asList("valintakoekutsut", "taulukkolaskenta"));
      dokumenttiProsessiKomponentti.tuoUusiProsessi(p);
      Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid))
          .subscribe(
              haku ->
                  valintakoekutsutExcelService.luoExcel(
                      p,
                      haku,
                      hakukohdeOid,
                      lisatiedot.getValintakoeTunnisteet(),
                      Sets.newHashSet(
                          Optional.ofNullable(lisatiedot.getHakemusOids())
                              .orElse(Collections.emptyList()))));
      return p.toProsessiId();
    } catch (Exception e) {
      LOG.error(
          "Valintakoekutsut excelin luonti epäonnistui hakukohteelle "
              + hakukohdeOid
              + ", valintakoeoideille"
              + Arrays.toString(lisatiedot.getValintakoeTunnisteet().toArray()),
          e);
      throw new RuntimeException("Valintakoekutsut excelin luonti epäonnistui!", e);
    }
  }

  @Autowired private ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
  @Autowired private SijoittelunTulosExcelKomponentti sijoittelunTulosExcelKomponentti;
  @Autowired private ApplicationAsyncResource applicationAsyncResource;
  @Autowired private DokumenttiAsyncResource dokumenttiAsyncResource;
  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;

  @POST
  @Path("/sijoitteluntulos/aktivoi")
  @Consumes("application/json")
  @Produces("application/json")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(value = "Sijoittelun tulokset Excel-raporttina", response = Response.class)
  public ProsessiId haeSijoittelunTuloksetExcelMuodossa(
      @QueryParam("sijoitteluajoId") String sijoitteluajoId,
      @QueryParam("hakukohdeOid") String hakukohdeOid,
      @QueryParam("hakuOid") String hakuOid) {
    try {
      final DokumenttiProsessi p =
          new DokumenttiProsessi(
              "Sijoitteluntulosexcel",
              "Sijoitteluntulokset taulukkolaskenta tiedosto",
              "",
              Arrays.asList("sijoitteluntulos", "taulukkolaskenta"));
      String id = UUID.randomUUID().toString();
      AuditSession auditSession = AuthorizationUtil.createAuditSession(httpServletRequestJaxRS);
      p.setKokonaistyo(1);
      Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid))
          .flatMap(
              haku -> {
                Observable<List<HakemusWrapper>> hakemuksetO =
                    ((haku.isHakemuspalvelu())
                        ? Observable.fromFuture(
                            ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid))
                        : Observable.fromFuture(
                            applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid)));
                CompletableFuture<Hakukohde> hakukohdeF =
                    tarjontaAsyncResource.haeHakukohde(hakukohdeOid);
                return Observable.zip(
                        Observable.fromFuture(hakukohdeF),
                        Observable.fromFuture(
                            hakukohdeF.thenComposeAsync(
                                hakukohde ->
                                    CompletableFutureUtil.sequence(
                                        hakukohde.tarjoajaOids.stream()
                                            .map(organisaatioAsyncResource::haeOrganisaatio)
                                            .collect(Collectors.toList())))),
                        Observable.fromFuture(
                            hakukohdeF.thenComposeAsync(
                                hakukohde ->
                                    CompletableFutureUtil.sequence(
                                        hakukohde.toteutusOids.stream()
                                            .map(tarjontaAsyncResource::haeToteutus)
                                            .collect(Collectors.toList())))),
                        valintaTulosServiceAsyncResource.findValintatulokset(hakuOid, hakukohdeOid),
                        valintaTulosServiceAsyncResource.fetchLukuvuosimaksut(
                            hakukohdeOid, auditSession),
                        hakemuksetO,
                        valintaTulosServiceAsyncResource.getHakukohdeBySijoitteluajoPlainDTO(
                            hakuOid, hakukohdeOid),
                        Observable.fromFuture(
                            valintalaskentaResource.laskennantulokset(hakukohdeOid)),
                        (tarjontaHakukohde,
                            tarjoajat,
                            toteutukset,
                            valintatulokset,
                            lukuvuosimaksut,
                            hakemukset,
                            hakukohde,
                            valinnanvaiheet) -> {
                          try {
                            String opetuskieli =
                                KirjeetHakukohdeCache.getOpetuskieli(
                                    toteutukset.stream()
                                        .flatMap(toteutus -> toteutus.opetuskielet.stream())
                                        .collect(Collectors.toList()));
                            Teksti hakukohteenNimet = new Teksti(tarjontaHakukohde.nimi);
                            String tarjoajaNimet =
                                Teksti.getTeksti(
                                    tarjoajat.stream()
                                        .map(Organisaatio::getNimi)
                                        .collect(Collectors.toList()),
                                    " - ",
                                    opetuskieli);

                            InputStream xls =
                                sijoittelunTulosExcelKomponentti.luoXls(
                                    VastaanottoFilterUtil.nullifyVastaanottoBasedOnHakemuksenTila(
                                        valintatulokset, hakukohde),
                                    opetuskieli,
                                    hakukohteenNimet.getTeksti(opetuskieli),
                                    tarjoajaNimet,
                                    hakukohdeOid,
                                    hakemukset,
                                    lukuvuosimaksut,
                                    hakukohde,
                                    haku,
                                    valinnanvaiheet);
                            return dokumenttiAsyncResource.tallenna(
                                id,
                                "sijoitteluntulos_" + hakukohdeOid + ".xlsx",
                                DateTime.now().plusHours(24).toDate().getTime(),
                                Collections.emptyList(),
                                "application/vnd.ms-excel",
                                xls);
                          } catch (Throwable e) {
                            LOG.error("Dokumentin generointi epäonnistui", e);
                            p.getPoikkeukset()
                                .add(
                                    new Poikkeus(
                                        Poikkeus.DOKUMENTTIPALVELU,
                                        "Dokumentin generointi",
                                        e.getMessage()));
                            return Observable.<Response>error(e);
                          }
                        })
                    .flatMap(o -> o);
              })
          .subscribe(
              ehkaOk -> {
                LOG.info("Dokumentin generointi valmistui onnistuneesti");
                if (ehkaOk.getStatus() < 300) {
                  p.setDokumenttiId(id);
                  p.inkrementoiTehtyjaToita();
                } else {
                  LOG.error(
                      "Dokumentin tallennus epäonnistui: Dokumentti palvelun paluuarvo {}",
                      ehkaOk.getStatus());
                  p.getPoikkeukset()
                      .add(
                          new Poikkeus(
                              Poikkeus.DOKUMENTTIPALVELU, "Dokumenttipalvelulle tallennus"));
                }
              },
              poikkeus -> {
                LOG.error("Dokumentin tallennus epäonnistui", poikkeus);
                p.getPoikkeukset()
                    .add(
                        new Poikkeus(
                            Poikkeus.DOKUMENTTIPALVELU,
                            "Dokumenttipalvelulle tallennus",
                            poikkeus.getMessage()));
              });
      dokumenttiProsessiKomponentti.tuoUusiProsessi(p);
      return p.toProsessiId();
    } catch (Exception e) {
      LOG.error(
          "Sijoitteluntulos excelin luonti epäonnistui hakukohteelle {} ja sijoitteluajolle {}",
          hakukohdeOid,
          sijoitteluajoId,
          e);
      throw e;
    }
  }

  @GET
  @Path("/valintalaskennantulos/aktivoi")
  @Produces("application/vnd.ms-excel")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
  @ApiOperation(value = "Valintalaskennan tulokset Excel-raporttina", response = Response.class)
  public void haeValintalaskentaTuloksetExcelMuodossa(
      @QueryParam("hakukohdeOid") String hakukohdeOid, @Suspended AsyncResponse asyncResponse) {
    Observable<Hakukohde> hakukohdeObservable =
        Observable.fromFuture(tarjontaResource.haeHakukohde(hakukohdeOid));
    Observable<List<Organisaatio>> tarjoajatObservable =
        hakukohdeObservable.flatMap(
            hakukohde ->
                Observable.fromFuture(
                    CompletableFutureUtil.sequence(
                        hakukohde.tarjoajaOids.stream()
                            .map(organisaatioAsyncResource::haeOrganisaatio)
                            .collect(Collectors.toList()))));
    final Observable<Haku> hakuObservable =
        hakukohdeObservable.flatMap(
            hakukohde -> Observable.fromFuture(tarjontaResource.haeHaku(hakukohde.hakuOid)));
    final Observable<List<ValintatietoValinnanvaiheDTO>> valinnanVaiheetObservable =
        Observable.fromFuture(valintalaskentaResource.laskennantulokset(hakukohdeOid));
    final Observable<List<HakemusWrapper>> hakemuksetObservable =
        hakuObservable.flatMap(
            haku -> {
              if (haku.isHakemuspalvelu()) {
                return Observable.fromFuture(
                    ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid));
              } else {
                return Observable.fromFuture(
                    applicationResource.getApplicationsByOid(haku.oid, hakukohdeOid));
              }
            });
    final Observable<XSSFWorkbook> workbookObservable =
        Observable.combineLatest(
            hakuObservable,
            hakukohdeObservable,
            tarjoajatObservable,
            valinnanVaiheetObservable,
            hakemuksetObservable,
            ValintalaskennanTulosExcel::luoExcel);
    workbookObservable.subscribe(
        (workbook) ->
            asyncResponse.resume(
                Response.ok(
                        Excel.export(workbook),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("content-disposition", "inline; filename=valintalaskennantulos.xlsx")
                    .build()),
        (e) -> {
          LOG.error(
              "Valintalaskennan tulokset -excelin luonti epäonnistui hakukohteelle " + hakukohdeOid,
              e);
          asyncResponse.resume(
              Response.ok(
                      ExcelExportUtil.exportGridAsXls(
                          new Object[][] {
                            new Object[] {
                              "Tarvittavien tietojen hakeminen epäonnistui!",
                              "Virhe: " + e.getMessage()
                            }
                          }),
                      APPLICATION_VND_MS_EXCEL)
                  .header("content-disposition", "inline; filename=yritauudelleen.xls")
                  .build());
        });
  }
}
