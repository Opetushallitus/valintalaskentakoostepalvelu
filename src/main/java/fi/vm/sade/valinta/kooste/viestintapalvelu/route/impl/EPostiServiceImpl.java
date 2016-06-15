package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.OppijantunnistusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensRequest;
import fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto.TokensResponse;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.EPostiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.List;
import java.util.Optional;

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
    public void lahetaSecurelinkit(String hakuOid, String kirjeenTyyppi, String asiointikieli, String templateName) {
        String hakuMessage = "Haku=" + hakuOid + ", kirjeen tyyppi=" + kirjeenTyyppi + ", asiointikieli=" + asiointikieli;

        LOG.info("Aloitetaan securelinkien muodostus ja lähetys, " + hakuMessage);

        viestintapalveluAsyncResource.haeJulkaistuKirjelahetys(hakuOid, kirjeenTyyppi, asiointikieli)
                .flatMap( batchIdOptional -> haeEPostiOsoitteet(batchIdOptional, hakuMessage) )
                .flatMap( ePostiOsoitteet -> lahetaSecureLinkit(ePostiOsoitteet, templateName, asiointikieli, hakuMessage))
                .subscribe(

                        tokensResponse -> LOG.info("Valmis. Securelinkit {} kpl on lähetetty Viestintäpalveluun. "
                                + hakuMessage, tokensResponse.getRecipients().size()),

                        throwable -> LOG.error("Securelinkien vienti Viestintäpalveluun epäonnistui. "
                                + hakuMessage, throwable)
                );
    }

    private Observable<List<String>> haeEPostiOsoitteet(Optional<Long> batchIdOptional, String hakuMessage) {
        if(batchIdOptional.isPresent()) {
            LOG.info("Saatiin kirjelähetyksen id {}. Haetaan sähköpostiosoitteet. " + hakuMessage, batchIdOptional.get());
            return viestintapalveluAsyncResource.haeEPostiOsoitteet(batchIdOptional.get());
        }
        throw new RuntimeException("Ei löydetty sopivaa kirjelähetyksen ID:tä.");
    }

    private Observable<TokensResponse> lahetaSecureLinkit(List<String> ePostiOsoitteet, String templateName, String asiointikieli, String hakuMessage) {
        if(!ePostiOsoitteet.isEmpty()) {
            LOG.info("Saatiin sähköpostiosoitteet {} kpl. Lähetetään oppijan tunnistukseen." + hakuMessage, ePostiOsoitteet.size());
            return oppijantunnistusAsyncResource.sendSecureLinks(
                    new TokensRequest("http://www.google.com/", templateName, asiointikieli, ePostiOsoitteet, System.currentTimeMillis()));
        }
        throw new RuntimeException("Ei löydetty sähköpostiosoitteita.");
    }
}
