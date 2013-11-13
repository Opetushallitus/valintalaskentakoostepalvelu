package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Date;

import fi.vm.sade.valinta.kooste.kela.KelaCache;

public class KelaHeader implements Comparable<KelaHeader> {

    private final String documentId;
    private final String type;
    private final String header;
    private final long amount;
    private final long size;
    private final Date createdAt;

    private KelaHeader(String documentId, String type, String header, long amount, long size, Date createdAt) {
        this.documentId = documentId;
        this.type = type;
        this.header = header;
        this.amount = amount;
        this.size = size;
        this.createdAt = createdAt;
    }

    public int compareTo(KelaHeader o) {
        return o.createdAt.compareTo(createdAt);
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getType() {
        return type;
    }

    public long getAmount() {
        return amount;
    }

    public String getHeader() {
        return header;
    }

    public String getSize() {
        return humanReadableByteCount(size, true);
    }

    public String getCreatedAt() {
        return KelaCache.FORMATTER.format(createdAt);
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static KelaHeader createHeader(String documentId, KelaCacheDocument tietue) {
        long len = 0L;
        if (KelaCacheDocument.KelaTietueTyyppi.FILE.equals(tietue.getType())) {
            len = tietue.getData().length;
        }
        return new KelaHeader(documentId, tietue.getType().toString(), tietue.getHeader(), tietue.getAmount(), len,
                tietue.getCreatedAt());
    }
}
