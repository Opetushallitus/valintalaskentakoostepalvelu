package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import static fi.vm.sade.valinta.kooste.kela.route.KelaRoute.PROPERTY_AINEISTONNIMI;
import static fi.vm.sade.valinta.kooste.kela.route.KelaRoute.PROPERTY_ORGANISAATIONNIMI;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;

import org.apache.camel.Body;
import org.apache.camel.Property;
import org.springframework.stereotype.Component;

import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAALKU;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVALOPPU;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;
import fi.vm.sade.valinta.kooste.kela.komponentti.KelaDokumentinLuontiKomponentti;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Voi joutua refaktoroimaan sadantuhannen kanssa niin etta riveja
 *         luotaessa stream on auki dokumenttipalveluun
 */
@Component
public class KelaDokumentinLuontiKomponenttiImpl implements KelaDokumentinLuontiKomponentti {

    @Override
    public InputStream luo(@Body Collection<TKUVAYHVA> rivit,
    //
            @Property(PROPERTY_AINEISTONNIMI) String aineistonNimi,
            //
            @Property(PROPERTY_ORGANISAATIONNIMI) String organisaationNimi) {
        int count = rivit.size();

        Deque<InputStream> streams = new ArrayDeque<InputStream>();
        for (TKUVAYHVA t : rivit) {
            streams.add(new ByteArrayInputStream(t.toByteArray()));
        }
        streams.addFirst(new ByteArrayInputStream(new TKUVAALKU.Builder().setAjopaivamaara(new Date())
                .setAineistonnimi(aineistonNimi).setOrganisaationimi(organisaationNimi).build().toByteArray()));
        streams.addLast(new ByteArrayInputStream(new TKUVALOPPU.Builder().setAjopaivamaara(new Date())
                .setTietuelukumaara(count).build().toByteArray()));
        //
        // Add line ending '\n' between every stream
        //
        Collection<InputStream> withLineEndings = new ArrayList<InputStream>();
        for (InputStream stream : streams) {
            withLineEndings.add(new SequenceInputStream(stream, new ByteArrayInputStream(KelaUtil.RIVINVAIHTO)));
        }
        InputStream input = new SequenceInputStream(Collections.enumeration(withLineEndings));
        return input;
    }
}
