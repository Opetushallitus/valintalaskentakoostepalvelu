package fi.vm.sade.valinta.kooste.kela.komponentti;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Collections2;
import fi.vm.sade.valinta.kooste.kela.KelaCache;
import fi.vm.sade.valinta.kooste.kela.dto.KelaCacheDocument;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class KelaCacheImpl implements KelaCache {

    private static final Logger LOG = LoggerFactory.getLogger(KelaCacheImpl.class);
    private Cache<String, KelaCacheDocument> downloads = CacheBuilder.newBuilder().expireAfterWrite(12, TimeUnit.HOURS)
            .build();

    @Override
    public void addDocument(KelaCacheDocument newDocument) {
        String documentId = UUID.randomUUID().toString();
        downloads.put(documentId, newDocument);
    }

    public KelaCacheDocument getDocument(String documentId) {
        KelaCacheDocument download = downloads.getIfPresent(documentId);
        if (download != null) {
            downloads.invalidate(download);
        }
        return download;
    }

    public SortedSet<KelaHeader> getHeaders() {
        return new TreeSet<KelaHeader>(Collections2.transform(downloads.asMap().entrySet(),
                new Function<Entry<String, KelaCacheDocument>, KelaHeader>() {
                    public KelaHeader apply(Entry<String, KelaCacheDocument> o) {
                        return KelaHeader.createHeader(o.getKey(), o.getValue());
                    }
                }));
    }
}
