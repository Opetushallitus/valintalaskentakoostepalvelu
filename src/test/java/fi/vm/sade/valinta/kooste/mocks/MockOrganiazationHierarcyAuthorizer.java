package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.generic.service.exception.NotAuthorizedException;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Created by heikki.honkanen on 11/10/16.
 */
public class MockOrganiazationHierarcyAuthorizer implements OrganizationHierarchyAuthorizer {

    @Override
    public void checkAccess(Authentication currentUser, String targetOrganisationOid, String[] requriedRoles) throws NotAuthorizedException {
        return;
    }
}
