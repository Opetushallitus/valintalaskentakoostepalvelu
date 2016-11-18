package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import com.google.common.collect.ImmutableMap;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.OppijantunnistusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensRequest;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.EPostiRequest;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.EPostiResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.EPostiService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class EPostiServiceImpl implements EPostiService {
    private static final Logger LOG = LoggerFactory.getLogger(EPostiServiceImpl.class);

    private OppijantunnistusAsyncResource oppijantunnistusAsyncResource;
    private ViestintapalveluAsyncResource viestintapalveluAsyncResource;
    private OhjausparametritAsyncResource ohjausparametritAsyncResource;
    private Map<String, String> secureLinkUrls;

    @Autowired
    public EPostiServiceImpl(OppijantunnistusAsyncResource oppijantunnistusAsyncResource,
                             ViestintapalveluAsyncResource viestintapalveluAsyncResource,
                             OhjausparametritAsyncResource ohjausparametritAsyncResource,
                             @Value("${omatsivut.email.application.modify.link.en}") String secureLinkUrlEn,
                             @Value("${omatsivut.email.application.modify.link.fi}") String secureLinkUrlFi,
                             @Value("${omatsivut.email.application.modify.link.sv}") String secureLinkUrlSv) {
        this.oppijantunnistusAsyncResource = oppijantunnistusAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
        this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
        secureLinkUrls = ImmutableMap.of("fi", secureLinkUrlFi, "sv", secureLinkUrlSv, "en", secureLinkUrlEn);
    }

    @Override
    public void lahetaSecurelinkit(EPostiRequest ePostiRequest, Consumer<EPostiResponse> success, Consumer<String> failure) {
        String hakuMessage = "Haku=" + ePostiRequest.getHakuOid()
                + ", kirjeen tyyppi=" + ePostiRequest.getKirjeenTyyppi()
                + ", asiointikieli=" + ePostiRequest.getAsiointikieli()
                + ", kirjeen tunniste=" + ePostiRequest.getLetterId();

        LOG.info("Aloitetaan securelinkien muodostus ja lähetys, " + hakuMessage);

        if(StringUtils.isBlank(ePostiRequest.getTemplateName())) {
            ePostiRequest.setTemplateName(ePostiRequest.getKirjeenTyyppi());
        }

        EPostiResponse response = new EPostiResponse();
        viestintapalveluAsyncResource.haeKirjelahetysEPostille(ePostiRequest.getHakuOid(), ePostiRequest.getKirjeenTyyppi(), ePostiRequest.getAsiointikieli())
                .flatMap( batchIdOptional -> haeEPostiOsoitteet(batchIdOptional, hakuMessage, response) )
                .flatMap( hakemusOidToEmailAddress -> teeTokensRequestJosOsoitteita(ePostiRequest, response, hakemusOidToEmailAddress, hakuMessage) )
                .flatMap( tokensRequest -> oppijantunnistusAsyncResource.sendSecureLinks(tokensRequest))
                .subscribe(

                        tokensResponse -> {
                            LOG.info("Valmis. Securelinkit {} kpl on lähetetty Viestintäpalveluun. "
                                    + hakuMessage, tokensResponse.getRecipients().size());
                            success.accept(response);
                        },

                        throwable -> {
                            LOG.error("Securelinkien vienti Viestintäpalveluun epäonnistui. "
                                    + hakuMessage, throwable);
                            failure.accept(throwable.getMessage());
                        }
                );
    }

    @Override
    public void esikatseleSecurelinkki(EPostiRequest ePostiRequest, Consumer<Response> success, Consumer<String> failure) {
        String hakuMessage = "Haku=" + ePostiRequest.getHakuOid()
                + ", kirjeen tyyppi=" + ePostiRequest.getKirjeenTyyppi()
                + ", asiointikieli=" + ePostiRequest.getAsiointikieli();

        if(StringUtils.isBlank(ePostiRequest.getTemplateName())) {
            ePostiRequest.setTemplateName(ePostiRequest.getKirjeenTyyppi());
        }

        teeTokensRequest(ePostiRequest, Collections.emptyMap())
                .flatMap( tokensRequest -> oppijantunnistusAsyncResource.previewSecureLink(tokensRequest))
                .subscribe(
                        response -> {
                            success.accept(response);
                        },

                        throwable -> {
                            LOG.error("Securelinkin esikatselu epäonnistui. "
                                    + hakuMessage, throwable);
                            failure.accept(throwable.getMessage());
                        }
                );
    }

    private Observable<TokensRequest> teeTokensRequestJosOsoitteita(EPostiRequest ePostiRequest, EPostiResponse ePostiResponse, Map<String, String> applicationOidToEmailAddress, String hakuMessage) {
        if(!applicationOidToEmailAddress.isEmpty()) {
            LOG.info("Saatiin sähköpostiosoitteet {} kpl. Lähetetään oppijan tunnistukseen." + hakuMessage, applicationOidToEmailAddress.size());
            ePostiResponse.setNumberOfRecipients(applicationOidToEmailAddress.size());
            return teeTokensRequest(ePostiRequest, applicationOidToEmailAddress);
        }
        throw new RuntimeException("Ei löydetty sähköpostiosoitteita.");
    }

    private Observable<TokensRequest> teeTokensRequest(EPostiRequest ePostiRequest, Map<String, String> applicationOidToEmailAddress) {
        return haeExpirationTime(ePostiRequest.getHakuOid())
                .map( expirationTime -> {
                    TokensRequest request = new TokensRequest();
                    request.setApplicationOidToEmailAddress(applicationOidToEmailAddress);
                    request.setExpires(expirationTime);
                    request.setTemplatename(ePostiRequest.getTemplateName());
                    request.setHakuOid(ePostiRequest.getHakuOid());
                    request.setLetterId(ePostiRequest.getLetterId());
                    request.setLang(ePostiRequest.getAsiointikieli());
                    request.setUrl(secureLinkUrls.get(ePostiRequest.getAsiointikieli()));
                    return request;
                });
    }

    private Observable<Long> haeExpirationTime(String hakuOid) {
        return ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid).map(
                (parametritDTO) -> parametritDTO.getPH_HKP().getDate().getTime()
        );
    }

    private Observable<Map<String, String>> haeEPostiOsoitteet(Optional<Long> batchIdOptional, String hakuMessage, EPostiResponse response) {
        if(batchIdOptional.isPresent()) {
            LOG.info("Saatiin kirjelähetyksen id {}. Haetaan sähköpostiosoitteet. " + hakuMessage, batchIdOptional.get());
            response.setBatchId(batchIdOptional.get());
            return viestintapalveluAsyncResource.haeEPostiOsoitteet(batchIdOptional.get());
        }
        throw new RuntimeException("Ei löydetty sopivaa kirjelähetyksen ID:tä.");
    }
}
