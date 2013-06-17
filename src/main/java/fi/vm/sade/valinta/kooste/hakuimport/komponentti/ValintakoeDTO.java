package fi.vm.sade.valinta.kooste.hakuimport.komponentti;

/**
 * Created with IntelliJ IDEA.
 * User: kkammone
 * Date: 17.6.2013
 * Time: 15:02
 * To change this template use File | Settings | File Templates.
 */
public class ValintakoeDTO {

    private String oid;
    private String tyyppiUri;

    public ValintakoeDTO() {
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getTyyppiUri() {
        return tyyppiUri;
    }

    public void setTyyppiUri(String tyyppiUri) {
        this.tyyppiUri = tyyppiUri;
    }
}
