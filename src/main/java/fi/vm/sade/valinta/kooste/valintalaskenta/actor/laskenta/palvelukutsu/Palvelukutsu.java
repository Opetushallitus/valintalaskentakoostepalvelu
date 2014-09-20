package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

import java.util.function.Consumer;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface Palvelukutsu extends Comparable<Palvelukutsu> {

	boolean onkoPeruutettu();

	void peruuta();

	/**
	 * WARNING: Ala kutsu suoraan. Anna PalvelukutsuStrategian tehda
	 * palvelukutsut sita mukaa kuin SLA:ssa on sovittu.
	 * 
	 * @param takaisinkutsu
	 *            tehdaan palvelukutsun omalla referenssilla
	 * @return palauttaa referenssin itseensa
	 */
	Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu);

	String getHakukohdeOid();
}
