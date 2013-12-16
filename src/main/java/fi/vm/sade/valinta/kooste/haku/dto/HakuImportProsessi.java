package fi.vm.sade.valinta.kooste.haku.dto;

import java.util.concurrent.atomic.AtomicInteger;

import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class HakuImportProsessi extends Prosessi {

    // private List<String> importoitu = new CopyOnWriteArrayList<String>();
    private volatile int hakukohteita = 0;
    private AtomicInteger importoitu = new AtomicInteger();
    private AtomicInteger virhe = new AtomicInteger();
    private AtomicInteger tuonti = new AtomicInteger();

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
        return virhe.get();
    }

    public int lisaaImportoitu() {
        return importoitu.incrementAndGet();
    }

    public int lisaaTuonti() {
        return tuonti.incrementAndGet();
    }

    public int lisaaVirhe() {
        return virhe.incrementAndGet();
    }

    public void setHakukohteita(int hakukohteita) {
        this.hakukohteita = hakukohteita;
    }

}
