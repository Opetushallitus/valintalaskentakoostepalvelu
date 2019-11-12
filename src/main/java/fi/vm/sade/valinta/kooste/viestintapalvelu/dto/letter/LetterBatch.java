package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

import fi.vm.sade.valinta.kooste.viestintapalvelu.model.types.ContentStructureType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LetterBatch {
    private List<Letter> letters;
    private Map<String, Object> templateReplacements;
    private String templateName;
    private String languageCode;
    private String storingOid;
    private String organizationOid;
    private String applicationPeriod;
    private String fetchTarget;
    private String tag;
    private boolean iposti = false;
    private boolean skipDokumenttipalvelu = false;
    private final List<ContentStructureType> contentStructureTypes;

    public Map<String, Object> getTemplateReplacements() {
        return templateReplacements;
    }

    public void setTemplateReplacements(Map<String, Object> templateReplacements) {
        this.templateReplacements = templateReplacements;
    }

    public void setLetters(List<Letter> letters) {
        this.letters = letters;
    }

    public LetterBatch(List<Letter> letters, List<ContentStructureType> contentStructureTypes) {
        this.letters = letters;
        this.contentStructureTypes = contentStructureTypes;
    }

    public List<Letter> getLetters() {
        return letters;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getStoringOid() {
        return storingOid;
    }

    public void setStoringOid(String storingOid) {
        this.storingOid = storingOid;
    }

    public String getOrganizationOid() {
        return organizationOid;
    }

    public void setOrganizationOid(String organizationOid) {
        this.organizationOid = organizationOid;
    }

    public String getApplicationPeriod() {
        return applicationPeriod;
    }

    public void setApplicationPeriod(String applicationPeriod) {
        this.applicationPeriod = applicationPeriod;
    }

    public String getFetchTarget() {
        return fetchTarget;
    }

    public void setFetchTarget(String fetchTarget) {
        this.fetchTarget = fetchTarget;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isSkipDokumenttipalvelu() {
        return skipDokumenttipalvelu;
    }

    public void setSkipDokumenttipalvelu(boolean skipDokumenttipalvelu) {
        this.skipDokumenttipalvelu = skipDokumenttipalvelu;
    }

    public List<ContentStructureType> getContentStructureTypes() {
        return contentStructureTypes;
    }

    public List<LetterBatch> split(int limit) {
        List<LetterBatch> batches = new ArrayList<LetterBatch>();
        split(letters, batches, limit);
        return batches;
    }

    private LetterBatch createSubBatch(List<Letter> lettersOfSubBatch) {
        LetterBatch result = new LetterBatch(
                lettersOfSubBatch,
                Collections.singletonList(ContentStructureType.letter)
        );
        result.setLanguageCode(languageCode);
        result.setApplicationPeriod(applicationPeriod);
        result.setFetchTarget(fetchTarget);
        result.setOrganizationOid(organizationOid);
        result.setStoringOid(storingOid);
        result.setTemplateName(templateName);
        result.setTemplateReplacements(templateReplacements);
        result.setTag(tag);
        return result;
    }

    private void split(List<Letter> remaining, List<LetterBatch> batches,
                       int limit) {
        if (limit >= remaining.size()) {
            batches.add(createSubBatch(remaining));
        } else {
            batches.add(createSubBatch(new ArrayList<Letter>(remaining.subList(0, limit))));
            split(remaining.subList(limit, remaining.size()), batches, limit);
        }
    }

    @Override
    public String toString() {
        return "LetterBatch [letters=" + letters + ", template="
                + ", templateId=" + ", templateReplacements="
                + templateReplacements + ", templateName=" + templateName
                + ", languageCode=" + languageCode + ", storingOid="
                + storingOid + ", organizationOid=" + organizationOid
                + ", applicationPeriod=" + applicationPeriod + ", fetchTarget="
                + fetchTarget + ", tag=" + tag + ", skipDokumenttipalvelu="
                + skipDokumenttipalvelu + ", contentStructureTypes=" + contentStructureTypes + "]";
    }

    public boolean isIposti() {
        return iposti;
    }

    public void setIposti(boolean iposti) {
        this.iposti = iposti;
    }
}
