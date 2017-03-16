package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHakijaRivi;

import java.util.Optional;

@Component
public class KelaHakijaRiviKomponenttiImpl {

    private static final Integer KESAKUU = 6;

    public TKUVAYHVA luo(KelaHakijaRivi hakija) {
        TKUVAYHVA.Builder builder = new TKUVAYHVA.Builder();
        builder.setSiirtotunnus(hakija.getSiirtotunnus());
        builder.setTutkinnontaso1(hakija.getTasoLaajuus().getTasoCode());
        builder.setOppilaitosnumero(hakija.getOppilaitosnumero());
        builder.setOrganisaatio(hakija.getOrganisaatio());
        builder.setHakukohde(hakija.getHakukohde());
        builder.setLukuvuosi(hakija.getLukuvuosi());
        builder.setValintapaivamaara(hakija.getValintapaivamaara());
        builder.setSukunimi(hakija.getSukunimi());
        builder.setEtunimet(hakija.getEtunimi());
        if (hakija.hasHenkilotunnus()) {
            builder.setHenkilotunnus(hakija.getHenkilotunnus());
        } else { // Ulkomaalaisille syntyma-aika hetun
            // sijaan
            // // esim
            // 04.05.1965
            // Poistetaan pisteet ja tyhjaa loppuun
            builder.setHenkilotunnus(hakija.getSyntymaaika().replace(".", ""));
        }
        builder.setPoimintapaivamaara(hakija.getPoimintapaivamaara());
        DateTime dateTime = new DateTime(hakija.getLukuvuosi());
        if (dateTime.getMonthOfYear() > KESAKUU) { // myohemmin
            // kuin
            // kesakuussa!
            builder.setSyksyllaAlkavaKoulutus();
        } else {
            builder.setKevaallaAlkavaKoulutus();
        }
        builder.setTutkinnonLaajuus1(hakija.getTasoLaajuus().getLaajuus1());
        builder.setTutkinnonLaajuus2(hakija.getTasoLaajuus().getLaajuus2());

        return builder.build();
    }

}

