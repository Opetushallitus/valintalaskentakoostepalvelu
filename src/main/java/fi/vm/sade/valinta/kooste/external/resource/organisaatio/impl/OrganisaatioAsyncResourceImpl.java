package fi.vm.sade.valinta.kooste.external.resource.organisaatio.impl;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *         https://${host.virkailija}/organisaatio-service/rest esim
 *         /organisaatio-service/rest/organisaatio/1.2.246.562.10.39218317368
 *         ?noCache=1413976497594
 */
@Service
public class OrganisaatioAsyncResourceImpl extends HttpResource implements OrganisaatioAsyncResource {
    @Autowired
    public OrganisaatioAsyncResourceImpl(
            @Value("${valintalaskentakoostepalvelu.organisaatioService.rest.url}") String address) {
        super(address, TimeUnit.HOURS.toMillis(1));
    }

    @Override
    public Future<Response> haeOrganisaatio(String organisaatioOid) {
        String url = "/organisaatio/" + organisaatioOid + "/";
        return getWebClient().path(url)
                .accept(MediaType.WILDCARD)
                .async()
                .get();
    }

    @Override
    public Future<String> haeOrganisaationOidKetju(String organisaatioOid) {
        String url = "/organisaatio/" + organisaatioOid + "/parentoids";
        return getWebClient().path(url)
                .accept(MediaType.WILDCARD)
                .async()
                .get(String.class);
    }

    @Override
    public Observable<HakutoimistoDTO> haeHakutoimisto(String organisaatioId) {
        return getAsObservable("/organisaatio/v2/" + organisaatioId + "/hakutoimisto", HakutoimistoDTO.class);
    }
}
