package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Wrapperi sijoittelun HakutoiveDTO:lle jossa meta dataa liittyen
 *         kaikkiin hakukohteen hakijoihin
 */
public class MetaHakukohde {

    private final Teksti hakukohdeNimi;
    private final Teksti tarjoajaNimi;

    public MetaHakukohde(Teksti hakukohdeNimi, Teksti tarjoajaNimi) {
        this.hakukohdeNimi = hakukohdeNimi;
        this.tarjoajaNimi = tarjoajaNimi;
    }

    public Teksti getHakukohdeNimi() {
        return hakukohdeNimi;
    }

    public Teksti getTarjoajaNimi() {
        return tarjoajaNimi;
    }

}
