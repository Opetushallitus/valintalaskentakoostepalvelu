package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintalaskentaProsessi extends Prosessi {

    private List<String> kasitellytHakukohteet = new CopyOnWriteArrayList<String>();
    private volatile int hakukohteitaYhteensa;
    private Integer valinnanvaihe;

    public ValintalaskentaProsessi(String resurssi, String toiminto, String hakuOid, Integer hakukohteitaYhteensa,
            Integer valinnanvaihe) {
        super(resurssi, toiminto, hakuOid);
        this.valinnanvaihe = valinnanvaihe;
        setHakukohteitaYhteensa(hakukohteitaYhteensa);
    }

    public void setHakukohteitaYhteensa(Integer hakukohteitaYhteensa) {
        if (hakukohteitaYhteensa == null) {
            this.hakukohteitaYhteensa = -1; // <- ei pitaisi tapahtua mutta jos
                                            // tapahtuu niin merkki siita etta
                                            // camel-reitti on konfiguroitu
                                            // vaarin!
        } else {
            this.hakukohteitaYhteensa = hakukohteitaYhteensa;
        }
    }

    public String getHakukohteitaKasiteltyJaYhteensa() {
        StringBuilder b = new StringBuilder();
        if (hakukohteitaYhteensa == -1) {
            return b.append(kasitellytHakukohteet.size()).append(" / ")
                    .append("<< hakukohteet hakematta tarjonnalta >>").toString();
        } else {
            return b.append(kasitellytHakukohteet.size()).append(" / ").append(hakukohteitaYhteensa).toString();
        }
    }

    public int getHakukohteitaYhteensa() {
        return hakukohteitaYhteensa;
    }

    public Integer getValinnanvaihe() {
        return valinnanvaihe;
    }

    public List<String> getKasitellytHakukohteet() {
        return kasitellytHakukohteet;
    }

    public void addHakukohde(String hakukohdeOid) {
        kasitellytHakukohteet.add(hakukohdeOid);
    }

}
