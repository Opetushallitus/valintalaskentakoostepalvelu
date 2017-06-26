package fi.vm.sade.valinta.kooste.util;

public class TuloskirjeNimiPaattelyStrategy extends NimiPaattelyStrategy {
    @Override
    public String paatteleNimi(String kutsumanimi, String etunimet) {
        // OK-145: Kutsumanimen sijaan kirjeelle tulee hakijan koko nimi
        return etunimet;
    }
}
