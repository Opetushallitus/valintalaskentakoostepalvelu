package fi.vm.sade.valinta.cache.resource.impl;

import java.io.InputStream;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import fi.vm.sade.valinta.cache.domain.MetaData;
import fi.vm.sade.valinta.cache.dto.DocumentDto;
import fi.vm.sade.valinta.cache.resource.DocumentCacheResource;
import fi.vm.sade.valinta.cache.service.DocumentCacheService;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class DocumentCacheResourceImpl implements DocumentCacheResource {

    @Autowired
    private DocumentCacheService documentCacheService;

    @Override
    public Collection<DocumentDto> hae() {
        return Collections2.transform(documentCacheService.getAll(), new Function<MetaData, DocumentDto>() {
            public DocumentDto apply(MetaData input) {
                return input.toDocumentDto();
            }
        });
    }

    @Override
    public Collection<DocumentDto> hae(String serviceName, String documentType) {
        return Collections2.transform(documentCacheService.search(serviceName, documentType),
                new Function<MetaData, DocumentDto>() {
                    public DocumentDto apply(MetaData input) {
                        return input.toDocumentDto();
                    }
                });
    }

    @Override
    public InputStream lataa(String documentId) {
        return documentCacheService.download(documentId);
    }

    @Override
    public void tallenna(MetaData metaData, InputStream tiedosto) {
        documentCacheService.save(metaData, tiedosto);
    }
}
