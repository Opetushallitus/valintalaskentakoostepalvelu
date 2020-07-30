package fi.vm.sade.valinta.kooste.kela.komponentti;

import java.util.Date;

public interface TilaSource {
  Date getVastaanottopvm(
      String hakemusOid, String hakuOid, String hakukohdeOid, String valintatapajonoOid);
}
