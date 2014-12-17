package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.Message;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class MockDokumenttiResource implements DokumenttiResource {
    private static Map<String, InputStream> docs = new HashMap<>();

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
        docs.put(id, filedata);
    }

    @Override
    public void viesti(Message message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void tyhjenna() {
        throw new UnsupportedOperationException();
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

    public final static InputStream getStoredDocument(String id) {
        if (!docs.containsKey(id)) {
            throw new IllegalStateException("Doc " + id + " not found");
        }
        return docs.get(id);
    }
}
