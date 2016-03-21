package fi.vm.sade.valinta.kooste.external.resource.authentication.impl;

import fi.vm.sade.authentication.business.service.Authorizer;
import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.security.OidProvider;
import fi.vm.sade.security.OrganisationHierarchyAuthorizer;
import fi.vm.sade.valinta.kooste.external.resource.AsyncResourceWithCas;
import fi.vm.sade.valinta.kooste.external.resource.authentication.AuthorizationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class AuthorizationAsyncResourceImpl extends AsyncResourceWithCas implements AuthorizationAsyncResource {
    private final TarjontaAsyncResource tarjontaAsyncResource;

    @Autowired
    public AuthorizationAsyncResourceImpl(@Qualifier("AuthenticationServiceRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor,
                                          @Value("${valintalaskentakoostepalvelu.authentication.rest.url}") String address,
                                          ApplicationContext context,
                                          TarjontaAsyncResource tarjontaAsyncResource) {
        super(casInterceptor, address, context, TimeUnit.HOURS.toMillis(1));
        this.tarjontaAsyncResource = tarjontaAsyncResource;
    }

    @Override
    public void checkOrganisationAccess(
            Authentication authentication,
            String hakuOid, String hakukohdeOid, Collection<String> roolit, Consumer<Response> responseCallback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException("Not implemented");
        /*
        tarjontaAsyncResource.haeHakukohde(hakuOid, hakukohdeOid,
                hakukohde -> {
                    final String tarjoajaOid = hakukohde.getTarjoajaOid();
                    String url = "/resources/s2s/koostepalvelu";
                    getWebClient()
                            .path(url)
                            .accept(MediaType.APPLICATION_JSON_TYPE)
                            .async()
                            .post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE), new GenericType<List<Henkilo>>() {
                            });
                },
                poikkeus -> {
                    failureCallback.accept(poikkeus);
                });
                */
    }
    /*
    public Future<List<Henkilo>> haeTaiLuoHenkilot(List<HenkiloCreateDTO> henkiloPrototyypit) {
        String url = "/resources/s2s/koostepalvelu";
        return getWebClient()
                .path(url)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(henkiloPrototyypit, MediaType.APPLICATION_JSON_TYPE), new GenericType<List<Henkilo>>() {
                });
    }
    */
}
