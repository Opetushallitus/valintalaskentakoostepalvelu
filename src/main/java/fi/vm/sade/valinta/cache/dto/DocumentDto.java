package fi.vm.sade.valinta.cache.dto;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.time.FastDateFormat;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public final class DocumentDto {
    static final FastDateFormat FORMATTER = FastDateFormat.getInstance("dd.MM.yyyy HH.mm");

    private final String mimeType;
    private final String documentId;
    private final String filename;
    private final Date createdAt;
    private final Date expirationDate;
    private final String serviceName;
    private final String documentType;
    private final Map<String, String> data;
    private final long size;

    public DocumentDto(String documentId, String filename, String mimeType, String serviceName, String documentType,
            Date createdAt, Date expirationDate, Map<String, String> data, long size) {
        this.documentId = documentId;
        this.mimeType = mimeType;
        this.expirationDate = expirationDate;
        this.filename = filename;
        this.data = data;
        this.serviceName = serviceName;
        this.documentType = documentType;
        this.size = size;
        this.createdAt = createdAt;
    }

    public Map<String, String> getData() {
        return data;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getExpirationDate() {
        return FORMATTER.format(expirationDate);
    }

    public String getFilename() {
        return filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getSize() {
        return humanReadableByteCount(size, true);
    }

    public String getCreatedAt() {
        return FORMATTER.format(createdAt);
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

}
