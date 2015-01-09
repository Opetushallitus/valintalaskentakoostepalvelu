package fi.vm.sade.valinta.kooste.security;

import fi.vm.sade.valinta.kooste.OrganisaatioOikeuksienTarkistus;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Created by jussija on 09/01/15.
 */
@Service("hasOrganization")
@Scope("session")
public class HasOrganizationImpl implements HasOrganization {
    private static final Logger LOG = LoggerFactory.getLogger(HasOrganizationImpl.class);
    @Autowired
    private OrganisaatioAsyncResource organisaatioAsyncResource;

    public boolean hasOrganization(String tarjoajaOid) throws Exception {
        LOG.error("Has Organisation tarkistus!");
        try {
            return OrganisaatioOikeuksienTarkistus.tarkistaKayttooikeudet(organisaatioAsyncResource.haeOrganisaationOidKetju(tarjoajaOid).get());
        } catch(Exception e) {
            LOG.error("Has Organisation tarkistus epäonnistui! {} {}", e.getMessage(), Arrays.asList(e.getStackTrace()));
            throw e;
        }
    }
}
