package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel(value = "ValinnanvaiheDTO", description = "Valinnan vaihe")
public class MergeValinnanvaiheDTO {
    @ApiModelProperty(value = "Järjestysnumero", required = true)
    private int jarjestysnumero;

    @ApiModelProperty(value = "Valinnan vaiheen OID", required = true)
    private String valinnanvaiheoid;

    @ApiModelProperty(value = "Haun OID", required = true)
    private String hakuOid;

    @ApiModelProperty(value = "Valinnan vaiheen nimi")
    private String nimi;

    @ApiModelProperty(value = "Onko viimeinen valinnanvaihe")
    private boolean viimeinenVaihe = false;

    @ApiModelProperty(value = "Valinnan vaiheen hakukohteen OID")
    private String hakukohdeOid;

    public String getHakukohdeOid() {
        return hakukohdeOid;
    }

    public void setHakukohdeOid(String hakukohdeOid) {
        this.hakukohdeOid = hakukohdeOid;
    }

    private List<MergeValintatapajonoDTO> valintatapajonot = new ArrayList<>();

    public int getJarjestysnumero() {
        return jarjestysnumero;
    }

    public void setJarjestysnumero(int jarjestysnumero) {
        this.jarjestysnumero = jarjestysnumero;
    }

    public String getValinnanvaiheoid() {
        return valinnanvaiheoid;
    }

    public void setValinnanvaiheoid(String valinnanvaiheoid) {
        this.valinnanvaiheoid = valinnanvaiheoid;
    }

    public String getHakuOid() {
        return hakuOid;
    }

    public void setHakuOid(String hakuOid) {
        this.hakuOid = hakuOid;
    }

    public String getNimi() {
        return nimi;
    }

    public void setNimi(String nimi) {
        this.nimi = nimi;
    }

    public List<MergeValintatapajonoDTO> getValintatapajonot() {
        return valintatapajonot;
    }

    public void setValintatapajonot(List<MergeValintatapajonoDTO> valintatapajonot) {
        this.valintatapajonot = valintatapajonot;
    }

    public boolean isViimeinenVaihe() {
        return viimeinenVaihe;
    }

    public void setViimeinenVaihe(boolean viimeinenVaihe) {
        this.viimeinenVaihe = viimeinenVaihe;
    }
}
