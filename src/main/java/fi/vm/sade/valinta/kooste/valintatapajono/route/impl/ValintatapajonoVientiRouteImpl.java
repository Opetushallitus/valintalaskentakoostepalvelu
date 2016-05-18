package fi.vm.sade.valinta.kooste.valintatapajono.route.impl;

import java.io.InputStream;
import java.util.List;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.HakukohdeResource;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakuTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoExcel;
import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoVientiRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;

@Component
public class ValintatapajonoVientiRouteImpl extends AbstractDokumenttiRouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ValintatapajonoVientiRouteImpl.class);

    private final ApplicationResource applicationResource;
    private final DokumenttiResource dokumenttiResource;
    private final HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeTarjonnalta;
    private final HaeHakuTarjonnaltaKomponentti hakuTarjonnalta;
    private final HakukohdeResource hakukohdeResource;

    @Autowired
    public ValintatapajonoVientiRouteImpl(
            ApplicationResource applicationResource,
            DokumenttiResource dokumenttiResource,
            HaeHakukohdeNimiTarjonnaltaKomponentti hakukohdeTarjonnalta,
            HaeHakuTarjonnaltaKomponentti hakuTarjonnalta,
            HakukohdeResource hakukohdeResource) {
        this.applicationResource = applicationResource;
        this.dokumenttiResource = dokumenttiResource;
        this.hakukohdeTarjonnalta = hakukohdeTarjonnalta;
        this.hakuTarjonnalta = hakuTarjonnalta;
        this.hakukohdeResource = hakukohdeResource;
    }

    @Override
    public void configure() throws Exception {
        Endpoint valintatapajonoVienti = endpoint(ValintatapajonoVientiRoute.SEDA_VALINTATAPAJONO_VIENTI);
        Endpoint luontiEpaonnistui = endpoint("direct:valintatapajono_vienti_deadletterchannel");
        from(valintatapajonoVienti)
                .errorHandler(
                        deadLetterChannel(luontiEpaonnistui)
                                .maximumRedeliveries(0)
                                .logExhaustedMessageHistory(true)
                                .logExhausted(true).logStackTrace(true)
                                // hide retry/handled stacktrace
                                .logRetryStackTrace(false).logHandled(false))
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        dokumenttiprosessi(exchange).setKokonaistyo(
                                // haun nimi ja hakukohteen nimi
                                1 + 1 +
                                        // osallistumistiedot + valintaperusteet +
                                        // hakemuspistetiedot
                                        1 + 1
                                        // luonti
                                        + 1
                                        // dokumenttipalveluun vienti
                                        + 1);
                        String hakuOid = hakuOid(exchange);
                        String hakukohdeOid = hakukohdeOid(exchange);
                        String hakuNimi = new Teksti(hakuTarjonnalta.getHaku(hakuOid).getNimi()).getTeksti();
                        dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
                        HakukohdeDTO hnimi = hakukohdeTarjonnalta.haeHakukohdeNimi(hakukohdeOid);
                        dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
                        String hakukohdeNimi = new Teksti(hnimi.getHakukohdeNimi()).getTeksti();
                        String valintatapajonoOid = valintatapajonoOid(exchange);
                        if (hakukohdeOid == null || hakuOid == null || valintatapajonoOid == null) {
                            LOG.error("Pakolliset tiedot reitille puuttuu hakuOid = {}, hakukohdeOid = {}, valintatapajonoOid = {}", hakuOid, hakukohdeOid, valintatapajonoOid);
                            dokumenttiprosessi(exchange).getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Puutteelliset lähtötiedot"));
                            throw new RuntimeException("Pakolliset tiedot reitille puuttuu hakuOid, hakukohdeOid, valintatapajonoOid");
                        }
                        final List<Hakemus> hakemukset;
                        try {
                            hakemukset = applicationResource.getApplicationsByOid(hakuOid, hakukohdeOid,
                                    ApplicationResource.ACTIVE_AND_INCOMPLETE, ApplicationResource.MAX);
                            LOG.debug("Saatiin hakemukset {}", hakemukset.size());
                            dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
                        } catch (Exception e) {
                            LOG.error("Hakemuspalvelun virhe", e);
                            dokumenttiprosessi(exchange).getPoikkeukset().add(new Poikkeus(Poikkeus.HAKU, "Hakemuspalvelulta ei saatu hakemuksia hakukohteelle", ""));
                            throw e;
                        }
                        if (hakemukset.isEmpty()) {
                            LOG.error("Nolla hakemusta!");
                            dokumenttiprosessi(exchange).getPoikkeukset().add(new Poikkeus(Poikkeus.HAKU, "Hakukohteella ei ole hakemuksia!", ""));
                            throw new RuntimeException("Hakukohteelle saatiin tyhjä hakemusjoukko!");
                        }
                        final List<ValintatietoValinnanvaiheDTO> valinnanvaiheet;
                        try {
                            valinnanvaiheet = hakukohdeResource.hakukohde(hakukohdeOid);
                            dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
                        } catch (Exception e) {
                            LOG.error("Valinnanvaiheiden haku virhe", e);
                            dokumenttiprosessi(exchange).getPoikkeukset().add(new Poikkeus(Poikkeus.VALINTALASKENTA, "Valintalaskennalta ei saatu valinnanvaiheita", ""));
                            throw e;
                        }
                        InputStream xlsx;
                        try {
                            ValintatapajonoExcel valintatapajonoExcel = new ValintatapajonoExcel(hakuOid, hakukohdeOid, valintatapajonoOid,
                                    hakuNimi, hakukohdeNimi, valinnanvaiheet, hakemukset);
                            xlsx = valintatapajonoExcel.getExcel().vieXlsx();
                            dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
                        } catch (Exception e) {
                            LOG.error("Valintatapajono excelin luonti virhe", e);
                            dokumenttiprosessi(exchange).getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Valintatapajono exceliä ei saatu luotua!", ""));
                            throw e;
                        }
                        try {
                            String id = generateId();
                            Long expirationTime = defaultExpirationDate().getTime();
                            List<String> tags = dokumenttiprosessi(exchange).getTags();
                            dokumenttiResource.tallenna(id, "valintatapajono.xlsx", expirationTime, tags, "application/octet-stream", xlsx);
                            dokumenttiprosessi(exchange).setDokumenttiId(id);
                            dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
                        } catch (Exception e) {
                            LOG.error("Dokumenttipalveluun vienti virhe", e);
                            dokumenttiprosessi(exchange).getPoikkeukset().add(new Poikkeus(Poikkeus.DOKUMENTTIPALVELU, "Dokumenttipalveluun ei saatu vietyä taulukkolaskentatiedostoa!", ""));
                            throw e;
                        }
                    }
                })
                .stop();
        /**
         * DEAD LETTER CHANNEL
         */
        from(luontiEpaonnistui)
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        String syy;
                        if (exchange.getException() == null) {
                            syy = "Valintatapajonon taulukkolaskentaan vienti epäonnistui. Ota yheys ylläpitoon.";
                        } else {
                            syy = exchange.getException().getMessage();
                        }
                        dokumenttiprosessi(exchange).getPoikkeukset().add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Valintatapajonon vienti", syy));
                    }
                })
                .stop();
    }
}
