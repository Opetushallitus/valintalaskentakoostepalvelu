package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

public interface EsitiedonKuuntelija<T> {

	ValintalaskentaTyo esitietoSaatavilla(T esitieto);
}
