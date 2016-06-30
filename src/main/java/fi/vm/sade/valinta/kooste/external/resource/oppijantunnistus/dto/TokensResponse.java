package fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto;

import java.util.List;

public class TokensResponse {
    private List<Recipient> recipients;

    public List<Recipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<Recipient> recipients) {
        this.recipients = recipients;
    }

    @Override
    public String toString() {
        return "TokensResponse{" +
                "recipients=" + recipients +
                '}';
    }
}
