package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Arvosana {
	private String id;
	private String suoritus;
	private String aine;
	private Boolean valinnainen = false;
	private String myonnetty;
	private String source;
	private Map<String,String> lahdeArvot = new HashMap<>();
	private Arvio arvio = new Arvio();
    private String lisatieto;

	public Arvosana() {
	}

	public Arvosana(String id, String suoritus, String aine, Boolean valinnainen, String myonnetty, String source, Map<String,String> lahdeArvot, Arvio arvio, String lisatieto) {
		this.id = id;
		this.suoritus = suoritus;
        this.aine = aine;
		this.valinnainen = valinnainen;
		this.myonnetty = myonnetty;
		this.source = source;
		this.lahdeArvot = lahdeArvot;
		this.arvio = arvio;
        this.lisatieto = lisatieto;
	}

	public void setAine(String aine) {
		this.aine = aine;
	}

	public void setArvio(Arvio arvio) {
		this.arvio = arvio;
	}

	public void setLisatieto(String lisatieto) {
		this.lisatieto = lisatieto;
	}

	public void setMyonnetty(String myonnetty) {
		this.myonnetty = myonnetty;
	}

	public void setSuoritus(String suoritus) {
		this.suoritus = suoritus;
	}

	public void setValinnainen(Boolean valinnainen) {
		this.valinnainen = valinnainen;
	}

	public void setSource(String source) {
		this.source = source;
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

	public Map<String, String> getLahdeArvot() {
		return lahdeArvot;
	}

	public void setLahdeArvot(Map<String, String> lahdeArvot) {
		this.lahdeArvot = lahdeArvot;
	}
}
