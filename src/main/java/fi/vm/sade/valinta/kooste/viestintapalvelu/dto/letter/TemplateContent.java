package fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter;

import java.util.Date;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class TemplateContent implements Comparable<TemplateContent> {

	private Long id;
	private String name;
	private int order;
	private String content;
	private Date timestamp;

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "TemplateContent [order=" + order + ", name=" + name
				+ ", content=" + content + ", timestamp=" + timestamp + ", id="
				+ id + "]";
	}

	@Override
	public int compareTo(TemplateContent o) {
		Integer ord = new Integer(order);
		return ord.compareTo(o.order);
	}

}
