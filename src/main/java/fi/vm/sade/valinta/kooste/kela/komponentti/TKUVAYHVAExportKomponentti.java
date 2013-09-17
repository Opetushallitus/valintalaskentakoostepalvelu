package fi.vm.sade.valinta.kooste.kela.komponentti;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;

@Component("TKUVAYHVAKomponentti")
public class TKUVAYHVAExportKomponentti {

    @Autowired
    private SijoitteluResource sijoitteluResource;

    public InputStream luoTKUVAYHVA(@Property("hakuOid") String hakuOid) {

        List<InputStream> streams = new ArrayList<InputStream>();
        // TODO: Odottaa valmiiksi rakennettua sijoittelurajapintaa
        // Koko haun hakeminen on tarpeetonta!
        // SijoitteluajoDTO ajo = sijoitteluResource.getSijoitteluajo(hakuOid,
        // SijoitteluResource.LATEST);
        // for (HakukohdeDTO hakukohde : ajo.getHakukohteet()) {
        // TKUVAYHVA.Builder builder = new TKUVAYHVA.Builder();
        //
        // streams.add(new ByteArrayInputStream(builder.build().toByteArray()));
        // break;
        // }
        return new SequenceInputStream(Collections.enumeration(streams));
    }
}
