package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.util.HakemusUtil;
import fi.vm.sade.valinta.kooste.util.KieliUtil;

import java.util.Optional;

public class Sijoitus {
    private String nimi;
    private String tila;
    private String tilanKuvaus;
    private String hyvaksymisenEhto;
    private String oma;
    private String varasija;
    private Pisteet pisteet;

    public Sijoitus(HakutoiveenValintatapajonoDTO valintatapajono,
                    String varasijaTeksti,
                    Pisteet pisteet,
                    String preferoituKielikoodi) {
        this.nimi = valintatapajono.getValintatapajonoNimi();
        this.tila = HakemusUtil.tilaConverter(valintatapajono, preferoituKielikoodi);
        this.tilanKuvaus = valintatapajono.getTilanKuvaukset().get(preferoituKielikoodi);
        if (valintatapajono.getTila().isHyvaksytty() && valintatapajono.isEhdollisestiHyvaksyttavissa()) {
            switch (preferoituKielikoodi) {
                case KieliUtil.SUOMI:
                    this.hyvaksymisenEhto = valintatapajono.getEhdollisenHyvaksymisenEhtoFI();
                    break;
                case KieliUtil.RUOTSI:
                    this.hyvaksymisenEhto = valintatapajono.getEhdollisenHyvaksymisenEhtoSV();
                    break;
                case KieliUtil.ENGLANTI:
                    this.hyvaksymisenEhto = valintatapajono.getEhdollisenHyvaksymisenEhtoEN();
                    break;
            }
        }
        this.oma = Optional.ofNullable(valintatapajono.getHyvaksytty()).map(Object::toString).orElse(null);
        this.varasija = varasijaTeksti;
        this.pisteet = pisteet;
    }

    public Pisteet getPisteet() {
        return pisteet;
    }

    public String getVarasija() {
        return varasija;
    }

    public String getNimi() {
        return nimi;
    }

    public String getOma() {
        return oma;
    }

    public String getTila() {
        return tila;
    }

    public String getTilanKuvaus() {
        return tilanKuvaus;
    }

    public String getHyvaksymisenEhto() {
        return hyvaksymisenEhto;
    }
}
