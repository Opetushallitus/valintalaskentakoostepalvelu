package fi.vm.sade.valinta.kooste.valintatapajono.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.GsonBuilder;

import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.DokumentinSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.util.PoikkeusKasittelijaSovitin;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRivi;
import fi.vm.sade.valinta.seuranta.dto.VirheilmoitusDto;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;

@Service
public class ValintatapajonoTuontiService {
    private static final Logger LOG = LoggerFactory.getLogger(ValintatapajonoTuontiService.class);
    private static final String VALMIS = "valmis";
    @Autowired
    private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    @Autowired
    private ApplicationAsyncResource applicationAsyncResource;
    @Autowired
    private ValintalaskentaAsyncResource valintalaskentaAsyncResource;
    @Autowired
    private DokumentinSeurantaAsyncResource dokumentinSeurantaAsyncResource;

    public void tuo(
            BiFunction<List<ValintatietoValinnanvaiheDTO>, List<Hakemus>, Collection<ValintatapajonoRivi>> riviFunction,
            final String hakuOid,
            final String hakukohdeOid,
            final String tarjoajaOid,
            final String valintatapajonoOid,
            AsyncResponse asyncResponse) {
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
        AtomicReference<List<Hakemus>> hakemuksetRef = new AtomicReference<>();

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
                                LOG.error("Tuli ok viesti");
                                dokumentinSeurantaAsyncResource.paivitaDokumenttiId(dokumenttiIdRef.get(), VALMIS).subscribe(
                                        dontcare -> {
                                            LOG.error("Saatiin paivitettya dokId");
                                        },
                                        dontcare ->
                                        {
                                            LOG.error("Ei saatu paivitettya " + dontcare.getMessage(), dontcare);
                                        });
                            },
                            poikkeusKasittelija("Tallennus valintapalveluun epäonnistui", asyncResponse, dokumenttiIdRef));
                    LOG.info("Saatiin vastaus muodostettua hakukohteelle {} haussa {}. Palautetaan se asynkronisena paluuarvona.", hakukohdeOid, hakuOid);
                    dokumentinSeurantaAsyncResource.paivitaKuvaus(dokumenttiIdRef.get(), "Tuonnin esitiedot haettu onnistuneesti. Tallennetaan kantaan...").subscribe(
                            dontcare -> {},
                            dontcare -> {
                                LOG.error("Onnistumisen ilmoittamisessa virhe! " + dontcare.getMessage(), dontcare);
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
        applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid).subscribe(
                hakemukset -> {
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
                        LOG.error("Aikakatkaisu ehti ensin. Palvelu on todennäköisesti kovan kuormanalla. " + t.getMessage(), t);
                    }
                }, poikkeusKasittelija("Seurantapalveluun ei saatu yhteyttä", asyncResponse, dokumenttiIdRef));
    }

    private PoikkeusKasittelijaSovitin poikkeusKasittelija(String viesti, AsyncResponse asyncResponse, AtomicReference<String> dokumenttiIdRef) {
        return new PoikkeusKasittelijaSovitin(poikkeus -> {
            if (poikkeus == null) {
                LOG.error("###Poikkeus tuonnissa {}\r\n###", viesti);
            } else {
                LOG.error("###Poikkeus tuonnissa :" + viesti + " " + poikkeus.getMessage() + "\r\n###", poikkeus);
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
                                LOG.error("Virheen ilmoittamisessa virhe! " + dontcare.getMessage(), dontcare);
                            });
                }
            } catch (Throwable t) {
                LOG.error("Odottamaton virhe: " + t.getMessage(), t);
            }
        });
    }
}

