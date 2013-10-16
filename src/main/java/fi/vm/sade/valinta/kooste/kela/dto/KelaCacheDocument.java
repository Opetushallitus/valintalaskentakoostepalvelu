package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Date;

public class KelaCacheDocument {

    public static enum KelaTietueTyyppi {
        FILE, INFO, ERROR
    }

    private final KelaTietueTyyppi type;
    private final String header;
    private final long amount;
    private final byte[] data;
    private final Date createdAt;

    private KelaCacheDocument(KelaTietueTyyppi type, String header, long amount, byte[] data) {
        this.type = type;
        this.header = header;
        this.amount = amount;
        this.data = data;
        this.createdAt = new Date();
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public byte[] getData() {
        return data;
    }

    public String getHeader() {
        return header;
    }

    public long getAmount() {
        return amount;
    }

    public KelaTietueTyyppi getType() {
        return type;
    }

    public static KelaCacheDocument createFile(String text, long amount, byte[] data) {
        return new KelaCacheDocument(KelaTietueTyyppi.FILE, text, amount, data);
    }

    public static KelaCacheDocument createInfoMessage(String text) {
        return new KelaCacheDocument(KelaTietueTyyppi.INFO, text, 0, null);
    }

    public static KelaCacheDocument createErrorMessage(String text) {
        return new KelaCacheDocument(KelaTietueTyyppi.ERROR, text, 0, null);
    }
}
