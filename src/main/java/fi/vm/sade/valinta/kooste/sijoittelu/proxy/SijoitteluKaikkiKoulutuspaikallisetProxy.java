package fi.vm.sade.valinta.kooste.sijoittelu.proxy;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;

import java.util.Collection;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         TODO: Refaktoroi yhdeksi proxyksi sijoittelun operaatiot!
 */
public interface SijoitteluKaikkiKoulutuspaikallisetProxy {

    Collection<HakijaDTO> koulutuspaikalliset(String hakuOid);
}
