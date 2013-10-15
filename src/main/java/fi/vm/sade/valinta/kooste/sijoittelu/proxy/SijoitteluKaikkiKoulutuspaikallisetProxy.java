package fi.vm.sade.valinta.kooste.sijoittelu.proxy;

import java.util.Collection;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         TODO: Refaktoroi yhdeksi proxyksi sijoittelun operaatiot!
 */
public interface SijoitteluKaikkiKoulutuspaikallisetProxy {

    Collection<HakijaDTO> koulutuspaikalliset(String hakuOid);
}
