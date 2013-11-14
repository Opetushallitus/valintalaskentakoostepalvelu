package fi.vm.sade.valinta.cache.domain;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class MetaData {

    private String mimeType;
    private String documentId;
    private String filename;
    private Date expirationDate;
    private String serviceName; // viestintapalvelu, koostepalvelu, ...
    private String documentType; // hyvaksymiskirje, jalkiohjauskirje, ...
    private Map<String, String> data;
    private long size;

    public MetaData(String documentId, String filename, String mimeType, String serviceName, String documentType,
            Date expirationDate, Map<String, String> data, long size) {
        this.documentId = documentId;
        this.mimeType = mimeType;
        this.expirationDate = expirationDate;
        this.filename = filename;
        this.data = data;
        this.serviceName = serviceName;
        this.documentType = documentType;
        this.size = size;
    }

    public Map<String, String> getData() {
        return data;
    }

    public long getSize() {
        return size;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getFilename() {
        return filename;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getMimeType() {
        return mimeType;
    }

    /**
     * Creates mimetype from file extension. Expiration date as time-to-live
     * hours
     * 
     * Usage:
     * 
     * MetaData.newBuilder().setHoursUntilExpire(5)
     * .setData(mapWithHakukohdeOidHakuOid...)
     * .setSearchKey("viestintapalvelu.hyvaksymiskirje")
     * .setFilename("hyvaksymiskirje.pdf").build();
     */
    public static class MetaDataBuilder {

        private static final int ONE_DAY = 1;

        private String mimeType;
        private String documentId;
        private String filename;
        private Date expirationDate;
        private Map<String, String> data;
        private String serviceName;
        private String documentType;
        private long size;

        public MetaDataBuilder() {
        }

        public MetaDataBuilder setHoursUntilExpire(int hours) {
            this.expirationDate = DateTime.now().plusHours(hours).toDate();
            return this;
        }

        public MetaDataBuilder setExpirationDate(Date expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public MetaDataBuilder setSize(long size) {
            this.size = size;
            return this;
        }

        public MetaDataBuilder setFilename(String filename) {
            this.filename = filename;
            return this;
        }

        public MetaDataBuilder setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public MetaDataBuilder setDocumentType(String documentType) {
            this.documentType = documentType;
            return this;
        }

        // @Optional
        public MetaDataBuilder setDocumentId(String documentId) {
            this.documentId = documentId;
            return this;
        }

        public MetaDataBuilder setData(Map<String, String> data) {
            this.data = data;
            return this;
        }

        public MetaData build() {
            if (documentId == null) { // generate random id
                documentId = UUID.randomUUID().toString();
            }
            if (mimeType == null) { // file extension to mimetype
                String extension = getExtension(filename);
                if ("pdf".equalsIgnoreCase(extension)) {
                    mimeType = "application/pdf";
                } else if ("xls".equalsIgnoreCase(extension)) {
                    mimeType = "application/vnd.ms-excel";
                }
            }
            if (data == null) {
                data = Collections.emptyMap();
            }
            if (expirationDate == null) {
                expirationDate = DateTime.now().plusDays(ONE_DAY).toDate();
            }
            return new MetaData(documentId, filename, mimeType, serviceName, documentType, expirationDate, data, size);
        }
    }

    private static String getExtension(String filename) {
        String dotSeparated[] = filename.split(".");
        return dotSeparated[dotSeparated.length - 1];
    }

    public static MetaDataBuilder newBuilder() {
        return new MetaDataBuilder();
    }
}
