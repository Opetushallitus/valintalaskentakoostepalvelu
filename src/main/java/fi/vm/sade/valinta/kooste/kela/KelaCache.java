package fi.vm.sade.valinta.kooste.kela;

import fi.vm.sade.valinta.kooste.kela.dto.KelaCacheDocument;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHeader;
import org.apache.commons.lang.time.FastDateFormat;

import java.util.SortedSet;

public interface KelaCache {
    static final FastDateFormat FORMATTER = FastDateFormat.getInstance("dd.MM.yyyy HH.mm");

    SortedSet<KelaHeader> getHeaders();

    KelaCacheDocument getDocument(String documentId);

    void addDocument(KelaCacheDocument newDocument);
}
