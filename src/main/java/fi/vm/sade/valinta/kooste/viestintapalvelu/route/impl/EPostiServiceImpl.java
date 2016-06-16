package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.OppijantunnistusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensRequest;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensResponse;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.EPostiRequest;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.EPostiResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.EPostiService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class EPostiServiceImpl implements EPostiService {
    private static final Logger LOG = LoggerFactory.getLogger(EPostiServiceImpl.class);

    private OppijantunnistusAsyncResource oppijantunnistusAsyncResource;
    private ViestintapalveluAsyncResource viestintapalveluAsyncResource;

    @Autowired
    public EPostiServiceImpl(OppijantunnistusAsyncResource oppijantunnistusAsyncResource,
                             ViestintapalveluAsyncResource viestintapalveluAsyncResource) {
        this.oppijantunnistusAsyncResource = oppijantunnistusAsyncResource;
        this.viestintapalveluAsyncResource = viestintapalveluAsyncResource;
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
                .flatMap( ePostiOsoitteet -> lahetaSecureLinkit(ePostiOsoitteet, ePostiRequest, hakuMessage, response))
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

    private Observable<List<String>> haeEPostiOsoitteet(Optional<Long> batchIdOptional, String hakuMessage, EPostiResponse response) {
        if(batchIdOptional.isPresent()) {
            LOG.info("Saatiin kirjelähetyksen id {}. Haetaan sähköpostiosoitteet. " + hakuMessage, batchIdOptional.get());
            response.setBatchId(batchIdOptional.get());
            return viestintapalveluAsyncResource.haeEPostiOsoitteet(batchIdOptional.get());
        }
        throw new RuntimeException("Ei löydetty sopivaa kirjelähetyksen ID:tä.");
    }

    private Observable<TokensResponse> lahetaSecureLinkit(List<String> ePostiOsoitteet, EPostiRequest ePostiRequest, String hakuMessage, EPostiResponse response) {
        if(!ePostiOsoitteet.isEmpty()) {
            LOG.info("Saatiin sähköpostiosoitteet {} kpl. Lähetetään oppijan tunnistukseen." + hakuMessage, ePostiOsoitteet.size());
            response.setNumberOfRecipients(ePostiOsoitteet.size());
            return oppijantunnistusAsyncResource.sendSecureLinks(
                    new TokensRequest("http://www.google.com/", ePostiRequest.getTemplateName(), ePostiRequest.getAsiointikieli(), ePostiOsoitteet, System.currentTimeMillis()));
        }
        throw new RuntimeException("Ei löydetty sähköpostiosoitteita.");
    }
}
