package fi.vm.sade.valinta.cache.dao;

import java.io.InputStream;
import java.util.Collection;

import fi.vm.sade.valinta.cache.domain.MetaData;

public interface DocumentDao {

    void put(MetaData documentMetaData, InputStream document);

    Collection<MetaData> getAll();
}
