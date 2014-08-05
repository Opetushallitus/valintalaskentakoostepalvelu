package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.Util;

/**
 * 
 * @author Jussi Jartamo
 * 
 * Maski osittaiseen laskentaan haulle
 */
public class Maski {
	private final static Logger LOG = LoggerFactory
			.getLogger(Maski.class);
	private final static String NIMI_FORMAT = "Maski whitelist(%s) blacklist(%s) \r\nMask(%s)";
	private final Set<String> hakukohdeOidsMask;
	private final boolean whiteList;
	
	public Maski() {
		this.whiteList= false;
		this.hakukohdeOidsMask = null;
	}
	
	public Maski(boolean whitelist, Set<String> hakukohdeOidsMask) {
		this.whiteList = whitelist;
		this.hakukohdeOidsMask = hakukohdeOidsMask;
	}
	
	public boolean isMask() {
		return hakukohdeOidsMask != null && !hakukohdeOidsMask.isEmpty();
	}

	public boolean isBlacklist() {
		return !whiteList && isMask();
	}

	public boolean isWhitelist() {
		return whiteList && isMask();
	}
	public Collection<String> maskaa(Collection<String> original) {
		Collection<String> lopulliset = original;
		if (isMask()) {
			if (isWhitelist()) {
				lopulliset = Util.whitelist(
						hakukohdeOidsMask,
						original,
						(s -> LOG
								.error("Haku ei taysin vastaa syotetyn whitelistin hakukohteita! Puuttuvat hakukohteet \r\n{}",
										Arrays.toString(s
												.toArray()))));
			} else if (isBlacklist()) {
				lopulliset = Util
						.blacklist(
								hakukohdeOidsMask,
								original,
								(s -> LOG
										.error("Haku ei taysin vastaa syotetyn blacklistin hakukohteita! Ylimaaraiset hakukohteet \r\n{}",
												Arrays.toString(s
														.toArray()))));
			}
		}
		return lopulliset;
	}
	public String toString() {
		if(hakukohdeOidsMask == null) {
			return String.format(NIMI_FORMAT, isWhitelist(), StringUtils.EMPTY);	
		}
		return String.format(NIMI_FORMAT, isWhitelist(),
				isBlacklist(), Arrays.toString(hakukohdeOidsMask.toArray()));
	}
}
