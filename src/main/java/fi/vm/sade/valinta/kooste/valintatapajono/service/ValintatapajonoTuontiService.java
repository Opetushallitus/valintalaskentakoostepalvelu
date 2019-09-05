package fi.vm.sade.valinta.kooste.valintatapajono.service;

import com.google.gson.GsonBuilder;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.auditlog.User;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import fi.vm.sade.valinta.sharedutils.ValintaResource;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.DokumentinSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.PoikkeusKasittelijaSovitin;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRivi;
import fi.vm.sade.valinta.seuranta.dto.VirheilmoitusDto;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Service
public class ValintatapajonoTuontiService {
    private static final Logger LOG = LoggerFactory.getLogger(ValintatapajonoTuontiService.class);
    private static final String VALMIS = "valmis";
    @Autowired
    private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    @Autowired
    private TarjontaAsyncResource tarjontaAsyncResource;
    @Autowired
    private ApplicationAsyncResource applicationAsyncResource;
    @Autowired
    private AtaruAsyncResource ataruAsyncResource;
    @Autowired
    private ValintalaskentaAsyncResource valintalaskentaAsyncResource;
    @Autowired
    private DokumentinSeurantaAsyncResource dokumentinSeurantaAsyncResource;

    public void tuo (BiFunction<List<ValintatietoValinnanvaiheDTO>, List<HakemusWrapper>, Collection<ValintatapajonoRivi>> riviFunction,
        final String hakuOid,
        final String hakukohdeOid,
        final String tarjoajaOid,
        final String valintatapajonoOid,
        AsyncResponse asyncResponse,
        User user) {
        AtomicReference<String> dokumenttiIdRef = new AtomicReference<>(null);
        AtomicInteger counter = new AtomicInteger(
                1 // valinnanvaiheet
                        + 1 // valintaperusteet
                        + 1 // hakemukset
                        + 1 // dokumentti
                //+1 // org oikeuksien tarkistus
        );
        AtomicReference<List<ValintatietoValinnanvaiheDTO>> valinnanvaiheetRef = new AtomicReference<>();
        AtomicReference<List<ValinnanVaiheJonoillaDTO>> valintaperusteetRef = new AtomicReference<>();
        AtomicReference<List<HakemusWrapper>> hakemuksetRef = new AtomicReference<>();

        final Supplier<Void> mergeSuplier = () -> {
            if (counter.decrementAndGet() == 0) {
                Collection<ValintatapajonoRivi> rivit;
                try {
                    rivit = riviFunction.apply(valinnanvaiheetRef.get(), hakemuksetRef.get());
                } catch (Throwable t) {
                    poikkeusKasittelija("Rivien lukeminen annetuista tiedoista epäonnistui", asyncResponse, dokumenttiIdRef).accept(t);
                    return null;
                }
                try {
                    ValinnanvaiheDTO valinnanvaihe = ValintatapajonoTuontiConverter.konvertoi(hakuOid, hakukohdeOid, valintatapajonoOid,
                            valintaperusteetRef.get(), hakemuksetRef.get(), valinnanvaiheetRef.get(), rivit);
                    LOG.info("{}", new GsonBuilder().setPrettyPrinting().create().toJson(valinnanvaihe));
                    valintalaskentaAsyncResource.lisaaTuloksia(hakuOid, hakukohdeOid, tarjoajaOid, valinnanvaihe).subscribe(
                            ok -> {
                                try {
                                    valinnanvaihe.getValintatapajonot()
                                            .forEach(
                                                    v -> {
                                                        v.getJonosijat().forEach(jonosija -> {
                                                            Map<String,String> additionalAuditFields = new HashMap<>();
                                                            additionalAuditFields.put("hakuOid", hakuOid);
                                                            additionalAuditFields.put("hakukohdeOid", hakukohdeOid);
                                                            additionalAuditFields.put("valinnanvaiheOid", valinnanvaihe.getValinnanvaiheoid());
                                                            additionalAuditFields.put("valintatapajonoOid", v.getValintatapajonooid());
                                                            AuditLog.log(KoosteAudit.AUDIT, user, ValintaperusteetOperation.VALINNANVAIHE_TUONTI_EXCEL, ValintaResource.VALINTATAPAJONOSERVICE, jonosija.getHakijaOid(), Changes.addedDto(jonosija), additionalAuditFields);
                                                        });
                                                    }
                                            );
                                } catch (Throwable t) {
                                    LOG.error("Audit logitus epäonnistui", t);
                                }
                                dokumentinSeurantaAsyncResource.paivitaDokumenttiId(dokumenttiIdRef.get(), VALMIS).subscribe(
                                        dontcare -> {
                                            LOG.error("Saatiin paivitettya dokId");
                                        },
                                        dontcare ->
                                        {
                                            LOG.error("Ei saatu paivitettya!", dontcare);
                                        });
                            },
                            poikkeusKasittelija("Tallennus valintapalveluun epäonnistui", asyncResponse, dokumenttiIdRef));
                    LOG.info("Saatiin vastaus muodostettua hakukohteelle {} haussa {}. Palautetaan se asynkronisena paluuarvona.", hakukohdeOid, hakuOid);
                    dokumentinSeurantaAsyncResource.paivitaKuvaus(dokumenttiIdRef.get(), "Tuonnin esitiedot haettu onnistuneesti. Tallennetaan kantaan...").subscribe(
                            dontcare -> {},
                            dontcare -> {
                                LOG.error("Onnistumisen ilmoittamisessa virhe!", dontcare);
                            });
                } catch (Throwable t) {
                    poikkeusKasittelija("Tallennus valintapalveluun epäonnistui", asyncResponse, dokumenttiIdRef).accept(t);
                    return null;
                }
            }
            return null;
        };
        valintalaskentaAsyncResource.laskennantulokset(hakukohdeOid).subscribe(
                valinnanvaiheet -> {
                    valinnanvaiheetRef.set(valinnanvaiheet);
                    mergeSuplier.get();
                },
                poikkeusKasittelija("Valinnanvaiheiden hakeminen epäonnistui", asyncResponse, dokumenttiIdRef));
        valintaperusteetAsyncResource.haeIlmanlaskentaa(hakukohdeOid).subscribe(
                valintaperusteet -> {
                    valintaperusteetRef.set(valintaperusteet);
                    mergeSuplier.get();
                },
                poikkeusKasittelija("Hakemusten hakeminen epäonnistui", asyncResponse, dokumenttiIdRef)
        );
        tarjontaAsyncResource.haeHaku(hakuOid)
                .flatMap(haku -> {
                    if (haku.getAtaruLomakeAvain() == null) {
                        return applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid);
                    } else {
                        return ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid);
                    }
                })
                .subscribe(hakemukset -> {
                    if (hakemukset == null || hakemukset.isEmpty()) {
                        poikkeusKasittelija("Ei yhtään hakemusta hakukohteessa", asyncResponse, dokumenttiIdRef).accept(null);
                    } else {
                        hakemuksetRef.set(hakemukset);
                        mergeSuplier.get();
                    }
                }, poikkeusKasittelija("Hakemusten hakeminen epäonnistui", asyncResponse, dokumenttiIdRef));

        dokumentinSeurantaAsyncResource.luoDokumentti("Valintatapajonon tuonti").subscribe(
                dokumenttiId -> {
                    try {
                        asyncResponse.resume(Response.ok().header("Content-Type", "text/plain").entity(dokumenttiId).build());
                        dokumenttiIdRef.set(dokumenttiId);
                        mergeSuplier.get();
                    } catch (Throwable t) {
                        LOG.error("Aikakatkaisu ehti ensin. Palvelu on todennäköisesti kovan kuormanalla.", t);
                    }
                }, poikkeusKasittelija("Seurantapalveluun ei saatu yhteyttä", asyncResponse, dokumenttiIdRef));
    }

    private PoikkeusKasittelijaSovitin poikkeusKasittelija(String viesti, AsyncResponse asyncResponse, AtomicReference<String> dokumenttiIdRef) {
        return new PoikkeusKasittelijaSovitin(poikkeus -> {
            if (poikkeus == null) {
                LOG.error("###Poikkeus tuonnissa {}\r\n###", viesti);
            } else {
                LOG.error("###Poikkeus tuonnissa :" + viesti + "###", poikkeus);
            }
            try {
                asyncResponse.resume(Response.serverError().entity(viesti).build());
            } catch (Throwable t) {
                // ei väliä vaikka response jos tehty
            }
            try {
                String dokumenttiId = dokumenttiIdRef.get();
                if (dokumenttiId != null) {
                    dokumentinSeurantaAsyncResource.lisaaVirheilmoituksia(dokumenttiId, Arrays.asList(new VirheilmoitusDto("", viesti))).subscribe(
                            dontcare -> {},
                            dontcare -> {
                                LOG.error("Virheen ilmoittamisessa virhe!", dontcare);
                            });
                }
            } catch (Throwable t) {
                LOG.error("Odottamaton virhe", t);
            }
        });
    }
}

