package fi.vm.sade.valinta.kooste.viestintapalvelu;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface AddressLabelBatchAktivointiProxy {

    String addressLabelBatchAktivointi(String hakukohdeOid, List<String> valintakoeOid);
}
