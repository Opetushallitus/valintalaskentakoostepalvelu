package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import fi.vm.sade.valinta.kooste.valvomo.dto.OsaTyo;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tyo;

/**
 * 
 * @author Jussi Jartamo
 *
 * @Deprecated ei enaa tarpeellinen kun SSE eventit otetaan kayttoon
 */
@Deprecated
public class DokumenttiProsessi extends Prosessi {

	public static final String DOKUMENTTI_PROSESSI = "dokumenttiProsessi";

	private final OsaTyo kokonaistyo;
	private volatile boolean kasittelyssa;
	private AtomicReference<String> dokumenttiId;
	private final List<String> tags;
	private final Collection<Poikkeus> poikkeukset = new CopyOnWriteArrayList<Poikkeus>();
	private final Collection<Varoitus> varoitukset = new CopyOnWriteArrayList<Varoitus>();
	private final transient Collection<Poikkeus> poikkeuksetUudelleenYrityksessa = new CopyOnWriteArrayList<Poikkeus>();

	public DokumenttiProsessi(String resurssi, String toiminto, String hakuOid,
			List<String> tags) {
		super(resurssi, toiminto, hakuOid);
		this.kasittelyssa = false;
		this.dokumenttiId = new AtomicReference<String>(null);
		this.kokonaistyo = new OsaTyo("Kokonaistyö", -1);
		if (tags == null) {
			this.tags = Collections.emptyList();
		} else {
			this.tags = Collections.unmodifiableList(tags);
		}
	}

	public void luovutaUudelleenYritystenKanssa() {
		poikkeukset.addAll(poikkeuksetUudelleenYrityksessa);
	}

	public Collection<Poikkeus> getPoikkeuksetUudelleenYrityksessa() {
		return poikkeuksetUudelleenYrityksessa;
	}

	public Collection<Poikkeus> getPoikkeukset() {
		return poikkeukset;
	}

	public Tyo getKokonaistyo() {
		return kokonaistyo;
	}

	public List<String> getTags() {
		return tags;
	}

	public String getDokumenttiId() {
		return this.dokumenttiId.get();
	}

	public boolean isKeskeytetty() {
		return !getPoikkeukset().isEmpty();
	}

	public void setDokumenttiId(String id) {
		this.dokumenttiId.set(id);
	}

	public void setKasittelyssa() {
		this.kasittelyssa = true;
	}

	public boolean isKasittelyssa() {
		return kasittelyssa;
	}

	public void inkrementoiKokonaistyota() {
		this.kokonaistyo.inkrementoiKokonaismaaraa();
	}

	public void inkrementoiKokonaistyota(int delta) {
		this.kokonaistyo.inkrementoiKokonaismaaraa(delta);
	}

	public int inkrementoiTehtyjaToita() {
		return this.kokonaistyo.tyoValmistui(0L);
	}

	public void inkrementoiOhitettujaToita() {
		this.kokonaistyo.tyoOhitettu();
	}

	public void setKokonaistyo(int arvo) {
		this.kokonaistyo.setKokonaismaara(arvo);
	}

	public ProsessiId toProsessiId() {
		return new ProsessiId(getId());
	}

	public Collection<Varoitus> getVaroitukset() {
		return varoitukset;
	}

}
