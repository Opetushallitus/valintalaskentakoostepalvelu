package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuExcel;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.service.ErillishaunVientiService;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import org.apache.commons.collections.MapUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static rx.Observable.from;
import static rx.Observable.zip;

/**
 * @author Jussi Jartamo
 */
@Service
public class ErillishaunVientiServiceImpl implements ErillishaunVientiService {

    private static final Logger LOG = LoggerFactory.getLogger(ErillishaunVientiServiceImpl.class);
    private final SijoitteluAsyncResource sijoitteluAsyncResource;
    private final TarjontaAsyncResource hakuV1AsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final DokumenttiResource dokumenttiResource;

    @Autowired
    public ErillishaunVientiServiceImpl(
            ApplicationAsyncResource applicationAsyncResource,
            SijoitteluAsyncResource sijoitteluAsyncResource,
            TarjontaAsyncResource hakuV1AsyncResource,
            DokumenttiResource dokumenttiResource) {
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.hakuV1AsyncResource = hakuV1AsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.dokumenttiResource = dokumenttiResource;
    }

    @Override
    public void vie(KirjeProsessi prosessi, ErillishakuDTO erillishaku) {
        Future<List<Hakemus>> hakemusFuture = applicationAsyncResource
                .getApplicationsByOid(erillishaku.getHakuOid(), erillishaku.getHakukohdeOid());

        Future<HakukohdeDTO> hakukohdeFuture = sijoitteluAsyncResource
                .getLatestHakukohdeBySijoittelu(erillishaku.getHakuOid(), erillishaku.getHakukohdeOid());

        Future<List<Valintatulos>> valintatulosFuture = sijoitteluAsyncResource
                .getValintatuloksetHakukohteelle(erillishaku.getHakukohdeOid(), erillishaku.getValintatapajonoOid());

        Future<HakuV1RDTO> hakuFuture = hakuV1AsyncResource.haeHaku(erillishaku.getHakuOid());

        Future<fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO> tarjontaHakukohdeFuture = hakuV1AsyncResource
                .haeHakukohde(erillishaku.getHakukohdeOid());

        zip(
                from(hakemusFuture),
                from(hakuFuture),
                from(tarjontaHakukohdeFuture),
                (hakemukset, haku, tarjontaHakukohde) -> {
                    String hakuNimi = new Teksti(haku.getNimi()).getTeksti();
                    String hakukohdeNimi = new Teksti(tarjontaHakukohde
                            .getHakukohdeNimi()).getTeksti();
                    String tarjoajaNimi = new Teksti(tarjontaHakukohde
                            .getTarjoajaNimi()).getTeksti();
                    Map<String, Hakemus> oidToHakemus = hakemukset.stream().collect(Collectors.toMap(
                            h -> h.getOid(), h -> h
                    ));
                    Map<String, Valintatulos> valintatulokset;
                    HakukohdeDTO hakukohde;
                    try {
                        valintatulokset = valintatulosFuture.get().stream()
                                .filter(v -> erillishaku.getValintatapajonoOid().equals(v.getValintatapajonoOid()))
                                .collect(Collectors.toMap(
                                        v -> v.getHakemusOid(), v -> v));
                        hakukohde = hakukohdeFuture.get();
                    } catch (Exception e1) {
                        LOG.error("Sijoittelusta ei saatu tietoja hakukohteelle vientia varten!"
                                        + "Resurssi /sijoittelu/{}/sijoitteluajo/{}/hakukohde/{} "
                                        + "{}: {}", erillishaku.getHakuOid(),
                                erillishaku.getHakukohdeOid(), SijoitteluResource.LATEST, e1.getMessage(), Arrays.toString(e1.getStackTrace()));
                        throw new RuntimeException(e1);
                    }

                    if (MapUtils.isEmpty(valintatulokset)) {
                        // ei viela tuloksia, joten tehdaan tuonti haetuista hakemuksista
                        LOG.error("Hakemuksia ei ole viela tuotu ensimmaistakaan kertaa talle hakukohteelle! Generoidaan hakemuksista excel...");
                        List<ErillishakuRivi> rivit = hakemukset.stream().map(hakemus -> {
                            HakemusWrapper wrapper = new HakemusWrapper(hakemus);
                            ErillishakuRivi r = new ErillishakuRivi(wrapper.getSukunimi(),
                                    wrapper.getEtunimi(), wrapper.getHenkilotunnus(), wrapper.getSyntymaaika(), "HYLATTY", "", "", false);
                            return r;
                        }).collect(Collectors.toList());
                        return new ErillishakuExcel(erillishaku.getHakutyyppi(), hakuNimi, hakukohdeNimi,
                                tarjoajaNimi, rivit);
                    } else {
                        LOG.error("Muodostetaan Excel valintaperusteista!");
                        List<ErillishakuRivi> erillishakurivit = hakukohde
                                .getValintatapajonot().stream()
                                .flatMap(v -> v.getHakemukset().stream())
                                .map(h -> {
                                    HakemusWrapper h0 = new HakemusWrapper(oidToHakemus.get(h.getHakemusOid()));
                                    String hakemuksenTila = "";
                                    if (h.getTila() != null) {
                                        hakemuksenTila = h.getTila().toString();
                                    }
                                    Valintatulos tulos = valintatulokset.get(h.getHakemusOid());
                                    ErillishakuRivi e = new ErillishakuRivi(h
                                            .getSukunimi(), h.getEtunimi(), h0.getHenkilotunnus(), h0.getSyntymaaika(),
                                            hakemuksenTila, tulos.getTila().toString(), tulos.getIlmoittautumisTila().toString(),
                                            tulos.getJulkaistavissa());
                                    return e;
                                }).collect(Collectors.toList());

                        return new ErillishakuExcel(erillishaku.getHakutyyppi(), hakuNimi, hakukohdeNimi,
                                tarjoajaNimi, erillishakurivit);
                    }

                })
                .subscribeOn(Schedulers.newThread())
                .subscribe(e -> {
                            LOG.warn("Aloitetaan dokumenttipalveluun tallennus");
                            String uuid = UUID.randomUUID().toString();
                            dokumenttiResource.tallenna(uuid, "erillishaku.xlsx", DateTime.now().plusHours(1).toDate().getTime(),
                                    Arrays.asList("erillishaku"), "application/octet-stream", e.getExcel().vieXlsx());
                            prosessi.vaiheValmistui();
                            prosessi.valmistui(uuid);
                        },
                        poikkeus -> {
                            LOG.error("Erillishaun vienti keskeytyi virheeseen {}. {}", poikkeus.getMessage(), Arrays.toString(poikkeus.getStackTrace()));
                            prosessi.keskeyta();
                        },
                        () -> {
                            LOG.warn("Erillishaun vienti onnistui!");
                            prosessi.keskeyta();
                        });
    }
}
