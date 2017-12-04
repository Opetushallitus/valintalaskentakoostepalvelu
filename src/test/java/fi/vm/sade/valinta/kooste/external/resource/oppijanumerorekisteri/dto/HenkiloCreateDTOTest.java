package fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto;

import org.junit.Test;

import static org.junit.Assert.*;

public class HenkiloCreateDTOTest {
    @Test
    public void equals() throws Exception {
        HenkiloCreateDTO dto1 = getHenkiloWithFirstname("Matti");
        HenkiloCreateDTO dto2 = getHenkiloWithFirstname("Matti");

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void multipleFirstNames() {
        HenkiloCreateDTO dto = getHenkiloWithFirstname("Henrikki Aapeli Testi");
        assertEquals("Kutsumanimi should be the first one of first names","Henrikki", dto.kutsumanimi);
    }

    @Test
    public void multipleFirstNamesWithHyphen(){
        HenkiloCreateDTO dto = getHenkiloWithFirstname("Henrikki-Aapeli Testi");
        assertEquals("Kutsumanimi should be the first one of first names","Henrikki-Aapeli", dto.kutsumanimi);
    }

    @Test
    public void multipleFirstNamesWithHyphenTrimming() {
        //testaa konstruktorissa tapahtuvaa nimen trimmausta, huomioi välilyönti edessä
        HenkiloCreateDTO dto = getHenkiloWithFirstname(" Henrikki-Aapeli Testi");
        assertEquals("Kutsumanimi should be the first one of first names","Henrikki-Aapeli", dto.kutsumanimi);

        dto = getHenkiloWithFirstname(" Henrikki-Aapeli Testi  ");
        assertEquals("Kutsumanimi should be the first one of first names","Henrikki-Aapeli", dto.kutsumanimi);
    }

    private HenkiloCreateDTO getHenkiloWithFirstname(String firstname) {
        String aidinkieli = "fi";
        String sukupuoli = "MIES";
        String sukunimi = "Hakija";
        String hetu = "123456-789A";
        String syntymaAika = "11.11.2011";
        String henkiloOid = "henkilo1";
        HenkiloTyyppi henkiloTyyppi = HenkiloTyyppi.OPPIJA;
        String asiointikieli = "fi";
        String kansalaisuus = "FIN";

        return new HenkiloCreateDTO(
                aidinkieli,
                sukupuoli,
                firstname,
                sukunimi,
                hetu,
                syntymaAika,
                henkiloOid,
                henkiloTyyppi,
                asiointikieli,
                kansalaisuus);
    }

}