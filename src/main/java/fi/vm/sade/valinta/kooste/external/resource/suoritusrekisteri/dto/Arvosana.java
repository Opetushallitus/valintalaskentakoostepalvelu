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
	private Boolean valinnainen;
	private String myonnetty;
	private String source;
	private Arvio arvio;
    private String lisatieto;

	public Arvosana() {
	}

	public Arvosana(String id, String suoritus, String aine,
			Boolean valinnainen, String myonnetty, String source, Arvio arvio, String lisatieto) {
		this.id = id;
		this.suoritus = suoritus;
        this.aine = aine;
		this.valinnainen = valinnainen;
		this.myonnetty = myonnetty;
		this.source = source;
		this.arvio = arvio;
        this.lisatieto = lisatieto;
	}

	public Boolean isValinnainen() {
		return valinnainen;
	}

	public Boolean getValinnainen() {
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

    public String getLisatieto() {
        return lisatieto;
    }
}
