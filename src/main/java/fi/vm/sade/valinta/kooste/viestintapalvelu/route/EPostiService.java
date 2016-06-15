package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

public interface EPostiService {

    void lahetaSecurelinkit(String hakuOid, String kirjeenTyyppi, String asiointikieli, String templateName);

}
