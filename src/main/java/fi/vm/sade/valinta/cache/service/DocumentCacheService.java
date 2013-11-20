package fi.vm.sade.valinta.cache.service;

import java.io.InputStream;
import java.util.Collection;

import fi.vm.sade.valinta.cache.domain.MetaData;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface DocumentCacheService {

    Collection<MetaData> getAll();

    Collection<MetaData> search(String serviceName, String documentType);

    InputStream download(String documentId);

    void save(MetaData metaData, InputStream file);

}
