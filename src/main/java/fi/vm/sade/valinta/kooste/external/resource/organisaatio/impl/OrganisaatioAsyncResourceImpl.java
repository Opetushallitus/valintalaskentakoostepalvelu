package fi.vm.sade.valinta.kooste.external.resource.organisaatio.impl;

import java.util.concurrent.Future;

import com.google.common.util.concurrent.Futures;
import org.springframework.stereotype.Service;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         esim
 *         /organisaatio-service/rest/organisaatio/1.2.246.562.10.39218317368
 *         ?noCache=1413976497594
 */
@Service
public class OrganisaatioAsyncResourceImpl implements OrganisaatioAsyncResource {

	public OrganisaatioAsyncResourceImpl() {

	}

	@Override
	public Future<OrganisaatioRDTO> haeOrganisaatio(String organisaatioOid) {

        return Futures.immediateFuture(null);
	}
}
