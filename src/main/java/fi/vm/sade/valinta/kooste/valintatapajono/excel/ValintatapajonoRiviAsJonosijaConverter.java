package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.JarjestyskriteeritulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.valinta.JarjestyskriteerituloksenTila;

public class ValintatapajonoRiviAsJonosijaConverter {

    public static JonosijaDTO convert(String hakukohdeOid, ValintatapajonoRivi rivi, Hakemus hakemus) {
        JonosijaDTO j = new JonosijaDTO();
        HakemusWrapper h = new HakemusWrapper(hakemus);
        j.setEtunimi(h.getEtunimi());
        j.setSukunimi(h.getSukunimi());
        j.setHakemusOid(hakemus.getOid());
        j.setHakijaOid(hakemus.getPersonOid());
        j.setJonosija(rivi.asJonosija());
        j.setMuokattu(false);
        j.setHarkinnanvarainen(false);
        Integer prioriteetti = h.getHakutoiveenPrioriteetti(hakukohdeOid);
        if (prioriteetti == null) {
            throw new RuntimeException("Hakemuspalvelu palautti hakemuksen hakukohteelle vaikka hakija ei ole siihen hakenut!");
        }
        j.setPrioriteetti(prioriteetti);
        JarjestyskriteeritulosDTO kriteeri = new JarjestyskriteeritulosDTO();
        kriteeri.setArvo(new BigDecimal(rivi.asJonosija()).negate());
        kriteeri.setNimi(StringUtils.EMPTY);
        kriteeri.setTila(rivi.asTila());
        kriteeri.setKuvaus(rivi.getKuvaus());
        kriteeri.setPrioriteetti(0);
        j.getJarjestyskriteerit().add(kriteeri);
        j.setTuloksenTila(JarjestyskriteerituloksenTila.HYVAKSYTTAVISSA);
        return j;
    }
}
