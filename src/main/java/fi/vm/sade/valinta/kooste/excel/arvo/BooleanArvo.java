package fi.vm.sade.valinta.kooste.excel.arvo;

import java.util.Collection;

public class BooleanArvo extends MonivalintaArvo {

	public BooleanArvo(String arvo, Collection<String> valinnat, String tosi,
			String epatosi, String tyhja) {
		super(asString(asBoolean(arvo), tosi, epatosi, tyhja), valinnat);
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

	public static Boolean asBoolean(String arvo) {
		return Boolean.valueOf(arvo);
	}

	public Boolean asBoolean() {
		return asBoolean(getArvo());
	}
}
