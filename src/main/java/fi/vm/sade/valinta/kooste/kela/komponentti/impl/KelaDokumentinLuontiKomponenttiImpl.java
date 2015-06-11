package fi.vm.sade.valinta.kooste.kela.komponentti.impl;

import static fi.vm.sade.valinta.kooste.kela.route.KelaRoute.PROPERTY_AINEISTONNIMI;
import static fi.vm.sade.valinta.kooste.kela.route.KelaRoute.PROPERTY_ORGANISAATIONNIMI;
import static fi.vm.sade.valinta.kooste.kela.route.KelaRoute.PROPERTY_SIIRTOTUNNUS;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;

import org.apache.camel.Body;
import org.apache.camel.Property;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Component;

import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAALKU;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVALOPPU;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;

/**
 *         Voi joutua refaktoroimaan sadantuhannen kanssa niin etta riveja
 *         luotaessa stream on auki dokumenttipalveluun
 */
@Component
public class KelaDokumentinLuontiKomponenttiImpl {

    public byte[] luo(@Body Collection<TKUVAYHVA> rivit,
                      //
                      @Property(PROPERTY_AINEISTONNIMI) String aineistonNimi,
                      //
                      @Property(PROPERTY_ORGANISAATIONNIMI) String organisaationNimi,
                      //
                      @Property(PROPERTY_SIIRTOTUNNUS) String siirtotunnus)
            throws Exception {

        int count = rivit.size();

        Deque<InputStream> streams = new ArrayDeque<InputStream>();

        for (TKUVAYHVA t : rivit) {
            streams.add(new ByteArrayInputStream(addLineEnding(t.toByteArray(), createBuffer())));
        }

        Date ajopvm = new Date();
        streams.addFirst(new ByteArrayInputStream(addLineEnding(
                new TKUVAALKU.Builder().setAjopaivamaara(ajopvm)
                        .setSiirtotunnus(siirtotunnus)
                        .setAineistonnimi(aineistonNimi)
                        .setOrganisaationimi(organisaationNimi).build()
                        .toByteArray(), createBuffer())));
        streams.addLast(new ByteArrayInputStream(addLineEnding(
                new TKUVALOPPU.Builder().setAjopaivamaara(ajopvm)
                        .setSiirtotunnus(siirtotunnus)
                        .setTietuelukumaara(count).build().toByteArray(),
                createBuffer())));

        InputStream input = new SequenceInputStream(Collections.enumeration(streams));
        return IOUtils.toByteArray(input);
    }

    private ByteBuffer createBuffer() {
        return ByteBuffer.allocate(200 + KelaUtil.RIVINVAIHTO.length);
    }

    //
    // Add line ending '\n' between every stream
    //
    private byte[] addLineEnding(byte[] tietue, ByteBuffer buffer) {
        buffer.clear();
        buffer.put(tietue);
        buffer.put(KelaUtil.RIVINVAIHTO);
        return buffer.array();
    }
}
