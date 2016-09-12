package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.util.Collection;

import fi.vm.sade.valinta.kooste.excel.DataRivi;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;

public class ValintatapajonoDataRivi extends DataRivi {
    final Collection<? extends ValintatapajonoDataRiviKuuntelija> kuuntelijat;

    public ValintatapajonoDataRivi(Collection<Collection<Arvo>> arvot, Collection<? extends ValintatapajonoDataRiviKuuntelija> kuuntelijat) {
        super(arvot);
        this.kuuntelijat = kuuntelijat;
    }

    public boolean validoi(Rivi rivi) {
        String oid = rivi.getArvoAt(0);
        String jonosija = rivi.getArvoAt(1);
        String nimi = rivi.getArvoAt(2);
        String tila = rivi.getArvoAt(3);
        String pisteet = rivi.getArvoAt(4);
        String fi = rivi.getArvoAt(5);
        String sv = rivi.getArvoAt(6);
        String en = rivi.getArvoAt(7);
        ValintatapajonoRivi valintatapajonoRivi = new ValintatapajonoRivi(oid, jonosija, nimi, tila, pisteet, fi, sv, en);
        for (ValintatapajonoDataRiviKuuntelija kuuntelija : kuuntelijat) {
            kuuntelija.valintatapajonoDataRiviTapahtuma(valintatapajonoRivi);
        }
        return true;
    }
}
