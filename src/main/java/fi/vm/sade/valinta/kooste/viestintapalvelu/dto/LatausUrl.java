package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

/**
 * Paluuviesti koostepalvelun aktivoineelle clientille viestintapalvelun
 * cache-osoitteeseen, eli lopulliseen PDF:n.
 */
public class LatausUrl {

    private String latausUrl;

    public LatausUrl(String latausUrl) {
        this.latausUrl = latausUrl;
    }
}
