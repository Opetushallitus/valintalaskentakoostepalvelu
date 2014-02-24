package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;

public class ValintalaskentaMuistissaProsessi extends Prosessi {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaMuistissaProsessi.class);
	private final TyoImpl tarjonnastaHakukohteet;
	private final TyoImpl hakukohteilleHakemukset;
	private final TyoImpl hakemukset;
	private final TyoImpl valintaperusteet;
	private final TyoImpl valintalaskenta;
	private final Tyo kokonaistyo;
	private final boolean peruutaProsessiPoikkeuksesta;
	@JsonIgnore
	private List<Palvelukutsu> palvelukutsut = Collections
			.synchronizedList(Lists.<Palvelukutsu> newArrayList());
	@JsonIgnore
	private List<String> kasitellytHakukohteet = Collections
			.synchronizedList(Lists.<String> newArrayList());

	public ValintalaskentaMuistissaProsessi() {
		this(new TyoImpl("Valintalaskenta"), new TyoImpl(
				"Tarjonnasta hakukohteet", 1), new TyoImpl(
				"Hakukohteille hakemukset"), new TyoImpl("Hakemukset"),
				new TyoImpl("Valintaperusteet"));
	}

	public ValintalaskentaMuistissaProsessi(final TyoImpl valintalaskenta,
			final TyoImpl tarjonnastaHakukohteet,
			final TyoImpl hakukohteilleHakemukset, final TyoImpl hakemukset,
			final TyoImpl valintaperusteet) {
		this.peruutaProsessiPoikkeuksesta = true;
		this.valintalaskenta = valintalaskenta;
		this.tarjonnastaHakukohteet = tarjonnastaHakukohteet;
		this.hakukohteilleHakemukset = hakukohteilleHakemukset;
		this.hakemukset = hakemukset;
		this.valintaperusteet = valintaperusteet;
		this.kokonaistyo = new Tyo() {
			final Tyo[] tyot = new Tyo[] { valintalaskenta, valintaperusteet,
					tarjonnastaHakukohteet, hakukohteilleHakemukset, hakemukset };

			@JsonIgnore
			public Collection<Exception> getPoikkeukset() {
				Collection<Collection<Exception>> c = Lists.newArrayList();
				for (Tyo t : tyot) {
					c.add(t.getPoikkeukset());
				}
				return Lists.newArrayList(Iterables.concat(c));
			}

			public int getKokonaismaara() {
				int k = 0;
				for (Tyo t : tyot) {
					k += t.getKokonaismaara();
				}
				return k;
			}

			@Override
			public long getArvioituJaljellaOlevaKokonaiskesto() {
				int k = 0;
				for (Tyo t : tyot) {
					k += t.getArvioituJaljellaOlevaKokonaiskesto();
				}
				return k;
			}

			@Override
			public long getKesto() {
				int k = 0;
				for (Tyo t : tyot) {
					k += t.getKesto();
				}
				return k;
			}

			/**
			 * Palauttaa töiden mediaanikestojen keskiarvon
			 */
			@Override
			public long getYksittaisenTyonArvioituKesto() {
				int k = 0;
				for (Tyo t : tyot) {
					k += t.getYksittaisenTyonArvioituKesto();
				}
				return k / tyot.length;
			}

			public String getNimi() {
				return "kokonaistyo";
			}

			public int getTehty() {
				int k = 0;
				for (Tyo t : tyot) {
					k += t.getTehty();
				}
				return k;
			}
		};
	}

	public TyoImpl getValintaperusteet() {
		return valintaperusteet;
	}

	public TyoImpl getHakukohteilleHakemukset() {
		return hakukohteilleHakemukset;
	}

	public TyoImpl getTarjonnastaHakukohteet() {
		return tarjonnastaHakukohteet;
	}

	public TyoImpl getHakemukset() {
		return hakemukset;
	}

	public Tyo getKokonaistyo() {
		return kokonaistyo;
	}

	public TyoImpl getValintalaskenta() {
		return valintalaskenta;
	}

	@JsonIgnore
	public List<Palvelukutsu> getPalvelukutsut() {
		Collections.sort(palvelukutsut);
		return palvelukutsut;
	}

	@JsonIgnore
	public List<String> getKasitellytHakukohteet() {
		return kasitellytHakukohteet;
	}

	public boolean hasPoikkeuksia() {
		if (peruutaProsessiPoikkeuksesta) {
			return !kokonaistyo.getPoikkeukset().isEmpty();
		} else {
			return false;
		}
	}

	public boolean isPeruutaProsessiPoikkeuksesta() {
		return peruutaProsessiPoikkeuksesta;
	}
}
