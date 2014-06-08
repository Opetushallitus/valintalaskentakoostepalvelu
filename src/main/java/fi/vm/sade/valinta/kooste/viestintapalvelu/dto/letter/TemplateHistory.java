package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

import java.util.List;

public class TemplateHistory {

	private String name;
	private List<TemplateDetail> templateReplacements;

	public String getName() {
		return name;
	}

	public List<TemplateDetail> getTemplateReplacements() {
		return templateReplacements;
	}
}
