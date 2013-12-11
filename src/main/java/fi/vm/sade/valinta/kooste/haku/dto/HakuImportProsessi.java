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
    private volatile int hakukohteita;
    private AtomicInteger importoitu = new AtomicInteger();

    public HakuImportProsessi(String toiminto, String hakuOid) {
        super("Haku Import", toiminto, hakuOid);
    }

    public int getHakukohteita() {
        return hakukohteita;
    }

    public int getImportoitu() {
        return importoitu.get();
    }

    public void lisaaImportoitu() {
        importoitu.incrementAndGet();
    }

    public void setHakukohteita(int hakukohteita) {
        this.hakukohteita = hakukohteita;
    }

}
