package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.util.Collection;

import fi.vm.sade.valinta.kooste.excel.DataRivi;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.SoluLukija;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;

public class ValintatapajonoDataRivi extends DataRivi {
    final Collection<? extends ValintatapajonoDataRiviKuuntelija> kuuntelijat;

    public ValintatapajonoDataRivi(Collection<Collection<Arvo>> arvot, Collection<? extends ValintatapajonoDataRiviKuuntelija> kuuntelijat) {
        super(arvot);
        this.kuuntelijat = kuuntelijat;
    }

    public boolean validoi(Rivi rivi) {
        SoluLukija lukija = new SoluLukija(rivi.getSolut());
        String oid = lukija.getArvoAt(0);
        String jonosija = lukija.getArvoAt(1);
        String nimi = lukija.getArvoAt(2);
        String tila = lukija.getArvoAt(3);
        String fi = lukija.getArvoAt(4);
        String sv = lukija.getArvoAt(5);
        String en = lukija.getArvoAt(6);
        ValintatapajonoRivi valintatapajonoRivi = new ValintatapajonoRivi(oid, jonosija, nimi, tila, fi, sv, en);
        for (ValintatapajonoDataRiviKuuntelija kuuntelija : kuuntelijat) {
            kuuntelija.valintatapajonoDataRiviTapahtuma(valintatapajonoRivi);
        }
        return true;
    }
}
