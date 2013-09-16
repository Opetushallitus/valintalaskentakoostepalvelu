package fi.vm.sade.valinta.kooste.parametrit;

/**
 * User: tommiha
 * Date: 8/20/13
 * Time: 2:35 PM
 */
public class Parametrit {
    /*private long valintakoekutsutAlkupvm;
    private long valintakoekutsutLoppupvm;
    private long valintakoekutsutkouluillePvm;
    private long valintakoeAlkupvm;
    private long valintakoeLoppupvm;*/
    private long koetuloksetPvm;
    /*private long valintajasijoitteluAlkupvm;
    private long valintajasijoitteluLoppupvm;*/
    private long valintaesitysPvm;
    private long hakuAlkupvm;
    private long hakuLoppupvm;


    public long getKoetuloksetPvm() {
        return koetuloksetPvm;
    }

    public void setKoetuloksetPvm(long koetuloksetPvm) {
        this.koetuloksetPvm = koetuloksetPvm;
    }

    public long getValintaesitysPvm() {
        return valintaesitysPvm;
    }

    public void setValintaesitysPvm(long valintaesitysPvm) {
        this.valintaesitysPvm = valintaesitysPvm;
    }

    public void setHakuAlkupvm(long hakuAlkupvm) {
        this.hakuAlkupvm = hakuAlkupvm;
    }

    public long getHakuAlkupvm() {
        return hakuAlkupvm;
    }

    public void setHakuLoppupvm(long hakuLoppupvm) {
        this.hakuLoppupvm = hakuLoppupvm;
    }

    public long getHakuLoppupvm() {
        return hakuLoppupvm;
    }
}
