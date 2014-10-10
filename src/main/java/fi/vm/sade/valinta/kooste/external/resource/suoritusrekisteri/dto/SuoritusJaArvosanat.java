package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class SuoritusJaArvosanat {
	private Suoritus suoritus;
	private List<Arvosana> arvosanat;

	public List<Arvosana> getArvosanat() {
		return arvosanat;
	}

	public Suoritus getSuoritus() {
		return suoritus;
	}
}
