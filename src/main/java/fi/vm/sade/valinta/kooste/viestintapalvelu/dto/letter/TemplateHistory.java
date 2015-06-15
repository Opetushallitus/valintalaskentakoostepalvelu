package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

import java.util.List;

public class TemplateHistory {

    private String name;
    private List<TemplateDetail> templateReplacements;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTemplateReplacements(List<TemplateDetail> templateReplacements) {
        this.templateReplacements = templateReplacements;
    }

    public List<TemplateDetail> getTemplateReplacements() {
        return templateReplacements;
    }
}
