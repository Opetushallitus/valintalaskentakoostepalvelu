package fi.vm.sade.valinta.kooste.erillishaku.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Before;
import org.junit.Test;

import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.integrationtest.SharedTomcat;
import fi.vm.sade.valinta.kooste.ValintaKoosteTomcat;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuDataRivi;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ImportedErillisHakuExcel;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.mocks.MockDokumenttiResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;

public class ErillishakuResourceTest {
    String hakuOid = "1.2.246.562.5.2013080813081926341928";
    String hakukohdeOid = "1.2.246.562.5.72607738902";
    String tarjoajaOid = "1.2.246.562.10.591352080610";
    String valintatapajonoOid = "14090336922663576781797489829886";
    final String root = "http://localhost:" + SharedTomcat.port + "/valintalaskentakoostepalvelu/resources";

    @Before
    public void startServer() {
        ValintaKoosteTomcat.startShared();
    }

    @Test
    public void vientiExcelTiedostoon() throws IOException {
        final String url = root + "/erillishaku/vienti";
        final ProsessiId prosessiId = createClient(url)
            .query("hakutyyppi", "KORKEAKOULU")
            .query("hakuOid", hakuOid)
            .query("hakikohdeOid", hakukohdeOid)
            .query("tarjoajaOid", tarjoajaOid)
            .query("valintatapajonoOid", valintatapajonoOid)
            .query("valintatapajononNimi", "varsinainen jono")
            .type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(Arrays.asList(), MediaType.APPLICATION_JSON), ProsessiId.class);

        String documentId = odotaDokumenttiaJaPalautaId(prosessiId);
        final InputStream storedDocument = MockDokumenttiResource.getStoredDocument(documentId);
        assertNotNull(storedDocument);
        verifyCreatedExcelDocument(storedDocument);
    }

    private void verifyCreatedExcelDocument(final InputStream storedDocument) throws IOException {
        final ImportedErillisHakuExcel tulos = new ImportedErillisHakuExcel(Hakutyyppi.KORKEAKOULU, storedDocument);
        assertEquals(1, tulos.henkiloPrototyypit.size());
        final HenkiloCreateDTO expectedHenkilo = new HenkiloCreateDTO("etunimi", "sukunimi", "010101-123N", ErillishakuDataRivi.SYNTYMAAIKA.parseDateTime("1.1.1901").toDate(), HenkiloTyyppi.OPPIJA);
        assertEquals(expectedHenkilo, tulos.henkiloPrototyypit.get(0));
    }

    private String odotaDokumenttiaJaPalautaId(final ProsessiId prosessiId) {
        final Prosessi dokumenttiProsessi = createClient(root + "/dokumenttiprosessi/" + prosessiId.getId())
            .accept(MediaType.APPLICATION_JSON).get(Prosessi.class);
        if (!dokumenttiProsessi.valmis()) {
            return odotaDokumenttiaJaPalautaId(prosessiId);
        }
        return dokumenttiProsessi.dokumenttiId;
    }

    private WebClient createClient(String url) {
        return new HttpResource(url, 1000).webClient;
    }


    // Simple data transfer object (deserialization of DocumenttiProsessi doesn't work)
    static class Prosessi {
        public Osatyo kokonaistyo = new Osatyo();
        public String dokumenttiId;

        public boolean valmis() {
            return dokumenttiId != null;
        }

        static class Osatyo {
            public int tehty = 0;
            public int kokonaismaara = 0;

            public boolean valmis() {
                return tehty == 1;
            }
        }
    }
}
