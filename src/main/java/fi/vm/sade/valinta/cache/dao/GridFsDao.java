package fi.vm.sade.valinta.cache.dao;

import java.io.InputStream;
import java.util.List;

import fi.vm.sade.valinta.cache.domain.MetaData;

public interface GridFsDao {

    void put(MetaData documentMetaData, InputStream document);

    List<MetaData> getAll();
}
