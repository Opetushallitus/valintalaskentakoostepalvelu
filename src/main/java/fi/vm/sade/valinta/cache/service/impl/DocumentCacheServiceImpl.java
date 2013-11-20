package fi.vm.sade.valinta.cache.service.impl;

import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import fi.vm.sade.valinta.cache.dao.DocumentDao;
import fi.vm.sade.valinta.cache.domain.MetaData;
import fi.vm.sade.valinta.cache.service.DocumentCacheService;

public class DocumentCacheServiceImpl implements DocumentCacheService {

    @Autowired
    private DocumentDao documentDao;

    @Override
    public List<MetaData> getAll() {
        return null;
    }

    @Override
    public List<MetaData> search(String serviceName, String documentType) {
        return null;
    }

    @Override
    public InputStream download(String documentId) {
        return null;
    }

    @Override
    public void save(MetaData metaData, InputStream file) {
        return;
    }

}
