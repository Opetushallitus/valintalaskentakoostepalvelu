package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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

import java.util.*;
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
                + ", asiointikieli=" + ePostiRequest.getAsiointikieli();

        LOG.info("Aloitetaan securelinkien muodostus ja lähetys, " + hakuMessage);

        if(StringUtils.isBlank(ePostiRequest.getTemplateName())) {
            ePostiRequest.setTemplateName("opiskelijavalinnan_tulos_securelink");
        }

        EPostiResponse response = new EPostiResponse();
        viestintapalveluAsyncResource.haeJulkaistuKirjelahetys(ePostiRequest.getHakuOid(), ePostiRequest.getKirjeenTyyppi(), ePostiRequest.getAsiointikieli())
                .flatMap( batchIdOptional -> haeEPostiOsoitteet(batchIdOptional, hakuMessage, response) )
                .flatMap( ePostiOsoitteet -> teeTokensRequest(ePostiRequest, response, ePostiOsoitteet, hakuMessage) )
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

    private Observable<TokensRequest> teeTokensRequest(EPostiRequest ePostiRequest, EPostiResponse ePostiResponse, List<String> ePostiOsoitteet, String hakuMessage) {
        if(!ePostiOsoitteet.isEmpty()) {
            LOG.info("Saatiin sähköpostiosoitteet {} kpl. Lähetetään oppijan tunnistukseen." + hakuMessage, ePostiOsoitteet.size());
            ePostiResponse.setNumberOfRecipients(ePostiOsoitteet.size());
            return haeExpirationTime(ePostiRequest.getHakuOid())
                    .map( expirationTime -> {
                        TokensRequest request = new TokensRequest();
                        request.setEmails(ePostiOsoitteet);
                        request.setExpires(expirationTime);
                        request.setTemplatename(ePostiRequest.getTemplateName());
                        request.setLang(ePostiRequest.getAsiointikieli());
                        request.setUrl(secureLinkUrls.get(ePostiRequest.getAsiointikieli()));
                        return request;
                    });
        }
        throw new RuntimeException("Ei löydetty sähköpostiosoitteita.");
    }

    private Observable<Long> haeExpirationTime(String hakuOid) {
        return ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid).map(
                (parametritDTO) -> parametritDTO.getPH_HKP().getDate().getTime()
        );
    }

    private Observable<List<String>> haeEPostiOsoitteet(Optional<Long> batchIdOptional, String hakuMessage, EPostiResponse response) {
        if(batchIdOptional.isPresent()) {
            LOG.info("Saatiin kirjelähetyksen id {}. Haetaan sähköpostiosoitteet. " + hakuMessage, batchIdOptional.get());
            response.setBatchId(batchIdOptional.get());
            return viestintapalveluAsyncResource.haeEPostiOsoitteet(batchIdOptional.get());
        }
        throw new RuntimeException("Ei löydetty sopivaa kirjelähetyksen ID:tä.");
    }
}
