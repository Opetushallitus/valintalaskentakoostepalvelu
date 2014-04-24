package fi.vm.sade.valinta.kooste.excel.arvo;

import java.util.Collection;

public class BooleanArvo extends MonivalintaArvo {

	private final String tosi;
	private final String epatosi;

	public BooleanArvo(String arvo, Collection<String> valinnat, String tosi,
			String epatosi, String tyhja) {
		super(asString(asBoolean(arvo, tosi, epatosi), tosi, epatosi, tyhja),
				valinnat);
		this.tosi = tosi;
		this.epatosi = epatosi;
	}

	public static String asString(Boolean arvo, String tosi, String epatosi,
			String tyhja) {
		if (Boolean.TRUE.equals(arvo)) {
			return tosi;
		} else if (Boolean.FALSE.equals(arvo)) {
			return epatosi;
		} else {
			return tyhja;
		}
	}

	public static Boolean asBoolean(String arvo, String tosi, String epatosi) {
		if (tosi.equals(arvo)) {
			return Boolean.TRUE;
		} else if (epatosi.equals(arvo)) {
			return Boolean.FALSE;
		} else {
			return null;
		}
	}

	public Boolean asBoolean() {
		return asBoolean(getArvo(), this.tosi, this.epatosi);
	}
}
