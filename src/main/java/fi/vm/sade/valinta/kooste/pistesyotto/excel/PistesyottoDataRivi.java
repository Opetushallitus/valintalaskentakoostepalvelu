package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.excel.DataRivi;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.SoluLukija;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;

public class PistesyottoDataRivi extends DataRivi {

    private final Collection<PistesyottoDataRiviKuuntelija> kuuntelijat;
    private final Collection<PistesyottoDataArvo> dataArvot;

    public PistesyottoDataRivi(Collection<Collection<Arvo>> arvot, Collection<PistesyottoDataRiviKuuntelija> kuuntelijat,
                               Collection<PistesyottoDataArvo> dataArvot) {
        super(arvot);
        this.kuuntelijat = kuuntelijat;
        this.dataArvot = dataArvot;
    }

    @Override
    public boolean validoi(Rivi rivi) {
        SoluLukija lukija = new SoluLukija(rivi.getSolut());
        String oid = lukija.getArvoAt(0);
        String nimi = lukija.getArvoAt(1);
        String hetu = lukija.getArvoAt(2);
        String pvm = lukija.getArvoAt(3);
        Collection<PistesyottoArvo> arvot = Lists.newArrayList();
        {
            int i = 4;
            for (PistesyottoDataArvo dataArvo : dataArvot) {
                arvot.add(dataArvo.asPistesyottoArvo(lukija.getArvoAt(i),
                        lukija.getArvoAt(i + 1)));
                i += 2;
            }
        }
        PistesyottoRivi pistesyottorivi = new PistesyottoRivi(oid, nimi, hetu, pvm, arvot);
        for (PistesyottoDataRiviKuuntelija kuuntelija : kuuntelijat) {
            kuuntelija.pistesyottoDataRiviTapahtuma(pistesyottorivi);
        }
        return true;
    }

}
