package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

public interface LaskentaInfo {

	/**
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * 
	 * @return
	 */
	String getHakuOid();

	/**
	 * 
	 * @return
	 */
	boolean isValmis();

	/**
	 * 
	 * @return
	 */
	boolean isOsittainenLaskenta(); // eli maskilla aloitettu osajoukko koko
									// laskennasta

}
