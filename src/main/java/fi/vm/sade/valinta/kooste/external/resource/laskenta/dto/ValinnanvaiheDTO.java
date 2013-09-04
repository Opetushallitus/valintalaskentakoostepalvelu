package fi.vm.sade.valinta.kooste.external.resource.laskenta.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ValinnanvaiheDTO {

    private int jarjestysnumero;
    private String valinnanvaiheoid;
    private Date createdAt;
    private List<ValintatapajonoDTO> valintatapajono = new ArrayList<ValintatapajonoDTO>();

    public List<ValintatapajonoDTO> getValintatapajono() {
        return valintatapajono;
    }

    public String getValinnanvaiheoid() {
        return valinnanvaiheoid;
    }

    public int getJarjestysnumero() {
        return jarjestysnumero;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

}