package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class Oppija {

	private String oppijanumero;
	private List<Opiskelu> opiskelu;
	private List<SuoritusJaArvosanat> suoritukset;
	private List<Opiskeluoikeus> opiskeluoikeudet;
	private boolean ensikertalainen;

	public boolean isEnsikertalainen() {
		return ensikertalainen;
	}

	public List<Opiskelu> getOpiskelu() {
		return opiskelu;
	}

	public List<SuoritusJaArvosanat> getSuoritukset() {
		return suoritukset;
	}

	public List<Opiskeluoikeus> getOpiskeluoikeudet() {
		return opiskeluoikeudet;
	}

	public String getOppijanumero() {
		return oppijanumero;
	}

	// oppijanumero: "1.2.246.562.24.41800604157",
	// opiskelu: [ ],
	// suoritukset: [
	// {
	// suoritus: {
	// id: "1550748b-7b86-4f8f-91dc-bb61be002b98",
	// komo: "1.2.246.562.13.62959769647",
	// myontaja: "1.2.246.562.10.89541505767",
	// tila: "KESKEN",
	// valmistuminen: "01.09.2014",
	// henkiloOid: "1.2.246.562.24.41800604157",
	// yksilollistaminen: "Ei",
	// suoritusKieli: "AB",
	// source: "1.2.246.562.24.00000000001"
	// },
	// arvosanat: [
	// {
	// id: "4b91c010-bf29-40c7-8eda-0280b11f8dc6",
	// suoritus: "1550748b-7b86-4f8f-91dc-bb61be002b98",
	// arvio: {
	// arvosana: "7",
	// asteikko: "4-10"
	// },
	// aine: "AI",
	// valinnainen: false,
	// myonnetty: "01.01.2014",
	// source: "1.2.246.562.24.00000000001"
	// },
	// {
	// id: "b38832b5-8782-4a84-8415-43099bd928ca",
	// suoritus: "1550748b-7b86-4f8f-91dc-bb61be002b98",
	// arvio: {
	// arvosana: "9",
	// asteikko: "4-10"
	// },
	// aine: "B1",
	// valinnainen: false,
	// myonnetty: "01.01.2014",
	// source: "1.2.246.562.24.00000000001"
	// }
	// ]
	// },
	// {
	// suoritus: {
	// id: "817b2a9c-297c-472a-bad4-4559b322a054",
	// komo: "1.2.246.562.5.2013061010184237348007",
	// myontaja: "1.2.246.562.10.43628088406",
	// tila: "VALMIS",
	// valmistuminen: "01.06.2014",
	// henkiloOid: "1.2.246.562.24.41800604157",
	// yksilollistaminen: "Ei",
	// suoritusKieli: "FI",
	// source: "1.2.246.562.24.00000000001"
	// },
	// arvosanat: [
	// {
	// id: "61903a5d-6e5d-4fc7-b0b9-043ca55038bb",
	// suoritus: "817b2a9c-297c-472a-bad4-4559b322a054",
	// arvio: {
	// arvosana: "L",
	// asteikko: "YO",
	// pisteet: 5
	// },
	// aine: "A",
	// valinnainen: false,
	// myonnetty: "01.06.1985",
	// source: "1.2.246.562.24.00000000001"
	// },
	// {
	// id: "cd3e8909-e9b6-457c-9fdd-08afb08ef72e",
	// suoritus: "817b2a9c-297c-472a-bad4-4559b322a054",
	// arvio: {
	// arvosana: "A",
	// asteikko: "YO",
	// pisteet: 4
	// },
	// aine: "DC",
	// valinnainen: false,
	// myonnetty: "01.06.1981",
	// source: "1.2.246.562.24.00000000001"
	// }
	// ]
	// }
	// ],
	// opiskeluoikeudet: [ ],
	// ensikertalainen: true
}
