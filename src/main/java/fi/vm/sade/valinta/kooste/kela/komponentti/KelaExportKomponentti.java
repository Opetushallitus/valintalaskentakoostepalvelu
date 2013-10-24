package fi.vm.sade.valinta.kooste.kela.komponentti;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.List;

import org.apache.camel.Body;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAALKU;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVALOPPU;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;
import fi.vm.sade.valinta.kooste.kela.KelaCache;
import fi.vm.sade.valinta.kooste.kela.dto.KelaCacheDocument;

@Component("kelaExportKomponentti")
public class KelaExportKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(KelaExportKomponentti.class);

    @Autowired
    private KelaCache kelaCache;

    public void exportKela(@Body List<TKUVAYHVA> data) throws IOException {
        int count = data.size();
        LOG.info("KELA-dokumentin luonti {} tietueelle", count);

        Deque<InputStream> streams = new ArrayDeque<InputStream>();
        for (TKUVAYHVA t : data) {
            streams.add(new ByteArrayInputStream(t.toByteArray()));
        }
        streams.addFirst(new ByteArrayInputStream(new TKUVAALKU.Builder().setAjopaivamaara(new Date())
                .setAineistonnimi(StringUtils.EMPTY).setOrganisaationimi(StringUtils.EMPTY).build().toByteArray()));
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

        kelaCache.addDocument(KelaCacheDocument.createFile(KelaUtil.createTiedostoNimiYhva14(new Date()), count,
                IOUtils.toByteArray(input)));
        LOG.info("Palautetaan onnistuneesti luotu KELA-tiedosto");
    }
}
