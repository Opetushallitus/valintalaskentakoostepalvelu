package fi.vm.sade.valinta.kooste.parametrit.service.impl;

import fi.vm.sade.valinta.kooste.parametrit.Parametrit;
import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: tommiha
 * Date: 9/16/13
 * Time: 3:41 PM
 */
public class ParametriServiceImplTest {

    private Parametrit parametrit;

    private ParametriService parametriService;

    private final String ROOTOID = "1.2.246.562.10.00000000001";

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Before
    public void setUp() {
        parametrit = new Parametrit();
        parametriService = new ParametriServiceImpl();
        ReflectionTestUtils.setField(parametriService, "parametrit", parametrit);
        ReflectionTestUtils.setField(parametriService, "rootOrganisaatioOid", ROOTOID);
    }

    @Test
    public void testPistesyottoEnabled() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken("test", "test", Arrays.asList(new SimpleGrantedAuthority("ROLE_" + ROOTOID)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Calendar calendar = Calendar.getInstance();


        boolean visible = parametriService.pistesyottoEnabled("oid");
        assertTrue(visible);

        authentication = new UsernamePasswordAuthenticationToken("test", "test");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        calendar.add(Calendar.SECOND, 100);
        String plusTen = format.format(calendar.getTime());
        parametrit.setKoetuloksetLoppupvm(plusTen);

        calendar.add(Calendar.SECOND, -200);
        String minusTen = format.format(calendar.getTime());
        parametrit.setKoetuloksetAlkupvm(minusTen);
        ReflectionTestUtils.setField(parametriService, "parametrit", parametrit);

        visible = parametriService.pistesyottoEnabled("oid");
        assertTrue(visible);
    }

    @Test
    public void testHakeneetEnabled() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken("test", "test", Arrays.asList(new SimpleGrantedAuthority("ROLE_" + ROOTOID)));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        boolean visible = parametriService.hakeneetEnabled("oid");
        assertTrue(visible);

        authentication = new UsernamePasswordAuthenticationToken("test", "test");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 10000);
        parametrit.setHakuAlkupvm(format.format(calendar.getTime()));
        ReflectionTestUtils.setField(parametriService, "parametrit", parametrit);

        visible = parametriService.hakeneetEnabled("oid");
        assertFalse(visible);
    }

    @Test
    public void testHarkinnanvaraisetEnabled() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken("test", "test");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 100);
        parametrit.setKoetuloksetLoppupvm(format.format(calendar.getTime()));
        calendar.add(Calendar.SECOND, -200);
        parametrit.setHakuLoppupvm(format.format(calendar.getTime()));
        ReflectionTestUtils.setField(parametriService, "parametrit", parametrit);

        boolean visible = parametriService.harkinnanvaraisetEnabled("oid");
        assertTrue(visible);

        calendar.set(1970, 0, 1);
        parametrit.setKoetuloksetLoppupvm(format.format(calendar.getTime()));
        ReflectionTestUtils.setField(parametriService, "parametrit", parametrit);

        visible = parametriService.harkinnanvaraisetEnabled("oid");
        assertFalse(visible);
    }

}
