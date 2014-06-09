package fi.vm.sade.valinta.kooste.sijoitteluntulos.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Tiedosto {
	private final byte[] data;
	private final String tiedostonNimi;

	public Tiedosto(String tiedostonNimi, byte[] data) {
		this.tiedostonNimi = tiedostonNimi;
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}

	public String getTiedostonNimi() {
		return tiedostonNimi;
	}
}
