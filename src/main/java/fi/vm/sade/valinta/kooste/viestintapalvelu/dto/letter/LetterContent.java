package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

import java.util.Date;

public class LetterContent {
    private byte[] content;
    private String contentType = "";
    private Date timestamp;

    public LetterContent() {
    }

    public LetterContent(byte[] content, String contentType, Date timestamp) {
        super();
        this.content = content;
        this.contentType = contentType;
        this.timestamp = timestamp;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
