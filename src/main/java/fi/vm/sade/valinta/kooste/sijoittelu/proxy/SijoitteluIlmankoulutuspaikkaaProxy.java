package fi.vm.sade.valinta.kooste.sijoittelu.proxy;

import java.util.List;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         TODO: Refaktoroi yhdeksi proxyksi sijoittelun operaatiot!
 */
public interface SijoitteluIlmankoulutuspaikkaaProxy {

    List<HakijaDTO> ilmankoulutuspaikkaa(String hakuOid, String sijoitteluAjoId);
}
