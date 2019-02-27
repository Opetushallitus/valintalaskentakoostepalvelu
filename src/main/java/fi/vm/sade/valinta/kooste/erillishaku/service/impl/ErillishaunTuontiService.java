package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import static com.codepoetics.protonpack.StreamUtils.zip;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_HAKEMUSPALVELUN_VIRHE;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_OPPIJANUMEROREKISTERIN_VIRHE;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_RIVIN_HAKEMINEN_HENKILOLLA_VIRHE;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.HenkilonRivinPaattelyEpaonnistuiException;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.ainoastaanHakemuksenTilaPaivitys;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.autoTaytto;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.createHakemusprototyyppi;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.isKesken;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.riviWithHenkiloData;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.toErillishaunHakijaDTO;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.toPoistettavaErillishaunHakijaDTO;
import static io.reactivex.schedulers.Schedulers.newThread;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MINUTES;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.sharedutils.AuditLog;
import fi.vm.sade.sharedutils.ValintaResource;
import fi.vm.sade.sharedutils.ValintaperusteetOperation;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Maksuvelvollisuus;
import fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource;
import fi.vm.sade.valinta.kooste.excel.ExcelValidointiPoikkeus;
import fi.vm.sade.valinta.kooste.exception.ErillishaunDataException;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.LukuvuosimaksuMuutos;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Maksuntila;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Valinnantulos;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tunniste;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ErillishaunTuontiService extends ErillishaunTuontiValidator {
    private static final Logger LOG = LoggerFactory.getLogger(ErillishaunTuontiService.class);

    private final ApplicationAsyncResource applicationAsyncResource;
    private final OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource;
    private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
    private final Scheduler scheduler;
    private KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    public ErillishaunTuontiService(ApplicationAsyncResource applicationAsyncResource,
                                    OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource,
                                    ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
                                    KoodistoCachedAsyncResource koodistoCachedAsyncResource,
                                    Scheduler scheduler) {
        super(koodistoCachedAsyncResource);
        this.applicationAsyncResource = applicationAsyncResource;
        this.oppijanumerorekisteriAsyncResource = oppijanumerorekisteriAsyncResource;
        this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
        this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
        this.scheduler = scheduler;
    }

    @Autowired
    public ErillishaunTuontiService(ApplicationAsyncResource applicationAsyncResource,
                                    OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource,
                                    ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
                                    KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        this(applicationAsyncResource,
            oppijanumerorekisteriAsyncResource,
            valintaTulosServiceAsyncResource,
            koodistoCachedAsyncResource,
            newThread());
    }

    public void tuoExcelistä(AuditSession auditSession, KirjeProsessi prosessi, ErillishakuDTO erillishaku, InputStream data) {
        tuoData(auditSession, prosessi, erillishaku, (haku) -> new ImportedErillisHakuExcel(haku.getHakutyyppi(), data, koodistoCachedAsyncResource), true);
    }

    public void tuoJson(AuditSession auditSession, KirjeProsessi prosessi, ErillishakuDTO erillishaku, List<ErillishakuRivi> erillishakuRivit, final boolean saveApplications) {
        tuoData(auditSession, prosessi, erillishaku, (haku) -> new ImportedErillisHakuExcel(erillishakuRivit), saveApplications);
    }

    private void tuoData(AuditSession auditSession, KirjeProsessi prosessi, ErillishakuDTO erillishaku, Function<ErillishakuDTO, ImportedErillisHakuExcel> importer, final boolean saveApplications) {
        Observable.just(erillishaku).subscribeOn(scheduler).subscribe(haku -> {
            final ImportedErillisHakuExcel erillishakuExcel;
            try {
                erillishakuExcel = importer.apply(haku);
                tuoHakijatJaLuoHakemukset(auditSession, prosessi, erillishakuExcel, saveApplications, haku);
            } catch(ErillishaunDataException dataException) {
                LOG.warn("excel ei validi:", dataException);
                prosessi.keskeyta(Poikkeus.koostepalvelupoikkeus(ErillishakuResource.POIKKEUS_VIALLINEN_DATAJOUKKO,
                        dataException.getPoikkeusRivit().stream().map(p -> new Tunniste("Rivi " + p.getIndeksi() + ": " + p.getSelite(),ErillishakuResource.RIVIN_TUNNISTE_KAYTTOLIITTYMAAN)).collect(Collectors.toList())));
            } catch(ExcelValidointiPoikkeus validointiPoikkeus) {
                LOG.warn("excel ei validi", validointiPoikkeus);
                prosessi.keskeyta(validointiPoikkeus.getMessage());
            } catch(Exception e) {
                errorLogIncludingHttpResponse("unexpected tuoData exception!", e);
                prosessi.keskeyta();
            }
        }, poikkeus -> {
            errorLogIncludingHttpResponse("Erillishaun tuonti keskeytyi virheeseen", poikkeus);
            prosessi.keskeyta();
        }, () -> LOG.info("Tuonti lopetettiin"));
    }

    private void tuoHakijatJaLuoHakemukset(final AuditSession auditSession, final KirjeProsessi prosessi, final ImportedErillisHakuExcel erillishakuExcel, final boolean saveApplications, final ErillishakuDTO haku) {
        final String username = auditSession.getPersonOid();

        LOG.info("Aloitetaan tuonti. Rivit=" + erillishakuExcel.rivit.size());
        final List<ErillishakuRivi> rivit = autoTaytto(erillishakuExcel.rivit);
        validoiRivit(prosessi,haku,rivit,saveApplications);

        List<ErillishakuRivi> lisattavatTaiKeskeneraiset = luoHenkilotJaKasitteleHakemukset(prosessi, haku, rivit.stream().filter(rivi -> !rivi.isPoistetaankoRivi()).collect(Collectors.toList()), saveApplications);

        LOG.info("Viedaan hakijoita ({}kpl) jonoon {}", lisattavatTaiKeskeneraiset.size(), haku.getValintatapajonoOid());

        List<ErillishakuRivi> poistettavat = rivit.stream().filter(rivi -> rivi.isPoistetaankoRivi()).collect(Collectors.toList());
        passivoiHakemukset(poistettavat);

        Observable<String> maksuntilojenTallennus = maksutilojenTallennus(auditSession, haku, lisattavatTaiKeskeneraiset);

        Observable<List<Poikkeus>> vastaanotonJaValinnantuloksenTallennus = vastaanotonJaValinnantuloksenTallennus(auditSession, haku, lisattavatTaiKeskeneraiset, poistettavat);

        Observable.combineLatest(maksuntilojenTallennus, vastaanotonJaValinnantuloksenTallennus, (m, poikkeukset) -> poikkeukset).subscribe(
                poikkeukset -> {
                    Set<String> epaonnistuneet = poikkeukset.stream()
                            .flatMap(p -> p.getTunnisteet().stream())
                            .filter(t -> t.getTyyppi().equals(Poikkeus.HAKEMUSOID))
                            .map(Tunniste::getTunniste)
                            .collect(Collectors.toSet());
                    Stream<ErillishaunHakijaDTO> lokitettavat = Stream.concat(
                            lisattavatTaiKeskeneraiset.stream().map(rivi -> toErillishaunHakijaDTO(haku, rivi)),
                            poistettavat.stream().map(rivi -> toPoistettavaErillishaunHakijaDTO(haku, rivi)));
                    lokitettavat
                            .filter(h -> !epaonnistuneet.contains(h.getHakemusOid()))
                            .forEach(hakijaDTO -> {
                                Map<String, String> additionalAuditInfo = new HashMap<>();
                                additionalAuditInfo.put("hakuOid", haku.getHakuOid());
                                additionalAuditInfo.put("hakukohdeOid", haku.getHakukohdeOid());
                                additionalAuditInfo.put("valintatapajonoOid", haku.getValintatapajonoOid());
                                if (hakijaDTO.getPoistetaankoTulokset()) {
                                    AuditLog.log(KoosteAudit.AUDIT, auditSession.asAuditUser(), ValintaperusteetOperation.ERILLISHAKU_TUONTI_HAKIJA_POISTO, ValintaResource.ERILLISHAUNTUONTISERVICE, hakijaDTO.getHakijaOid(), Changes.deleteDto(hakijaDTO), additionalAuditInfo);
                                } else {
                                    AuditLog.log(KoosteAudit.AUDIT, auditSession.asAuditUser(), ValintaperusteetOperation.ERILLISHAKU_TUONTI_HAKIJA_PAIVITYS, ValintaResource.ERILLISHAUNTUONTISERVICE, hakijaDTO.getHakijaOid(), Changes.addedDto(hakijaDTO), additionalAuditInfo);
                                }
                        });
                    if (poikkeukset.isEmpty()) {
                        prosessi.vaiheValmistui();
                        prosessi.valmistui("ok");
                    } else {
                        String messages = poikkeukset.stream()
                                .map(Poikkeus::getViesti)
                                .collect(Collectors.joining(", "));
                        LOG.error(String.format(
                                "Osa erillishaun %s tulosten tallennuksesta epäonnistui: %s",
                                haku.getHakuOid(),
                                messages
                        ));
                        prosessi.keskeyta(poikkeukset);
                    }
                },
                t -> {
                    errorLogIncludingHttpResponse("Erillishaun tallennus epäonnistui", t);
                    prosessi.keskeyta(new Poikkeus(Poikkeus.KOOSTEPALVELU, "", t.getMessage()));
                }
        );
    }

    private void passivoiHakemukset(List<ErillishakuRivi> poistettavat) {
        if (!poistettavat.isEmpty()) {
            List<String> hakemusOidit = poistettavat.stream().map(ErillishakuRivi::getHakemusOid).collect(Collectors.toList());
            applicationAsyncResource
                .changeStateOfApplicationsToPassive(hakemusOidit, "Passivoitu erillishaun valintalaskennan käyttöliittymästä")
                .timeout(1, MINUTES)
                .blockingFirst();
        }
    }

    private Observable<String> maksutilojenTallennus(final AuditSession auditSession, final ErillishakuDTO haku, final List<ErillishakuRivi> lisattavatTaiKeskeneraiset) {
        final Map<String, Maksuntila> maksuntilat = lisattavatTaiKeskeneraiset.stream().filter(l -> l.getMaksuntila() != null)
                .filter(l -> Maksuvelvollisuus.REQUIRED.equals(l.getMaksuvelvollisuus())).collect(Collectors.toMap(l -> l.getPersonOid(), l -> l.getMaksuntila()));

        return valintaTulosServiceAsyncResource.fetchLukuvuosimaksut(haku.getHakukohdeOid(), auditSession).flatMap(nykyisetLukuvuosimaksut -> {
            Map<String, Maksuntila> vanhatMaksuntilat = nykyisetLukuvuosimaksut.stream().collect(Collectors.toMap(l -> l.getPersonOid(), l -> l.getMaksuntila()));
            List<LukuvuosimaksuMuutos> muuttuneetLukuvuosimaksut = maksuntilat.entrySet().stream().filter(e -> {
                final String personOid = e.getKey();
                Maksuntila vanhaMaksuntila = ofNullable(vanhatMaksuntilat.get(personOid)).filter(Objects::nonNull).orElse(Maksuntila.MAKSAMATTA);
                return !e.getValue().equals(vanhaMaksuntila);
            }).map(e -> new LukuvuosimaksuMuutos(e.getKey(), e.getValue())).collect(Collectors.toList());
            if(muuttuneetLukuvuosimaksut.isEmpty()) {
                return Observable.just("OK");
            } else {
                return valintaTulosServiceAsyncResource.saveLukuvuosimaksut(haku.getHakukohdeOid(), auditSession, muuttuneetLukuvuosimaksut);
            }
        });
    }

    private List<ErillishakuRivi> luoHenkilotJaKasitteleHakemukset(final KirjeProsessi prosessi, final ErillishakuDTO haku, final List<ErillishakuRivi> lisattavatTaiKeskeneraiset, final boolean saveApplications) {
        LOG.info("lisattavatTaiKeskeneraiset="+lisattavatTaiKeskeneraiset.size());
        if(!lisattavatTaiKeskeneraiset.isEmpty()) {
            LOG.info("Haetaan/luodaan henkilöt");
            final List<HenkiloPerustietoDto> henkilot;
            List<HenkiloCreateDTO> henkiloCreateDTOS = lisattavatTaiKeskeneraiset.stream()
                    .map(rivi -> rivi.toHenkiloCreateDTO(convertKansalaisuusKoodi(rivi.getKansalaisuus())))
                    .collect(Collectors.toList());
            try {
                henkilot = oppijanumerorekisteriAsyncResource.haeTaiLuoHenkilot(henkiloCreateDTOS).timeout(1, MINUTES).blockingFirst();
                LOG.info("Luotiin henkilot=" + henkilot.stream().map(h -> h.getOidHenkilo()).collect(Collectors.toList()));
            } catch (Exception e) {
                if(e.getCause() != null && e.getCause() instanceof WebApplicationException){
                    LOG.error(POIKKEUS_OPPIJANUMEROREKISTERIN_VIRHE + ". lisattavatTaiKeskeneraiset: {}. Response {}", henkiloCreateDTOS, getResponseAsString(e), e);
                } else {
                    LOG.error(POIKKEUS_OPPIJANUMEROREKISTERIN_VIRHE + ". lisattavatTaiKeskeneraiset: {}", henkiloCreateDTOS, e);
                }
                prosessi.keskeyta(Poikkeus.oppijanumerorekisteripoikkeus(POIKKEUS_OPPIJANUMEROREKISTERIN_VIRHE));
                throw e;
            }
            LOG.info("Käsitellään hakemukset ({}kpl)", lisattavatTaiKeskeneraiset.size());
            return kasitteleHakemukset(haku, henkilot, lisattavatTaiKeskeneraiset, saveApplications, prosessi);
        }
        return lisattavatTaiKeskeneraiset;
    }

    private String getResponseAsString(Exception e) {
        Response response = ((WebApplicationException) e.getCause()).getResponse();
        if (response.getEntity() != null && response.getEntity() instanceof InputStream) {
            return new Scanner((InputStream) response.getEntity()).useDelimiter("\\A").next();
        } else if(response.getEntity() != null){
            return response.getEntity().toString();
        }
        return "null";
    }

    private Observable<List<Poikkeus>> vastaanotonJaValinnantuloksenTallennus(final AuditSession auditSession, final ErillishakuDTO haku, final List<ErillishakuRivi> lisattavatTaiKeskeneraiset, final List<ErillishakuRivi> poistettavat) {
        List<ErillishakuRivi> lisattavat = lisattavatTaiKeskeneraiset.stream().filter(rivi -> !isKesken(rivi)).collect(Collectors.toList());
        List<ErillishakuRivi> kesken = lisattavatTaiKeskeneraiset.stream().filter(rivi -> isKesken(rivi)).collect(Collectors.toList());

        final Map<String, Valinnantulos> vanhatValinnantulokset = new HashMap<>();
        if(kesken.size() > 0) {
            vanhatValinnantulokset.putAll(valintaTulosServiceAsyncResource.getErillishaunValinnantulokset(auditSession, haku.getValintatapajonoOid())
                .timeout(5, MINUTES)
                .blockingFirst()
                .stream()
                .collect(Collectors.toMap(Valinnantulos::getHakemusOid, v -> v)));
        }

        return doValinnantuloksenTallennusValintaTulosServiceen(auditSession, haku, createValinnantuloksetForValintaTulosService(haku, lisattavat, kesken, poistettavat, vanhatValinnantulokset));
    }

    private List<Valinnantulos> createValinnantuloksetForValintaTulosService(final ErillishakuDTO haku,
                                                                             final List<ErillishakuRivi> lisattavat,
                                                                             final List<ErillishakuRivi> kesken,
                                                                             final List<ErillishakuRivi> poistettavat,
                                                                             final Map<String, Valinnantulos> vanhatValinnantulokset) {
        return Stream.concat(Stream.concat(
                poistettavat.stream()
                        .map(rivi -> toErillishaunHakijaDTO(haku, rivi))
                        .map(Valinnantulos::of),
                lisattavat.stream()
                        .map(rivi -> toErillishaunHakijaDTO(haku, rivi))
                        .map(hakijaDTO -> Valinnantulos.of(hakijaDTO, ainoastaanHakemuksenTilaPaivitys(hakijaDTO)))
                ),
                kesken.stream()
                        .map(rivi -> toErillishaunHakijaDTO(haku, rivi))
                        .filter(hakijaDTO -> vanhatValinnantulokset.containsKey(hakijaDTO.hakemusOid))
                        .map(hakijaDTO -> {
                            Valinnantulos valinnantulos = vanhatValinnantulokset.get(hakijaDTO.hakemusOid);
                            valinnantulos.setPoistettava(true);
                            return valinnantulos;
                        })
        ).collect(Collectors.toList());
    }


    private Observable<List<Poikkeus>> doValinnantuloksenTallennusValintaTulosServiceen(
            AuditSession auditSession,
            ErillishakuDTO haku,
            List<Valinnantulos> valinnantuloksetForValintaTulosService) {

        if (valinnantuloksetForValintaTulosService.isEmpty()) {
            return Observable.just(Collections.emptyList());
        }
        return valintaTulosServiceAsyncResource.postErillishaunValinnantulokset(
                auditSession,
                haku.getValintatapajonoOid(),
                valinnantuloksetForValintaTulosService)
                .map(r -> r.stream().map(s -> new Poikkeus(
                        Poikkeus.KOOSTEPALVELU,
                        Poikkeus.VALINTA_TULOS_SERVICE,
                        s.message,
                        new Tunniste(s.hakemusOid, Poikkeus.HAKEMUSOID)
                )).collect(Collectors.toList()))
                .onErrorResumeNext((Throwable t) -> Observable.error(new RuntimeException(String.format(
                        "Erillishaun %s valinnantulosten tallennus valinta-tulos-serviceen epäonnistui",
                        haku.getHakuOid()
                ), t)));
    }

    private List<ErillishakuRivi> kasitteleHakemukset(ErillishakuDTO haku, List<HenkiloPerustietoDto> henkilot, List<ErillishakuRivi> lisattavatTaiKeskeneraiset, boolean saveApplications, KirjeProsessi prosessi) {
        try {
            final List<ErillishakuRivi> rivitWithHenkiloData = henkilot.stream().map(h -> riviWithHenkiloData(h, lisattavatTaiKeskeneraiset)).collect(Collectors.toList());
            if(saveApplications) {
                List<HakemusPrototyyppi> hakemusPrototyypit = rivitWithHenkiloData.stream().map(rivi -> createHakemusprototyyppi(rivi, convertKuntaNimiToKuntaKoodi(rivi.getKotikunta()))).collect(Collectors.toList());
                LOG.info("Tallennetaan hakemukset ({}kpl) hakemuspalveluun", lisattavatTaiKeskeneraiset.size());
                final List<HakemusWrapper> hakemukset;
                try {
                    hakemukset = applicationAsyncResource.putApplicationPrototypes(haku.getHakuOid(), haku.getHakukohdeOid(), haku.getTarjoajaOid(), hakemusPrototyypit)
                        .timeout(1, MINUTES)
                        .blockingFirst();
                } catch (Exception e) {
                    errorLogIncludingHttpResponse(String.format("Error updating application prototypes %s", Arrays.toString(hakemusPrototyypit.toArray())), e);
                    LOG.error("Rivi with henkilodata={}", Arrays.toString(rivitWithHenkiloData.toArray()));
                    throw e;
                }
                if (hakemukset.size() != lisattavatTaiKeskeneraiset.size()) { // 1-1 relationship assumed
                    LOG.warn("Hakemuspalveluun tallennettujen hakemusten lukumäärä {}kpl on väärä!! Odotettiin {}kpl.",
                            hakemukset.size(), lisattavatTaiKeskeneraiset.size());
                }
                return zip(hakemukset.stream(), rivitWithHenkiloData.stream(), (hakemus, rivi) ->
                        rivi.withHakemusOid(hakemus.getOid())).collect(Collectors.toList());
            } else {
                return rivitWithHenkiloData;
            }
        } catch (HenkilonRivinPaattelyEpaonnistuiException e) {
            errorLogIncludingHttpResponse(POIKKEUS_RIVIN_HAKEMINEN_HENKILOLLA_VIRHE, e);
            prosessi.keskeyta(Poikkeus.hakemuspalvelupoikkeus(POIKKEUS_RIVIN_HAKEMINEN_HENKILOLLA_VIRHE + " " + e.getMessage()));
            throw e;
        } catch (Throwable e) { // temporary catch to avoid missing service dependencies
            errorLogIncludingHttpResponse(POIKKEUS_HAKEMUSPALVELUN_VIRHE, e);
            prosessi.keskeyta(Poikkeus.hakemuspalvelupoikkeus(POIKKEUS_HAKEMUSPALVELUN_VIRHE));
            throw e;
        }
    }

    private void errorLogIncludingHttpResponse(String message, Throwable exception) {
        LOG.error(HttpExceptionWithResponse.appendWrappedResponse(message, exception), exception);
    }
}

