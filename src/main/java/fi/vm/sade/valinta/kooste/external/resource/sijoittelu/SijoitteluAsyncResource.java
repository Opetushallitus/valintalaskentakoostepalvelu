package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import java.util.List;
import java.util.concurrent.Future;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;

public interface SijoitteluAsyncResource {

	Future<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid, String valintatapajonoOid);
	Future<HakijaPaginationObject> getKaikkiHakijat(
			String hakuOid, String hakukohdeOid);
	Future<HakijaPaginationObject> getKoulutuspaikkallisetHakijat(
			String hakuOid, String hakukohdeOid);

	Future<HakijaPaginationObject> getHakijatIlmanKoulutuspaikkaa(String hakuOid);
	
	Future<HakukohdeDTO> getLatestHakukohdeBySijoittelu(String hakuOid, String hakukohdeOid);
}
