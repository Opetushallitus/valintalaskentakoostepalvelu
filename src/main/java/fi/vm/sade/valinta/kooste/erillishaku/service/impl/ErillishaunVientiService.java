package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuExcel;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
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

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static rx.Observable.from;
import static rx.Observable.zip;

/**
 * @author Jussi Jartamo
 */
@Service
public class ErillishaunVientiService {
    private static final Logger LOG = LoggerFactory.getLogger(ErillishaunVientiService.class);
    private final SijoitteluAsyncResource sijoitteluAsyncResource;
    private final TarjontaAsyncResource hakuV1AsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final DokumenttiResource dokumenttiResource;

    @Autowired
    public ErillishaunVientiService(ApplicationAsyncResource applicationAsyncResource, SijoitteluAsyncResource sijoitteluAsyncResource, TarjontaAsyncResource hakuV1AsyncResource, DokumenttiResource dokumenttiResource) {
        this.sijoitteluAsyncResource = sijoitteluAsyncResource;
        this.hakuV1AsyncResource = hakuV1AsyncResource;
        this.applicationAsyncResource = applicationAsyncResource;
        this.dokumenttiResource = dokumenttiResource;
    }

    private String teksti(final Map<String, String> nimi) {
        return new Teksti(nimi).getTeksti();
    }

    public void vie(KirjeProsessi prosessi, ErillishakuDTO erillishaku) {
        Future<List<Hakemus>> hakemusFuture = applicationAsyncResource.getApplicationsByOid(erillishaku.getHakuOid(), erillishaku.getHakukohdeOid());
        Future<HakukohdeDTO> hakukohdeFuture = sijoitteluAsyncResource.getLatestHakukohdeBySijoittelu(erillishaku.getHakuOid(), erillishaku.getHakukohdeOid());
        Future<List<Valintatulos>> valintatulosFuture = sijoitteluAsyncResource.getValintatuloksetHakukohteelle(erillishaku.getHakukohdeOid(), erillishaku.getValintatapajonoOid());
        Future<HakuV1RDTO> hakuFuture = hakuV1AsyncResource.haeHaku(erillishaku.getHakuOid());
        Future<fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO> tarjontaHakukohdeFuture = hakuV1AsyncResource.haeHakukohde(erillishaku.getHakukohdeOid());

        zip(from(hakemusFuture), from(hakuFuture), from(tarjontaHakukohdeFuture), from(valintatulosFuture), from(hakukohdeFuture),
                (hakemukset, haku, tarjontaHakukohde, valintatulos, hakukohde) -> {
                    Map<String, Valintatulos> valintatulokset = getValintatulokset(erillishaku, valintatulos);
                    if (MapUtils.isEmpty(valintatulokset)) {
                        // ei viela tuloksia, joten tehdaan tuonti haetuista hakemuksista
                        return generoiIlmanTuloksia(erillishaku, hakemukset, haku, tarjontaHakukohde);
                    } else {
                        return generoiTuloksilla(erillishaku, hakemukset, haku, tarjontaHakukohde, hakukohde, valintatulokset);
                    }

            })
            .subscribeOn(Schedulers.newThread())
            .subscribe(
                excel -> {
                    LOG.info("Aloitetaan dokumenttipalveluun tallennus");
                    String uuid = UUID.randomUUID().toString();
                    dokumenttiResource.tallenna(uuid, "erillishaku.xlsx", DateTime.now().plusHours(1).toDate().getTime(),
                            Arrays.asList("erillishaku"), "application/octet-stream", excel.getExcel().vieXlsx());
                    prosessi.vaiheValmistui();
                    prosessi.valmistui(uuid);
                },
                poikkeus -> {
                    LOG.error("Erillishaun vienti keskeytyi virheeseen", poikkeus);
                    prosessi.keskeyta();
                },
                () -> {
                    LOG.info("Erillishaun vienti onnistui!");
                    prosessi.keskeyta();
                });
    }
    private String objectToString(Object o) {
        if(o == null) {
            return "";
        } else {
            return o.toString();
        }
    }
    private ErillishakuExcel generoiTuloksilla(final ErillishakuDTO erillishaku, final List<Hakemus> hakemukset, final HakuV1RDTO haku, final fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO tarjontaHakukohde, final HakukohdeDTO hakukohde, final Map<String, Valintatulos> valintatulokset) {
        LOG.info("Muodostetaan Excel valintaperusteista!");
        Map<String, Hakemus> oidToHakemus = hakemukset.stream().collect(Collectors.toMap(h -> h.getOid(), h -> h));
        List<ErillishakuRivi> erillishakurivit = hakukohde
                .getValintatapajonot().stream()
                .flatMap(v -> v.getHakemukset().stream())
                .map(h -> {
                    HakemusWrapper wrapper = new HakemusWrapper(oidToHakemus.get(h.getHakemusOid()));
                    Valintatulos tulos = Optional.ofNullable(valintatulokset.get(h.getHakemusOid())).orElse(new Valintatulos());
                    ErillishakuRivi e = new ErillishakuRivi(
                            h.getHakemusOid(),
                            h.getSukunimi(),
                            h.getEtunimi(),
                            wrapper.getHenkilotunnus(),
                            wrapper.getSahkopostiOsoite(),
                            wrapper.getSyntymaaika(),
                            wrapper.getPersonOid(),
                            objectToString(h.getTila()),
                            objectToString(tulos.getTila()),
                            objectToString(tulos.getIlmoittautumisTila()),
                            tulos.getJulkaistavissa(),
                            false);
                    return e;
                }).collect(Collectors.toList());
        return new ErillishakuExcel(erillishaku.getHakutyyppi(), teksti(haku.getNimi()), teksti(tarjontaHakukohde.getHakukohdeNimi()), teksti(tarjontaHakukohde.getTarjoajaNimi()), erillishakurivit);
    }

    private ErillishakuExcel generoiIlmanTuloksia(final ErillishakuDTO erillishaku, final List<Hakemus> hakemukset, final HakuV1RDTO haku, final fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO tarjontaHakukohde) {
        LOG.info("Hakemuksia ei ole viela tuotu ensimmaistakaan kertaa talle hakukohteelle! Generoidaan hakemuksista excel...");
        List<ErillishakuRivi> rivit = hakemukset.stream().map(hakemus -> {
            HakemusWrapper wrapper = new HakemusWrapper(hakemus);
            ErillishakuRivi r = new ErillishakuRivi(hakemus.getOid(), wrapper.getSukunimi(),
                    wrapper.getEtunimi(), wrapper.getHenkilotunnus(), wrapper.getSahkopostiOsoite(), wrapper.getSyntymaaika(), wrapper.getPersonOid(), "HYLATTY", "", "", false, false);
            return r;
        }).collect(Collectors.toList());
        return new ErillishakuExcel(erillishaku.getHakutyyppi(), teksti(haku.getNimi()), teksti(tarjontaHakukohde.getHakukohdeNimi()), teksti(tarjontaHakukohde.getTarjoajaNimi()), rivit);
    }

    private Map<String, Valintatulos> getValintatulokset(final ErillishakuDTO erillishaku, final List<Valintatulos> valintatulos) {
        try {
            return valintatulos.stream()
                    .filter(v -> erillishaku.getValintatapajonoOid().equals(v.getValintatapajonoOid()))
                    .collect(Collectors.toMap(v -> v.getHakemusOid(), v -> v));
        } catch (Exception e1) {
            LOG.error("Sijoittelusta ei saatu tietoja hakukohteelle vientia varten!"
                            + "Resurssi /sijoittelu/{}/sijoitteluajo/{}/hakukohde/{} "
                            + "{}: {}", erillishaku.getHakuOid(),
                    erillishaku.getHakukohdeOid(), SijoitteluResource.LATEST, e1.getMessage(), Arrays.toString(e1.getStackTrace()));
            throw new RuntimeException(e1);
        }
    }
}
