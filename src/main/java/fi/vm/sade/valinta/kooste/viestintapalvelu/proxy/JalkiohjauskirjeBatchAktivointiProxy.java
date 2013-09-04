package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface JalkiohjauskirjeBatchAktivointiProxy {

    String jalkiohjauskirjeetAktivoi(String hakukohdeOid, String hakuOid, Long sijoitteluajoId);
}
