package fi.vm.sade.valinta.kooste.kela.komponentti;

import static fi.vm.sade.valinta.kooste.kela.route.KelaRoute.PROPERTY_AINEISTONNIMI;
import static fi.vm.sade.valinta.kooste.kela.route.KelaRoute.PROPERTY_ORGANISAATIONNIMI;

import java.io.InputStream;
import java.util.Collection;

import org.apache.camel.Body;
import org.apache.camel.Property;

import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;

public interface KelaDokumentinLuontiKomponentti {

    InputStream luo(@Body Collection<TKUVAYHVA> rivit,
    //
            @Property(PROPERTY_AINEISTONNIMI) String aineistonNimi,
            //
            @Property(PROPERTY_ORGANISAATIONNIMI) String organisaationNimi);
}
