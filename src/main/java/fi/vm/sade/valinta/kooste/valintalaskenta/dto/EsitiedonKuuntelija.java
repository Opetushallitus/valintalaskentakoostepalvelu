package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

public interface EsitiedonKuuntelija<A, T> {

	A esitietoSaatavilla(T esitieto);

	A esitietoOhitettu();
}
