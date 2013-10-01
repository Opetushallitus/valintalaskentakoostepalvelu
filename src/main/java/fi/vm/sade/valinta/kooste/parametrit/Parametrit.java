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
    private String koetuloksetPvm;
    /*private long valintajasijoitteluAlkupvm;
    private long valintajasijoitteluLoppupvm;*/
    private String valintaesitysPvm;
    private String hakuAlkupvm;
    private String hakuLoppupvm;
    private String koetuloksetAlkupvm;


    public String getKoetuloksetLoppupvm() {
        return koetuloksetPvm;
    }

    public void setKoetuloksetLoppupvm(String koetuloksetPvm) {
        this.koetuloksetPvm = koetuloksetPvm;
    }

    public String getValintaesitysPvm() {
        return valintaesitysPvm;
    }

    public void setValintaesitysPvm(String valintaesitysPvm) {
        this.valintaesitysPvm = valintaesitysPvm;
    }

    public void setHakuAlkupvm(String hakuAlkupvm) {
        this.hakuAlkupvm = hakuAlkupvm;
    }

    public String getHakuAlkupvm() {
        return hakuAlkupvm;
    }

    public void setHakuLoppupvm(String hakuLoppupvm) {
        this.hakuLoppupvm = hakuLoppupvm;
    }

    public String getHakuLoppupvm() {
        return hakuLoppupvm;
    }

    public void setKoetuloksetAlkupvm(String koetuloksetAlkupvm) {
        this.koetuloksetAlkupvm = koetuloksetAlkupvm;
    }

    public String getKoetuloksetAlkupvm() {
        return koetuloksetAlkupvm;
    }
}
