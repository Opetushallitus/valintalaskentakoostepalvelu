package fi.vm.sade.valinta.kooste.kela.komponentti;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.LogEntry;

public interface TilaSource {
	LogEntry getVastaanottopvm(String hakemusOid, String hakuOid, String hakukohdeOid, String valintatapajonoOid);
}
