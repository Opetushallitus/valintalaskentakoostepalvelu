package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.JarjestyskriteeritulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.valinta.JarjestyskriteerituloksenTila;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;

public class ValintatapajonoRiviAsJonosijaConverter {

    public static JonosijaDTO convert(String hakukohdeOid, ValintatapajonoRivi rivi, HakemusWrapper hakemus) {
        JonosijaDTO j = new JonosijaDTO();
        j.setEtunimi(hakemus.getEtunimi());
        j.setSukunimi(hakemus.getSukunimi());
        j.setHakemusOid(hakemus.getOid());
        j.setHakijaOid(hakemus.getPersonOid());
        j.setJonosija(rivi.asJonosija());
        j.setMuokattu(false);
        j.setHarkinnanvarainen(false);
        Integer prioriteetti = hakemus.getHakutoiveenPrioriteetti(hakukohdeOid);
        if (prioriteetti == null) {
            throw new RuntimeException(String.format(
                    "Hakemuspalvelu palautti hakemuksen %s hakukohteelle %s vaikka hakija ei ole siihen hakenut!",
                    hakemus.getOid(),
                    hakukohdeOid
            ));
        }
        j.setPrioriteetti(prioriteetti);
        JarjestyskriteeritulosDTO kriteeri = new JarjestyskriteeritulosDTO();
        if(StringUtils.isNotBlank(rivi.getPisteet())) {
            kriteeri.setArvo(new BigDecimal(rivi.getPisteet()));
        } else {
            kriteeri.setArvo(new BigDecimal(rivi.asJonosija()).negate());
        }
        kriteeri.setTila(rivi.isMaarittelematon() ? JarjestyskriteerituloksenTila.HYVAKSYTTAVISSA : rivi.asTila());
        kriteeri.setNimi(StringUtils.EMPTY);
        kriteeri.setKuvaus(rivi.getKuvaus());
        kriteeri.setPrioriteetti(0);
        j.getJarjestyskriteerit().add(kriteeri);
        j.setTuloksenTila(JarjestyskriteerituloksenTila.HYVAKSYTTAVISSA);
        return j;
    }
}
