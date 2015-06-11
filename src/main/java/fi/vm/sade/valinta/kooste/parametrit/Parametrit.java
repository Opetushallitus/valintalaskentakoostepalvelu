package fi.vm.sade.valinta.kooste.parametrit;

public class Parametrit {
    private String koetuloksetPvm;
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
