package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Säilöö guava cacheen prosessit softreferensseinä -- eli ei taetta
 *         että prosessi on muistissa reitin päättymisen jälkeen
 */
@Component
public class DokumenttiProsessiKomponentti {

	private static final Logger LOG = LoggerFactory
			.getLogger(DokumenttiProsessiKomponentti.class);
	private final Cache<String, DokumenttiProsessi> prosessit;

	public DokumenttiProsessiKomponentti() {
		this.prosessit = CacheBuilder.newBuilder()
		// soft valuet poistetaan ennen muistin loppumista
				.softValues()
				// .expireAfterWrite(1L, TimeUnit.HOURS)
				.build();
	}

	public DokumenttiProsessi haeProsessi(String id) {
		if (id == null) {
			LOG.error("Yritetään hakea null-id:llä!");
			throw new RuntimeException(
					"null id:llä hakua dokumenttiprosesseista ei sallita!");
		}
		return prosessit.getIfPresent(id);
	}

	public void tuoUusiProsessi(DokumenttiProsessi prosessi) {
		if (prosessi == null || prosessi.getId() == null) {
			LOG.error("Yritetään tuoda null-prosessia!");
			throw new RuntimeException(
					"null prosessin talletusta dokumenttiprosesseihin ei sallita!");
		}
		prosessit.put(prosessi.getId(), prosessi);
	}

	public void poistaProsessi(String id) {
		prosessit.invalidate(id);
	}
}
