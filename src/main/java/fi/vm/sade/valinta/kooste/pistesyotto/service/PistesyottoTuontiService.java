package fi.vm.sade.valinta.kooste.pistesyotto.service;

import fi.vm.sade.auditlog.valintaperusteet.ValintaperusteetOperation;
import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoArvo;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoDataRiviListAdapter;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoRivi;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PistesyottoTuontiService extends AbstractPistesyottoKoosteService {
    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoTuontiService.class);

    @Autowired
    public PistesyottoTuontiService(
            ValintalaskentaValintakoeAsyncResource valintakoeResource,
            ValintaperusteetAsyncResource valintaperusteetResource,
            ValintapisteAsyncResource valintapisteAsyncResource,
            ApplicationAsyncResource applicationAsyncResource,
            SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
            TarjontaAsyncResource tarjontaAsyncResource,
            OhjausparametritAsyncResource ohjausparametritAsyncResource,
            OrganisaatioAsyncResource organisaatioAsyncResource,
            ValintaperusteetAsyncResource valintaperusteetAsyncResource,
            ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
        super(applicationAsyncResource,
                valintapisteAsyncResource,
                suoritusrekisteriAsyncResource,
                tarjontaAsyncResource,
                ohjausparametritAsyncResource,
                organisaatioAsyncResource,
                valintaperusteetAsyncResource,
                valintalaskentaValintakoeAsyncResource);
    }

    private void logPistesyotonTuontiEpaonnistui(Throwable t) {
        LOG.error(HttpExceptionWithResponse.appendWrappedResponse("Pistesyötön tuonti epäonnistui", t), t);
    }

    private List<String> getPistesyottoExcelVirheet(PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri, Map<String, ApplicationAdditionalDataDTO> oidToAdditionalMapping) {
        return pistesyottoTuontiAdapteri
                .getRivit().stream()
                .flatMap(
                        rivi -> {
                            String nimi = PistesyottoExcel.additionalDataToNimi(oidToAdditionalMapping.get(rivi.getOid()));
                            if (!Optional.ofNullable(rivi.getNimi()).orElse("").equals(nimi)) {
                                String virheIlmoitus = new StringBuffer()
                                        .append("Hakemuksella (OID = ")
                                        .append(rivi.getOid())
                                        .append(") nimet ei täsmää: ")
                                        .append(rivi.getNimi())
                                        .append(" != ")
                                        .append(nimi)
                                        .toString();
                                return Stream.of(virheIlmoitus);
                            }
                            if (!rivi.isValidi()) {
                                LOG.warn("Rivi on muuttunut mutta viallinen joten ilmoitetaan virheestä!");

                                for (PistesyottoArvo arvo : rivi.getArvot()) {
                                    if (!arvo.isValidi()) {
                                        String virheIlmoitus = new StringBuffer()
                                                .append("Henkilöllä ")
                                                .append(rivi.getNimi())
                                                .append(" (")
                                                .append(rivi.getOid())
                                                .append(")")
                                                .append(" oli virheellinen arvo '")
                                                .append(arvo.getArvo())
                                                .append("'")
                                                .append(" kohdassa ")
                                                .append(arvo.getTunniste())
                                                .toString();
                                        return Stream.of(virheIlmoitus);
                                    }
                                }
                            }
                            return Stream.empty();
                        }
                ).collect(Collectors.toList());
    }

    public void tuo(String username, AuditSession auditSession, String hakuOid, String hakukohdeOid, DokumenttiProsessi prosessi, InputStream stream) {
        PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri = new PistesyottoDataRiviListAdapter();
        muodostaPistesyottoExcel(hakuOid, hakukohdeOid, auditSession, prosessi, Collections.singleton(pistesyottoTuontiAdapteri))
                .flatMap(p -> {
                    PistesyottoExcel pistesyottoExcel = p.getLeft();
                    Map<String, ApplicationAdditionalDataDTO> pistetiedot = p.getRight();
                    try {
                        pistesyottoExcel.getExcel().tuoXlsx(stream);
                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                    List<String> virheet = getPistesyottoExcelVirheet(pistesyottoTuontiAdapteri, pistetiedot);
                    if (!virheet.isEmpty()) {
                        String v = virheet.stream().collect(Collectors.joining(", "));
                        return Observable.error(new RuntimeException(String.format("Virheitä pistesyöttöriveissä %s", v)));
                    }
                    Date valmistuminen = new Date();
                    Map<String, List<AbstractPistesyottoKoosteService.SingleKielikoeTulos>> uudetKielikoetulokset = new HashMap<>();
                    List<ApplicationAdditionalDataDTO> uudetPistetiedot =
                            pistesyottoTuontiAdapteri
                                    .getRivit().stream()
                                    .filter(PistesyottoRivi::isValidi)
                                    .flatMap(rivi -> {
                                        final String hakemusOid = rivi.getOid();
                                        ApplicationAdditionalDataDTO additionalData = pistetiedot.get(hakemusOid);
                                        Map<String, String> newPistetiedot = rivi.asAdditionalData(
                                                valintakoetunniste -> pistesyottoExcel.onkoHakijaOsallistujaValintakokeeseen(hakemusOid, valintakoetunniste));
                                        siirraKielikoepistetiedotKielikoetulosMapiin(valmistuminen, uudetKielikoetulokset, hakemusOid, newPistetiedot);
                                        additionalData.setAdditionalData(newPistetiedot);
                                        return Stream.of(additionalData);
                                    })
                                    .filter(Objects::nonNull)
                                    .filter(a -> !a.getAdditionalData().isEmpty())
                                    .collect(Collectors.toList());

                    if (uudetPistetiedot.isEmpty()) {
                        LOG.info("Pistesyötössä hakukohteeseen {} ei yhtäkään muuttunutta tietoa tallennettavaksi", hakukohdeOid);
                        return Observable.just(null);
                    } else {
                        LOG.info("Pistesyötössä hakukohteeseen {} muuttunutta {} tietoa tallennettavaksi", hakukohdeOid, uudetPistetiedot.size());
                        return tallennaKoostetutPistetiedot(hakuOid, hakukohdeOid, uudetPistetiedot,
                                uudetKielikoetulokset, username, ValintaperusteetOperation.PISTETIEDOT_TUONTI_EXCEL, auditSession);
                    }
                })
                .subscribe(x -> {
                    prosessi.inkrementoiTehtyjaToita();
                    prosessi.setDokumenttiId("valmis");
                }, t -> {
                    logPistesyotonTuontiEpaonnistui(t);
                    prosessi.getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Pistesyötön tuonti:", t.getMessage()));
                });
    }
}
