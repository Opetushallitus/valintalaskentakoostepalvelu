package fi.vm.sade.valinta.kooste.sijoittelu;

import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

/**
* Created with IntelliJ IDEA.
* User: jukais
* Date: 12.9.2013
* Time: 13.47
* To change this template use File | Settings | File Templates.
*/
@Component
public class Sijoittelu implements Serializable {

    private String  hakuOid;
    private Date    viimeksiAjettu;
    private String  lastError;

    public String getHakuOid() {
        return hakuOid;
    }

    public void setHakuOid(String hakuOid) {
        this.hakuOid = hakuOid;
    }

    public Date getViimeksiAjettu() {
        return viimeksiAjettu;
    }

    public void setViimeksiAjettu(Date viimeksiAjettu) {
        this.viimeksiAjettu = viimeksiAjettu;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
