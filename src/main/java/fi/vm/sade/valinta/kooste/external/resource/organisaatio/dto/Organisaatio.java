package fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto;

public class Organisaatio {
	private String oid;
	private String parentOid;
	private Metadata metadata;
	
	public Metadata getMetadata() {
		return metadata;
	}
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
	public String getOid() {
		return oid;
	}
	public void setOid(String oid) {
		this.oid = oid;
	}
	public String getParentOid() {
		return parentOid;
	}
	public void setParentOid(String parentOid) {
		this.parentOid = parentOid;
	}
}
