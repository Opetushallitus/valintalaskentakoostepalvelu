package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import java.util.concurrent.Future;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface KoekutsukirjeRoute {

    void koekutsukirjeetAktivointi(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
            @Property(OPH.HAKUOID) String hakuOid, @Property(OPH.SIJOITTELUAJOID) Long sijoitteluajoId);

    Future<Void> koekutsukirjeetAktivointiAsync(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
            @Property(OPH.HAKUOID) String hakuOid, @Property(OPH.SIJOITTELUAJOID) Long sijoitteluajoId);

    final String DIRECT_KOEKUTSUKIRJEET = "direct:koekutsukirjeet";
}
