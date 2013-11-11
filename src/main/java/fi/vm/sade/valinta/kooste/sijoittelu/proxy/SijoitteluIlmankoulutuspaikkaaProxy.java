package fi.vm.sade.valinta.kooste.sijoittelu.proxy;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         TODO: Refaktoroi yhdeksi proxyksi sijoittelun operaatiot!
 */
public interface SijoitteluIlmankoulutuspaikkaaProxy {

    List<HakijaDTO> ilmankoulutuspaikkaa(String hakuOid, String sijoitteluAjoId);
}
