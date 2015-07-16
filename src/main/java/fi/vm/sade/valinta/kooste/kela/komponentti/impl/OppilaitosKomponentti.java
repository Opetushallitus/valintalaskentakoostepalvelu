package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.valinta.kooste.exception.OrganisaatioException;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import org.apache.commons.lang.StringUtils;

@Component
public class OppilaitosKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(OppilaitosKomponentti.class);
    private static final String EMPTY_OPPILAITOSNRO_INDICATOR = "XXXXX";
    
    @Autowired
    private OrganisaatioResource organisaatioProxy;

    public String haeOppilaitosnumero(String tarjoajaOid) {
        try {
            OrganisaatioRDTO organisaatio = organisaatioProxy.getOrganisaatioByOID(tarjoajaOid);
            if (organisaatio == null) {
                // Tarjoaja org not found from organization service
                LOG.error("Oppilaitosnumeroa ei voitu hakea organisaatiolle (null) {}", tarjoajaOid);
                return EMPTY_OPPILAITOSNRO_INDICATOR;
            } else {
                if (StringUtils.isNotBlank(organisaatio.getOppilaitosKoodi())) {
                    // Tarjoaja org has oppilaitoskoodi so return it
                    return organisaatio.getOppilaitosKoodi();
                } else {
                    // Tarjoaja org was not of type Oppilaitos so we need to check parents and children
                    String oppilaitosNro = getOppilaitosnumeroFromParent(organisaatio);

                    if (oppilaitosNro != null) {
                        return oppilaitosNro;
                    }

                    oppilaitosNro = getOppilaitosnumeroFromChildren(organisaatio);

                    if (oppilaitosNro != null) {
                        return oppilaitosNro;
                    }

                    // Oppilaitosnumero not found from parents or children
                    LOG.error("Organisaatiopalvelu ei palauttanut yhteishaun oppilaitosnumeroa tarjoajalle "
                            + tarjoajaOid + " eikä sen isäntä- tai lapsiorganisaatioille.");
                    return EMPTY_OPPILAITOSNRO_INDICATOR;
                }
            }
        } catch (Exception e) {
            LOG.error("Yhteishaunkoulukoodia ei voitu hakea organisaatiolle " + tarjoajaOid, e);
        }
        throw new OrganisaatioException("Organisaatiopalvelu ei palauttanut oppilaitosnumeroa tarjoajalle " + tarjoajaOid);
    }

    public String haeOppilaitosKoodi(String tarjoajaOid) {
        try {
            OrganisaatioRDTO organisaatio = organisaatioProxy.getOrganisaatioByOID(tarjoajaOid);
            if (organisaatio == null) {
                LOG.error("Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}", tarjoajaOid);
            } else {
                if (organisaatio.getYhteishaunKoulukoodi() == null) {
                    LOG.error("Yhteishaunkoulukoodia ei voitu hakea organisaatiolle {}", tarjoajaOid);
                } else {
                    return organisaatio.getYhteishaunKoulukoodi();
                }
            }
        } catch (Exception e) {
            LOG.error("Yhteishaunkoulukoodia ei voitu hakea organisaatiolle " + tarjoajaOid, e);
        }
        throw new OrganisaatioException("Organisaatiopalvelu ei palauttanut yhteishaun koulukoodia tarjoajalle " + tarjoajaOid);
    }

    private String getOppilaitosnumeroFromParent(OrganisaatioRDTO organisaatio) {
        List<String> visitedOids = new LinkedList<>();
        String tarjoajaOid = organisaatio.getOid();

        // Go through parents until oppilaitoskoodi found
        while (StringUtils.isBlank(organisaatio.getOppilaitosKoodi())) {
            visitedOids.add(organisaatio.getOid());

            if (StringUtils.isNotEmpty(organisaatio.getParentOid())) {
                // Find parent org
                organisaatio = organisaatioProxy.getOrganisaatioByOID(organisaatio.getParentOid());

                if (organisaatio == null) {
                    return null;
                }
                if (visitedOids.contains(organisaatio.getParentOid())) {
                    // We should never get here
                    throw new OrganisaatioException("Organisaatiopalvelu : circular reference in parentoids {} " + tarjoajaOid);
                }
            } else {
                // Got to the end of parent chain
                return null;
            }

        }

        return organisaatio.getOppilaitosKoodi();
    }

    private String getOppilaitosnumeroFromChildren(OrganisaatioRDTO organisaatio) throws Exception {
        if (StringUtils.isNotBlank(organisaatio.getOppilaitosKoodi())) {
            return organisaatio.getOppilaitosKoodi();
        }

        List<OrganisaatioRDTO> children = organisaatioProxy.children(organisaatio.getOid(), false);

        if (children != null && !children.isEmpty()) {
            for (OrganisaatioRDTO childOrg : children) {
                String oppilaitosNro = getOppilaitosnumeroFromChildren(childOrg);
                if (oppilaitosNro != null) {
                    return oppilaitosNro;
                }
            }
        }

        return null;
    }
}
