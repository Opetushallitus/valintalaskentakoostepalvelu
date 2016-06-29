package fi.vm.sade.valinta.kooste.external.resource.oppijantunnistus.dto;

import java.util.Map;

public class TokensRequest {
    private String url;
    private String hakuOid;
    private String templatename;
    private String lang;
    private Map<String, String> applicationOidToEmailAddress;
    private Long expires;

    public TokensRequest() {

    }

    public TokensRequest(String url, String templatename, String lang, Map<String, String> applicationOidToEmailAddress, Long expires) {
        this.url = url;
        this.templatename = templatename;
        this.lang = lang;
        this.applicationOidToEmailAddress = applicationOidToEmailAddress;
        this.expires = expires;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTemplatename() {
        return templatename;
    }

    public void setTemplatename(String templatename) {
        this.templatename = templatename;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Map<String, String> getApplicationOidToEmailAddress() {
        return applicationOidToEmailAddress;
    }

    public void setApplicationOidToEmailAddress(Map<String, String> applicationOidToEmailAddress) {
        this.applicationOidToEmailAddress = applicationOidToEmailAddress;
    }

    public Long getExpires() {
        return expires;
    }

    public void setExpires(Long expires) {
        this.expires = expires;
    }

    public String getHakuOid() {
        return hakuOid;
    }

    public void setHakuOid(String hakuOid) {
        this.hakuOid = hakuOid;
    }

    @Override
    public String toString() {
        return "TokensRequest{" +
                "url='" + url + '\'' +
                ", hakuOid='" + hakuOid + '\'' +
                ", templatename='" + templatename + '\'' +
                ", lang='" + lang + '\'' +
                ", applicationOidToEmailAddress=" + applicationOidToEmailAddress +
                ", expires=" + expires +
                '}';
    }
}








/*
(s/defschema TokensRequest {:url (rs/describe s/Str "Base URL for secure links.")
                            :templatename (rs/describe s/Str "Template name for email. Template with this name should exist in Viestintäpalvelu and it must have replacement with name 'securelink'")
                            :lang (rs/describe s/Str "Email language in ISO-639-1 format. E.g. 'en','fi','sv'.")
                            :emails (rs/describe [s/Str] "List of recipient email addresses")
                            (s/optional-key :expires) (rs/describe Long "Expiration date as unix timestamp (long milliseconds).")
                            (s/optional-key :metadata) (s/conditional map? {s/Keyword s/Keyword})})
(s/defschema TokensResponse {:recipients [{:email s/Str
                                           :securelink s/Str}]} )
 */
