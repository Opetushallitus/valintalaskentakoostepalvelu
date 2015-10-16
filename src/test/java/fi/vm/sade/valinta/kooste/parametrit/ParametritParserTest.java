package fi.vm.sade.valinta.kooste.parametrit;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

public class ParametritParserTest {

    private final String ROOTOID = "1.2.246.562.10.00000000001";

    @Test
    public void testValintapalvelunKayttoEnabled() throws Exception {
        this.setOphUser();

        ParametritDTO parametritDTO = new ParametritDTO();
        ParametriDTO olvvpke = new ParametriDTO();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 100);
        olvvpke.setDateStart(cal.getTime());
        parametritDTO.setPH_OLVVPKE(olvvpke);
        ParametritParser parametritParser = new ParametritParser(parametritDTO, null, ROOTOID);

        assertTrue(parametritParser.valintapalvelunKayttoEnabled());

        this.setNormalUser();
        assertTrue(parametritParser.valintapalvelunKayttoEnabled());

        cal.add(Calendar.SECOND, -200);
        olvvpke.setDateStart(cal.getTime());
        parametritParser = new ParametritParser(parametritDTO, null, ROOTOID);
        assertFalse(parametritParser.valintapalvelunKayttoEnabled());

        cal.add(Calendar.SECOND, 50);
        olvvpke.setDateEnd(cal.getTime());
        parametritParser = new ParametritParser(parametritDTO, null, ROOTOID);
        assertTrue(parametritParser.valintapalvelunKayttoEnabled());

        cal.add(Calendar.SECOND, 100);
        olvvpke.setDateEnd(cal.getTime());
        parametritParser = new ParametritParser(parametritDTO, null, ROOTOID);
        assertFalse(parametritParser.valintapalvelunKayttoEnabled());

        olvvpke.setDateStart(null);
        parametritParser = new ParametritParser(parametritDTO, null, ROOTOID);
        assertFalse(parametritParser.valintapalvelunKayttoEnabled());

        olvvpke.setDateEnd(null);
        parametritParser = new ParametritParser(parametritDTO, null, ROOTOID);
        assertTrue(parametritParser.valintapalvelunKayttoEnabled());
    }

    @Test
    public void testKoetulostenTallentaminenEnabled() {
        this.setNormalUser();

        ParametritDTO parametritDTO = new ParametritDTO();
        ParametriDTO ph_ktt = new ParametriDTO();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -100);
        ph_ktt.setDateStart(cal.getTime());
        cal.add(Calendar.SECOND, 200);
        ph_ktt.setDateEnd(cal.getTime());
        parametritDTO.setPH_KTT(ph_ktt);
        ParametritParser parametritParser = new ParametritParser(parametritDTO, null, ROOTOID);
        assertTrue(parametritParser.koetulostenTallentaminenEnabled());

        ph_ktt.setDateStart(cal.getTime());
        parametritParser = new ParametritParser(parametritDTO, null, ROOTOID);
        assertFalse(parametritParser.koetulostenTallentaminenEnabled());

        ph_ktt.setDateStart(null);
        parametritParser = new ParametritParser(parametritDTO, null, ROOTOID);
        assertTrue(parametritParser.koetulostenTallentaminenEnabled());
    }


    private void setOphUser() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("test", "test", Arrays.asList(new SimpleGrantedAuthority("ROLE_" + ROOTOID)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setNormalUser() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("test", "test");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
