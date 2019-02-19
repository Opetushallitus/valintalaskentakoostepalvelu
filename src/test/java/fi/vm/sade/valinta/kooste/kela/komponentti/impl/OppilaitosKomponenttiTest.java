/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import com.google.common.collect.Lists;
import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

/**
 *
 * @author antto.sierla
 */
@RunWith(MockitoJUnitRunner.class)
public class OppilaitosKomponenttiTest {
    @Mock
    private OrganisaatioResource organisaatioResource;

    @InjectMocks
    OppilaitosKomponentti oppilaitosKomponentti = new OppilaitosKomponentti();

    @Before
    public void setup() throws Exception {
        /*
        ValintaperusteetValinnanVaiheDTO valinnanVaihe = new ValintaperusteetValinnanVaiheDTO();
        valinnanVaihe.setValinnanVaiheOid("valinnanVaiheFoundId");

        ValintaperusteetDTO valintaperusteet = new ValintaperusteetDTO();
        valintaperusteet.setValinnanVaihe(valinnanVaihe);

        when(valintaperusteetResource.haeValintaperusteet("hakukohdeoid", null)).thenReturn(Lists.newArrayList(valintaperusteet));
        */
        
        OrganisaatioRDTO org = new OrganisaatioRDTO();
        org.setOid("orgoid");
        org.setOppilaitosKoodi("oppilaitosNro");
        
        when(organisaatioResource.getOrganisaatioByOID("orgoid")).thenReturn(org);
        
        // For parent search
        OrganisaatioRDTO orgWithParent = new OrganisaatioRDTO();
        orgWithParent.setOid("orgWithParentOid");
        orgWithParent.setOppilaitosKoodi(null);
        orgWithParent.setParentOid("parentOid");
        
        OrganisaatioRDTO parentOrg = new OrganisaatioRDTO();
        parentOrg.setOid("parentOid");
        parentOrg.setOppilaitosKoodi("oppilaitosNroFromParent");
        
        when(organisaatioResource.getOrganisaatioByOID("orgWithParentOid")).thenReturn(orgWithParent);
        when(organisaatioResource.getOrganisaatioByOID("parentOid")).thenReturn(parentOrg);
        
        // For children search
        OrganisaatioRDTO orgWithChildren = new OrganisaatioRDTO();
        orgWithChildren.setOid("orgWithChildrenOid");
        orgWithChildren.setOppilaitosKoodi(null);
        orgWithChildren.setParentOid(null);
        
        OrganisaatioRDTO childOrg1 = new OrganisaatioRDTO();
        childOrg1.setOid("child1Oid");
        childOrg1.setOppilaitosKoodi(null);
        
        OrganisaatioRDTO childOrg2 = new OrganisaatioRDTO();
        childOrg2.setOid("child2Oid");
        childOrg2.setOppilaitosKoodi("oppilaitosNroFromChild");
        
        when(organisaatioResource.getOrganisaatioByOID("orgWithChildrenOid")).thenReturn(orgWithChildren);
        when(organisaatioResource.children("orgWithChildrenOid", false)).thenReturn(Arrays.asList(childOrg1, childOrg2));
        
        // For empty oppilaitosnumero
        OrganisaatioRDTO emptyOrg = new OrganisaatioRDTO();
        emptyOrg.setOid("emptyOrgOid");
        emptyOrg.setOppilaitosKoodi(null);
        
        when(organisaatioResource.getOrganisaatioByOID("emptyOrgOid")).thenReturn(emptyOrg);
    }

    @Test
    public void oppilaitosnumeroIsFoundDirectly() {
        String tarjoajaOid = "orgoid";
        String expResult = "oppilaitosNro";
        String result = oppilaitosKomponentti.haeOppilaitosnumero(tarjoajaOid);
        assertEquals(expResult, result);
    }
    
    @Test
    public void oppilaitosnumeroIsFoundFromParent() {
        String tarjoajaOid = "orgWithParentOid";
        String expResult = "oppilaitosNroFromParent";
        String result = oppilaitosKomponentti.haeOppilaitosnumero(tarjoajaOid);
        assertEquals(expResult, result);
    }
    
    @Test
    public void oppilaitosnumeroIsFoundFromChild() {
        String tarjoajaOid = "orgWithChildrenOid";
        String expResult = "oppilaitosNroFromChild";
        String result = oppilaitosKomponentti.haeOppilaitosnumero(tarjoajaOid);
        assertEquals(expResult, result);
    }
    
    @Test
    public void oppilaitosnumeroIsNotFound() {
        String tarjoajaOid = "emptyOrgOid";
        String expResult = "XXXXX";
        String result = oppilaitosKomponentti.haeOppilaitosnumero(tarjoajaOid);
        assertEquals(expResult, result);
    }
}
