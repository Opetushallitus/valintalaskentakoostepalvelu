package fi.vm.sade.valinta.kooste.erillishaku.excel.mocks;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.Message;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class MockDokumenttiResource implements DokumenttiResource {
    @Override
    public Collection<MetaData> hae(List<String> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<MetaData> yllapitohaku(List<String> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Response lataa(String documentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void tallenna(String id, String filename, Long expirationDate, List<String> tags, String mimeType, InputStream filedata) {

    }

    @Override
    public void viesti(Message message) {

    }

    @Override
    public void tyhjenna() {

    }

    @Override
    public Collection<MetaData> osoitetarrat(String hakukohdeOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<MetaData> hyvaksymiskirjeet(String hakukohdeOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<MetaData> sijoitteluntulokset(String hakukohdeOid) {
        throw new UnsupportedOperationException();
    }
}
