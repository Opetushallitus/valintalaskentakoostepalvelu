package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Arvosana {
	private String id;
	private String suoritus;
	private String aine;
	private boolean valinnainen;
	private String myonnetty;
	private String source;
	private Arvio arvio;

	public boolean isValinnainen() {
		return valinnainen;
	}

	public boolean getValinnainen() {
		return valinnainen;
	}

	public Arvio getArvio() {
		return arvio;
	}

	public String getAine() {
		return aine;
	}

	public String getId() {
		return id;
	}

	public String getMyonnetty() {
		return myonnetty;
	}

	public String getSource() {
		return source;
	}

	public String getSuoritus() {
		return suoritus;
	}
}
