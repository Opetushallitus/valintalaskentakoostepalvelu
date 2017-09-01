package fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto;

import org.junit.Test;

import static org.junit.Assert.*;

public class HenkiloCreateDTOTest {
    @Test
    public void equals() throws Exception {
        String aidinkieli = "fi";
        String sukupuoli = "MIES";
        String etunimet = "Matti";
        String sukunimi = "Meikäläinen";
        String hetu = "123456-789A";
        String syntymaAika = "11.11.2011";
        String henkiloOid = "henkilo1";
        HenkiloTyyppi henkiloTyyppi = HenkiloTyyppi.OPPIJA;
        String asiointikieli = "fi";
        String kansalaisuus = "FIN";

        HenkiloCreateDTO dto1 = new HenkiloCreateDTO(
                aidinkieli,
                sukupuoli,
                etunimet,
                sukunimi,
                hetu,
                syntymaAika,
                henkiloOid,
                henkiloTyyppi,
                asiointikieli,
                kansalaisuus);
        HenkiloCreateDTO dto2 = new HenkiloCreateDTO(
                aidinkieli,
                sukupuoli,
                etunimet,
                sukunimi,
                hetu,
                syntymaAika,
                henkiloOid,
                henkiloTyyppi,
                asiointikieli,
                kansalaisuus);

        assertTrue(dto1.equals(dto2));
    }

}