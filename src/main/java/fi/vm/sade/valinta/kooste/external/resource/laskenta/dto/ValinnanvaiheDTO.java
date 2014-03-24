package fi.vm.sade.valinta.kooste.external.resource.laskenta.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.wordnik.swagger.annotations.ApiModelProperty;

public class ValinnanvaiheDTO {

	@ApiModelProperty(value = "Järjestysnumero", required = true)
	private int jarjestysnumero;

	@ApiModelProperty(value = "Valinnan vaiheen OID", required = true)
	private String valinnanvaiheoid;

	@ApiModelProperty(value = "Valinnan vaiheen nimi")
	private String nimi;

	@ApiModelProperty(value = "Luomisajankohta", required = true)
	private Date createdAt;

	@ApiModelProperty(value = "Valintatapajonot", required = true)
	private List<ValintatapajonoDTO> valintatapajono = new ArrayList<ValintatapajonoDTO>();

	public List<ValintatapajonoDTO> getValintatapajono() {
		return valintatapajono;
	}

	public String getValinnanvaiheoid() {
		return valinnanvaiheoid;
	}

	public void setValinnanvaiheoid(String valinnanvaiheoid) {
		this.valinnanvaiheoid = valinnanvaiheoid;
	}

	public void setValintatapajono(List<ValintatapajonoDTO> valintatapajono) {
		this.valintatapajono = valintatapajono;
	}

	public int getJarjestysnumero() {
		return jarjestysnumero;
	}

	public void setJarjestysnumero(int jarjestysnumero) {
		this.jarjestysnumero = jarjestysnumero;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getNimi() {
		return nimi;
	}

	public void setNimi(String nimi) {
		this.nimi = nimi;
	}
}