package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.RiviBuilder;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;
import fi.vm.sade.valinta.kooste.excel.arvo.MonivalintaArvo;
import fi.vm.sade.valinta.kooste.excel.arvo.NumeroArvo;
import fi.vm.sade.valinta.kooste.excel.arvo.TekstiArvo;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.util.HakemusComparator;
import fi.vm.sade.valinta.kooste.util.KonversioBuilder;
import fi.vm.sade.valinta.kooste.util.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValintatapajonoDTO;

public class ValintatapajonoExcel {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintatapajonoExcel.class);

	private final static String MAARITTELEMATON = "Määrittelemätön";
	private final static String HYVAKSYTTAVISSA = "Hyväksyttävissä";
	private final static String HYLATTY = "Hylätty";

	public final static String VAKIO_MAARITTELEMATON = "MAARITTELEMATON";
	public final static String VAKIO_HYVAKSYTTAVISSA = "HYVAKSYTTAVISSA";
	public final static String VAKIO_HYLATTY = "HYLATTY";

	private final static Collection<String> VAIHTOEHDOT = Arrays.asList(
			MAARITTELEMATON, HYVAKSYTTAVISSA, HYLATTY);
	private final static Map<String, String> VAIHTOEHDOT_KONVERSIO = new KonversioBuilder()
	//
			.addKonversio(StringUtils.EMPTY, MAARITTELEMATON)
			//
			.addKonversio(VAKIO_MAARITTELEMATON, MAARITTELEMATON)
			//
			.addKonversio(VAKIO_HYVAKSYTTAVISSA, HYVAKSYTTAVISSA)
			//
			.addKonversio(VAKIO_HYLATTY, HYLATTY).build();
	private final static Map<String, String> VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO = new KonversioBuilder()
	//
			.addKonversio(StringUtils.EMPTY, VAKIO_MAARITTELEMATON)
			//
			.addKonversio(MAARITTELEMATON, VAKIO_MAARITTELEMATON)
			//
			.addKonversio(HYVAKSYTTAVISSA, VAKIO_HYVAKSYTTAVISSA)
			//
			.addKonversio(HYLATTY, VAKIO_HYLATTY).build();

	private final Excel excel;

	@SuppressWarnings("unchecked")
	public ValintatapajonoExcel(String hakuOid, String hakukohdeOid,
			String valintatapajonoOid,
			//
			String hakuNimi, String hakukohdeNimi,
			//
			List<ValinnanvaiheDTO> valinnanvaihe,
			// List<ValinnanVaiheJonoillaDTO> valinnanvaiheet,
			List<Hakemus> hakemukset) {
		// Jonosija (13) Hakija Valintatieto Kuvaus (FI) Kuvaus (SV) Kuvaus (EN)
		Collection<Rivi> rivit = Lists.newArrayList();
		rivit.add(new RiviBuilder().addOid(hakuOid).addTeksti(hakuNimi, 4)
				.build());
		rivit.add(new RiviBuilder().addOid(hakukohdeOid)
				.addTeksti(hakukohdeNimi, 4).build());

		rivit.add(Rivi.tyhjaRivi());

		final RiviBuilder otsikkoRiviBuilder = new RiviBuilder()
				.addKeskitettyTeksti("Hakemus OID")
				.addKeskitettyTeksti("Jonosija (" + hakemukset.size() + ")")
				.addKeskitettyTeksti("Hakija")
				.addKeskitettyTeksti("Valintatieto")
				.addKeskitettyTeksti("Kuvaus (FI)")
				.addKeskitettyTeksti("Kuvaus (SV)")
				.addKeskitettyTeksti("Kuvaus (EN)");

		rivit.add(otsikkoRiviBuilder.build());

		final Map<String, Integer> jonosijat = Maps.newHashMap();
		final Map<String, String> valintatiedot = Maps.newHashMap();
		final Map<String, Map<String, String>> avaimet = Maps.newHashMap();
		for (ValinnanvaiheDTO vaihe : valinnanvaihe) {
			for (ValintatapajonoDTO jono : vaihe.getValintatapajonot()) {
				if (valintatapajonoOid.equals(jono.getOid())) {
					for (JonosijaDTO jonosija : jono.getJonosijat()) {
						String hakemusOid = jonosija.getHakemusOid();
						jonosijat.put(hakemusOid, jonosija.getJonosija());
						valintatiedot.put(hakemusOid, jonosija
								.getTuloksenTila().toString());
						if (!jonosija.getJarjestyskriteerit().isEmpty()) {

							avaimet.put(hakemusOid, jonosija
									.getJarjestyskriteerit().last().getKuvaus());

						}
					}
				}
			}
		}
		ComparatorChain jonosijaAndHakijaNameComparator = new ComparatorChain(
		// compare by jonosija
				new Comparator<Hakemus>() {
					@Override
					public int compare(Hakemus o1, Hakemus o2) {
						Integer i1 = jonosijat.get(o1.getOid());
						Integer i2 = jonosijat.get(o2.getOid());
						if (i1 == null) {
							i1 = Integer.MAX_VALUE;
						}
						if (i2 == null) {
							i2 = Integer.MAX_VALUE;
						}
						return i1.compareTo(i2);
					}
				});
		jonosijaAndHakijaNameComparator
				.addComparator(HakemusComparator.DEFAULT);
		Collections.sort(hakemukset, jonosijaAndHakijaNameComparator);

		Collection<Collection<Arvo>> sx = Lists.newArrayList();
		for (Hakemus data : hakemukset) {
			String hakemusOid = data.getOid();
			Collection<Arvo> s = Lists.newArrayList();
			s.add(new TekstiArvo(hakemusOid));

			s.add(new NumeroArvo(jonosijat.get(hakemusOid), 0, hakemukset
					.size()));
			Osoite osoite = OsoiteHakemukseltaUtil.osoiteHakemuksesta(data,
					null, null);

			s.add(new TekstiArvo(new StringBuilder()
					.append(osoite.getLastName()).append(" ")
					.append(osoite.getFirstName()).toString()));

			s.add(new MonivalintaArvo(VAIHTOEHDOT_KONVERSIO.get(StringUtils
					.trimToEmpty(valintatiedot.get(hakemusOid))), VAIHTOEHDOT));
			if (avaimet.containsKey(hakemusOid)) {
				Map<String, String> a = avaimet.get(hakemusOid);
				s.add(new TekstiArvo(StringUtils.trimToEmpty(a.get("FI")),
						false, true));
				s.add(new TekstiArvo(StringUtils.trimToEmpty(a.get("SV")),
						false, true));
				s.add(new TekstiArvo(StringUtils.trimToEmpty(a.get("EN")),
						false, true));
			} else {
				s.add(new TekstiArvo(StringUtils.EMPTY, false, true));
				s.add(new TekstiArvo(StringUtils.EMPTY, false, true));
				s.add(new TekstiArvo(StringUtils.EMPTY, false, true));
			}
			sx.add(s);
		}

		rivit.add(new ValintatapajonoDataRivi(sx));
		this.excel = new Excel("Valintatapajono", rivit);
	}

	public Excel getExcel() {
		return excel;
	}
}
