package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class SuoritusJaArvosanat {
	private Suoritus suoritus;
	private List<Arvosana> arvosanat  = Lists.newArrayList();

	public List<Arvosana> getArvosanat() {
		return arvosanat;
	}

	public Suoritus getSuoritus() {
		return suoritus;
	}

	public void setSuoritus(Suoritus suoritus) {
		this.suoritus = suoritus;
	}
}
