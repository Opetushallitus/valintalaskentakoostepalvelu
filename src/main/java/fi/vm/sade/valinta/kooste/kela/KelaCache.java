package fi.vm.sade.valinta.kooste.kela;

import java.util.SortedSet;

import org.apache.commons.lang.time.FastDateFormat;

import fi.vm.sade.valinta.kooste.kela.dto.KelaCacheDocument;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHeader;

public interface KelaCache {
    static final FastDateFormat FORMATTER = FastDateFormat.getInstance("dd.MM.yyyy HH.mm");

    SortedSet<KelaHeader> getHeaders();

    KelaCacheDocument getDocument(String documentId);

    void addDocument(KelaCacheDocument newDocument);
}
