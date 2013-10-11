package fi.vm.sade.valinta.kooste.sijoittelu.proxy;

import java.util.Collection;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         TODO: Refaktoroi yhdeksi proxyksi sijoittelun operaatiot!
 */
public interface SijoitteluKoulutuspaikallisetProxy {

    Collection<HakijaDTO> koulutuspaikalliset(String hakuOid, String hakukohdeOid, String sijoitteluajoId);
}
