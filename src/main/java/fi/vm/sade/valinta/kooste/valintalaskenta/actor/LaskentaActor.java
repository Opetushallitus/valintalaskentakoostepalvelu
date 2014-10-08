package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface LaskentaActor {

	String getHakuOid();

	boolean isValmis();

	void aloita();

	void lopeta();

}
