package fi.vm.sade.valinta.kooste.external.resource.laskenta.dto;

import java.util.ArrayList;
import java.util.List;

import com.wordnik.swagger.annotations.ApiModelProperty;

public class ValintatapajonoDTO {
	@ApiModelProperty(value = "OID", required = true)
	private String valintatapajonooid;

	@ApiModelProperty(value = "Nimi", required = true)
	private String nimi;

	@ApiModelProperty(value = "Prioriteetti", required = true)
	private int prioriteetti;

	@ApiModelProperty(value = "Aloituspaikat", required = true)
	private int aloituspaikat;

	@ApiModelProperty(value = "Siirretäänkö jono sijoitteluun", required = true)
	private boolean siirretaanSijoitteluun;

	@ApiModelProperty(value = "Tasasijasääntö", required = true)
	private Tasasijasaanto tasasijasaanto;

	@ApiModelProperty(value = "Onko varasijatäyttö käytössä", required = true)
	private Boolean eiVarasijatayttoa;

	@ApiModelProperty(value = "Jonosijat", required = true)
	private List<JonosijaDTO> jonosijat = new ArrayList<JonosijaDTO>();

	public String getNimi() {
		return nimi;
	}

	public void setNimi(String nimi) {
		this.nimi = nimi;
	}

	public boolean isSiirretaanSijoitteluun() {
		return siirretaanSijoitteluun;
	}

	public void setSiirretaanSijoitteluun(boolean siirretaanSijoitteluun) {
		this.siirretaanSijoitteluun = siirretaanSijoitteluun;
	}

	public int getPrioriteetti() {
		return prioriteetti;
	}

	public int getAloituspaikat() {
		return aloituspaikat;
	}

	public void setAloituspaikat(int aloituspaikat) {
		this.aloituspaikat = aloituspaikat;
	}

	public void setPrioriteetti(int prioriteetti) {
		this.prioriteetti = prioriteetti;
	}

	public String getOid() {
		return valintatapajonooid;
	}

	public void setOid(String oid) {
		this.valintatapajonooid = oid;
	}

	@Override
	public int hashCode() {
		return valintatapajonooid.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof ValintatapajonoDTO) {
			ValintatapajonoDTO vtj = (ValintatapajonoDTO) obj;
			return this == vtj;
		}
		return false;
	}

	public Tasasijasaanto getTasasijasaanto() {
		return tasasijasaanto;
	}

	public void setTasasijasaanto(Tasasijasaanto tasasijasaanto) {
		this.tasasijasaanto = tasasijasaanto;
	}

	public List<JonosijaDTO> getJonosijat() {
		return jonosijat;
	}

	public void setJonosijat(List<JonosijaDTO> jonosijat) {
		this.jonosijat = jonosijat;
	}

	public Boolean getEiVarasijatayttoa() {
		return eiVarasijatayttoa;
	}

	public void setEiVarasijatayttoa(Boolean eiVarasijatayttoa) {
		this.eiVarasijatayttoa = eiVarasijatayttoa;
	}
}