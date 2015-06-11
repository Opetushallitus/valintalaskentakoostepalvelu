package fi.vm.sade.valinta.kooste.haku.dto;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;

public class HakuImportProsessi extends Prosessi {
    private volatile int hakukohteita = 0;
    private AtomicInteger importoitu = new AtomicInteger();
    private AtomicInteger tuonti = new AtomicInteger();
    private CopyOnWriteArrayList<String> epaonnistuneetHakukohteet = new CopyOnWriteArrayList<String>();

    public HakuImportProsessi(String toiminto, String hakuOid) {
        super("Haku Import", toiminto, hakuOid);
    }

    public int getHakukohteita() {
        return hakukohteita;
    }

    public int getImportoitu() {
        return importoitu.get();
    }

    public int getTuonti() {
        return tuonti.get();
    }

    public int getVirhe() {
        return epaonnistuneetHakukohteet.size();
    }

    public String[] getEpaonnistuneetHakukohteet() {
        return epaonnistuneetHakukohteet.toArray(new String[]{});
    }

    public int lisaaImportoitu() {
        return importoitu.incrementAndGet();
    }

    public int lisaaTuonti() {
        return tuonti.incrementAndGet();
    }

    public int lisaaVirhe(String hakukohdeOid) {
        epaonnistuneetHakukohteet.add(hakukohdeOid);
        return epaonnistuneetHakukohteet.size();
    }

    public void setHakukohteita(int hakukohteita) {
        this.hakukohteita = hakukohteita;
    }
}
