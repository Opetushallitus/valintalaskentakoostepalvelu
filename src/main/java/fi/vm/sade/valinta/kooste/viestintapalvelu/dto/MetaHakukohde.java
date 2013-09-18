package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;


/**
 * 
 * @author Jussi Jartamo
 * 
 *         Wrapperi sijoittelun HakutoiveDTO:lle jossa meta dataa liittyen
 *         kaikkiin hakukohteen hakijoihin
 */
public class MetaHakukohde {

    private final String hakukohdeNimi;
    private final String tarjoajaNimi;

    public MetaHakukohde(String hakukohdeNimi, String tarjoajaNimi) {
        this.hakukohdeNimi = hakukohdeNimi;
        this.tarjoajaNimi = tarjoajaNimi;
    }

    public String getHakukohdeNimi() {
        return hakukohdeNimi;
    }

    public String getTarjoajaNimi() {
        return tarjoajaNimi;
    }

}
