package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto;

import scala.Option;

import java.util.Date;
import java.util.Map;

public class IlmoittautumistilaDto {
    private String hakukohdeOid;
    private String tarjoajaOid;
    private String valintatila;
    private scala.Option<String> vastaanottotila;
    private scala.Option<IlmoittautumistilaDto> hakuToiveenIlmoittautumisTilaDto;
    private String vastaanotettavuustila;
    private scala.Option<Date> vastaanottoDeadline;
    private scala.Option<Integer> jonosija;
    private scala.Option<Date> varasijojaKaytetaanAlkaen;
    private scala.Option<Date> varasijojaTaytetaanAsti;
    private scala.Option<Integer> varasijanumero;

    public IlmoittautumistilaDto(String hakukohdeOid, String tarjoajaOid,
                                 String valintatila, Option<String> vastaanottotila,
                                 Option<IlmoittautumistilaDto> hakuToiveenIlmoittautumisTilaDto,
                                 String vastaanotettavuustila, Option<Date> vastaanottoDeadline,
                                 Option<Integer> jonosija, Option<Date> varasijojaKaytetaanAlkaen,
                                 Option<Date> varasijojaTaytetaanAsti,
                                 Option<Integer> varasijanumero,
                                 Map<String, String> tilanKuvaukset) {
        this.hakukohdeOid = hakukohdeOid;
        this.tarjoajaOid = tarjoajaOid;
        this.valintatila = valintatila;
        this.vastaanottotila = vastaanottotila;
        this.hakuToiveenIlmoittautumisTilaDto = hakuToiveenIlmoittautumisTilaDto;
        this.vastaanotettavuustila = vastaanotettavuustila;
        this.vastaanottoDeadline = vastaanottoDeadline;
        this.jonosija = jonosija;
        this.varasijojaKaytetaanAlkaen = varasijojaKaytetaanAlkaen;
        this.varasijojaTaytetaanAsti = varasijojaTaytetaanAsti;
        this.varasijanumero = varasijanumero;
        this.tilanKuvaukset = tilanKuvaukset;
    }

    private Map<String, String> tilanKuvaukset;

}
