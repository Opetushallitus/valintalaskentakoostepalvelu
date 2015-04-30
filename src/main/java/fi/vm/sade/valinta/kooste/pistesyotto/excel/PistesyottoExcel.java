package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.model.Funktiotyyppi;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.excel.OidRivi;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.RiviBuilder;
import fi.vm.sade.valinta.kooste.excel.Teksti;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;
import fi.vm.sade.valinta.kooste.excel.arvo.BooleanArvo;
import fi.vm.sade.valinta.kooste.excel.arvo.MonivalintaArvo;
import fi.vm.sade.valinta.kooste.excel.arvo.NumeroArvo;
import fi.vm.sade.valinta.kooste.excel.arvo.TekstiArvo;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.util.ApplicationAdditionalDataComparator;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KonversioBuilder;
import fi.vm.sade.valinta.kooste.valintalaskenta.tulos.function.ValintakoeOsallistuminenDTOFunction;
import fi.vm.sade.valinta.kooste.valintalaskenta.tulos.predicate.OsallistujatPredicate;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;

public class PistesyottoExcel {
	private static final Logger LOG = LoggerFactory
			.getLogger(PistesyottoExcel.class);
	private final static String MERKITSEMATTA = "Merkitsemättä";
	private final static String OSALLISTUI = "Osallistui";
	private final static String EI_OSALLISTUNUT = "Ei osallistunut";
	private final static String EI_VAADITA = "Ei vaadita";

	public final static String VAKIO_MERKITSEMATTA = "MERKITSEMATTA";
	public final static String VAKIO_OSALLISTUI = "OSALLISTUI";
	public final static String VAKIO_EI_OSALLISTUNUT = "EI_OSALLISTUNUT";
	public final static String VAKIO_EI_VAADITA = "EI_VAADITA";

	private final static Collection<String> VAIHTOEHDOT = Arrays.asList(
			MERKITSEMATTA, OSALLISTUI, EI_OSALLISTUNUT, EI_VAADITA);
	private final static Map<String, String> VAIHTOEHDOT_KONVERSIO = new KonversioBuilder()
	//
			.addKonversio(StringUtils.EMPTY, MERKITSEMATTA)
			//
			.addKonversio(VAKIO_MERKITSEMATTA, MERKITSEMATTA)
			//
			.addKonversio(VAKIO_OSALLISTUI, OSALLISTUI)
			//
			.addKonversio(VAKIO_EI_VAADITA, EI_VAADITA)
			//
			.addKonversio(VAKIO_EI_OSALLISTUNUT, EI_OSALLISTUNUT).build();
	private final static Map<String, String> VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO = new KonversioBuilder()
	//
			.addKonversio(StringUtils.EMPTY, VAKIO_MERKITSEMATTA)
			//
			.addKonversio(MERKITSEMATTA, VAKIO_MERKITSEMATTA)
			//
			.addKonversio(OSALLISTUI, VAKIO_OSALLISTUI)
			//
			.addKonversio(EI_VAADITA, VAKIO_EI_VAADITA)
			//
			.addKonversio(EI_OSALLISTUNUT, VAKIO_EI_OSALLISTUNUT).build();
	public final static String TOSI = "Hyväksytty";
	public final static String EPATOSI = "Hylätty";
	public final static String TYHJA = "Tyhjä";
	private final static Collection<String> TOTUUSARVO = Arrays.asList(TYHJA,
			TOSI, EPATOSI);
	private final static Map<String, String> TOTUUSARVO_KONVERSIO = new KonversioBuilder()
	//
			.addKonversio(TOSI, Boolean.TRUE.toString())
			//
			.addKonversio(EPATOSI, Boolean.FALSE.toString())
			//
			.addKonversio(TYHJA, null).build();
	private final Excel excel;

	/**
	 * @return < HakemusOid , < Tunniste , ValintakoeDTO > >
	 */
	private Map<String, Map<String, ValintakoeDTO>> valintakoeOidit(
			String hakukohdeOid, Set<String> valintakoeTunnisteet,
			List<ValintakoeOsallistuminenDTO> osallistumistiedot) {
		Map<String, Map<String, ValintakoeDTO>> tunnisteOid = Maps.newHashMap();
		for (ValintakoeOsallistuminenDTO o : osallistumistiedot) {
			for (HakutoiveDTO h : o.getHakutoiveet()) {
				if (!hakukohdeOid.equals(h.getHakukohdeOid())) {
					continue;
				}
				Map<String, ValintakoeDTO> k = Maps.newHashMap();
				for (ValintakoeValinnanvaiheDTO v : h.getValinnanVaiheet()) {
					for (ValintakoeDTO valintakoe : v.getValintakokeet()) {
						k.put(valintakoe.getValintakoeTunniste(), valintakoe);
					}
				}
				tunnisteOid.put(o.getHakemusOid(), k);
			}
		}
		return tunnisteOid;
	}

	public PistesyottoExcel(String hakuOid, String hakukohdeOid,
			String tarjoajaOid, String hakuNimi, String hakukohdeNimi,
			String tarjoajaNimi, Collection<Hakemus> hakemukset,
			Set<String> kaikkiKutsutaanTunnisteet,
			Collection<String> valintakoeTunnisteet,
			List<ValintakoeOsallistuminenDTO> osallistumistiedot,
			List<ValintaperusteDTO> valintaperusteet,
			List<ApplicationAdditionalDataDTO> pistetiedot) {
		this(hakuOid, hakukohdeOid, tarjoajaOid, hakuNimi, hakukohdeNimi,
				tarjoajaNimi, hakemukset, kaikkiKutsutaanTunnisteet,
				valintakoeTunnisteet, osallistumistiedot, valintaperusteet,
				pistetiedot, Collections
						.<PistesyottoDataRiviKuuntelija> emptyList());
	}

	public PistesyottoExcel(String hakuOid, String hakukohdeOid,
			String tarjoajaOid, String hakuNimi, String hakukohdeNimi,
			String tarjoajaNimi, Collection<Hakemus> hakemukset,
			Set<String> kaikkiKutsutaanTunnisteet,
			Collection<String> valintakoeTunnisteet,
			List<ValintakoeOsallistuminenDTO> osallistumistiedot,
			List<ValintaperusteDTO> valintaperusteet,
			List<ApplicationAdditionalDataDTO> pistetiedot,
			PistesyottoDataRiviKuuntelija kuuntelija) {
		this(hakuOid, hakukohdeOid, tarjoajaOid, hakuNimi, hakukohdeNimi,
				tarjoajaNimi, hakemukset, kaikkiKutsutaanTunnisteet,
				valintakoeTunnisteet, osallistumistiedot, valintaperusteet,
				pistetiedot, Arrays.asList(kuuntelija));
	}

	public PistesyottoExcel(String hakuOid, String hakukohdeOid,
			String tarjoajaOid, String hakuNimi, String hakukohdeNimi,
			String tarjoajaNimi, Collection<Hakemus> hakemukset,
			Set<String> kaikkiKutsutaanTunnisteet,
			Collection<String> valintakoeTunnisteet,
			List<ValintakoeOsallistuminenDTO> osallistumistiedot,
			List<ValintaperusteDTO> valintaperusteet,
			List<ApplicationAdditionalDataDTO> pistetiedot,
			Collection<PistesyottoDataRiviKuuntelija> kuuntelijat) {
		if (pistetiedot == null) {
			pistetiedot = Collections.emptyList();
		}

        Collections.sort(valintaperusteet, (o1, o2) -> o1.getKuvaus().compareTo(o2.getKuvaus()));

		Map<String, Map<String, ValintakoeDTO>> tunnisteValintakoe = valintakoeOidit(
				hakukohdeOid, Sets.newHashSet(valintakoeTunnisteet),
				osallistumistiedot);

		Set<String> osallistujat =
		//
		FluentIterable.from(osallistumistiedot)
				//
				.filter(OsallistujatPredicate.vainOsallistujatTunnisteella(
						hakukohdeOid, valintakoeTunnisteet))
				//
				.transform(ValintakoeOsallistuminenDTOFunction.TO_HAKEMUS_OIDS)
				//
				.toSet();
		// LOG.error("{}", Arrays.toString(osallistujat.toArray()));
		Collection<String> tunnisteet =
		//
		FluentIterable.from(valintaperusteet)
		//
				.transform(new Function<ValintaperusteDTO, String>() {
					@Override
					public String apply(ValintaperusteDTO valintaperuste) {
						return valintaperuste.getTunniste();
					}
				}).toList();

		Collection<Rivi> rivit = Lists.newArrayList();
		rivit.add(new RiviBuilder().addOid(hakuOid).addTeksti(hakuNimi, 4)
				.build());
		rivit.add(new RiviBuilder().addOid(hakukohdeOid)
				.addTeksti(hakukohdeNimi, 4).build());
		if (StringUtils.isBlank(tarjoajaOid)) {
			rivit.add(Rivi.tyhjaRivi());
		} else {
			rivit.add(new RiviBuilder().addOid(tarjoajaOid)
					.addTeksti(tarjoajaNimi, 4).build());
		}
		rivit.add(Rivi.tyhjaRivi());
		rivit.add(new RiviBuilder().addTyhja().addTyhja().addTyhja()
				.addRivi(new OidRivi(tunnisteet, 2, true)).build());
		final RiviBuilder valintakoeOtsikkoRiviBuilder = new RiviBuilder();
		final RiviBuilder otsikkoRiviBuilder = new RiviBuilder()
				.addKeskitettyTeksti("Hakemus OID")
				.addKeskitettyTeksti("Tiedot")
				.addKeskitettyTeksti("Henkilötunnus");
		valintakoeOtsikkoRiviBuilder.addTyhja().addTyhja().addTyhja();
		for (String valintakoe : FluentIterable.from(valintaperusteet)
		//
				.transform(new Function<ValintaperusteDTO, String>() {
					@Override
					public String apply(ValintaperusteDTO valintaperuste) {
						if (Funktiotyyppi.LUKUARVOFUNKTIO.equals(valintaperuste
								.getFunktiotyyppi())
								&& !StringUtils.isBlank(valintaperuste.getMin())
								&& !StringUtils.isBlank(valintaperuste.getMax())) {
							// create value constraint

							return new StringBuilder()
									.append(valintaperuste.getKuvaus())
									.append(" (")
									.append(Formatter
											.suomennaNumero(new BigDecimal(
													valintaperuste.getMin())))
									.append(" - ")
									.append(Formatter
											.suomennaNumero(new BigDecimal(
													valintaperuste.getMax())))
									.append(")").toString();
						} else {
							return valintaperuste.getKuvaus();
						}

					}
				}).toList()) {
			otsikkoRiviBuilder.addTyhja().addKeskitettyTeksti("Osallistuminen");
			valintakoeOtsikkoRiviBuilder.addSolu(new Teksti(valintakoe, true,
					true, false, 0, 2, false));
		}
		rivit.add(valintakoeOtsikkoRiviBuilder.build());
		rivit.add(otsikkoRiviBuilder.build());

		Collection<Collection<Arvo>> sx = Lists.newArrayList();

		Collections.sort(pistetiedot,
				ApplicationAdditionalDataComparator.ASCENDING);

		// Asennetaan konvertterit
		Collection<PistesyottoDataArvo> dataArvot = Lists.newArrayList();
		for (ValintaperusteDTO valintaperuste : valintaperusteet) {
			LOG.info("Tunniste=={}, osallistumisentunniste={}", valintaperuste.getTunniste(), valintaperuste.getOsallistuminenTunniste());
			if (Funktiotyyppi.LUKUARVOFUNKTIO.equals(valintaperuste
					.getFunktiotyyppi())) {
				Double max = asNumber(valintaperuste.getMax());
				Double min = asNumber(valintaperuste.getMin());
				if (min != null && max != null) {
					dataArvot.add(new NumeroDataArvo(min, max,
							VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO, StringUtils
									.trimToEmpty(valintaperuste.getTunniste())
									.replace(".", ","), VAKIO_OSALLISTUI,
							StringUtils.trimToEmpty(valintaperuste
									.getOsallistuminenTunniste())));
				} else if (valintaperuste.getArvot() != null
						&& !valintaperuste.getArvot().isEmpty()) {
					dataArvot.add(new DiskreettiDataArvo(valintaperuste
							.getArvot(), VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO,
							StringUtils.trimToEmpty(
									valintaperuste.getTunniste()).replace(".",
									","), VAKIO_OSALLISTUI, StringUtils
									.trimToEmpty(valintaperuste
											.getOsallistuminenTunniste())));
				} else {
                    dataArvot.add(new NumeroDataArvo(0, 0,
                            VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO, StringUtils
                            .trimToEmpty(valintaperuste.getTunniste())
                            .replace(".", ","), VAKIO_OSALLISTUI,
                            StringUtils.trimToEmpty(valintaperuste
                                    .getOsallistuminenTunniste())));

				}
			} else if (Funktiotyyppi.TOTUUSARVOFUNKTIO.equals(valintaperuste
					.getFunktiotyyppi())) {
				dataArvot.add(new BooleanDataArvo(TOTUUSARVO_KONVERSIO,
						VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO, StringUtils
								.trimToEmpty(valintaperuste.getTunniste()),
						VAKIO_OSALLISTUI, StringUtils
								.trimToEmpty(valintaperuste
										.getOsallistuminenTunniste())));
			} else {
				LOG.error("Tunnistamaton funktiotyyppi! Peruutetaan pistesyoton luonti!");
				throw new RuntimeException(
						"Tunnistamaton syote! Peruutetaan pistesyoton luonti!");
			}
		}
		Map<String, String> oidToHetu = hakemukset.stream().collect(
				Collectors.toMap(Hakemus::getOid, h -> new HakemusWrapper(h)
						.getHenkilotunnusTaiSyntymaaika()));
		for (ApplicationAdditionalDataDTO data : pistetiedot) {
			boolean osallistuja = osallistujat.contains(data.getOid());
			// Hakemuksen <tunniste, valintakoeDTO> tiedot
			Map<String, ValintakoeDTO> tunnisteDTO = Optional.ofNullable(tunnisteValintakoe
					.get(data.getOid())).orElse(Collections.emptyMap());
			Collection<Arvo> s = Lists.newArrayList();
			s.add(new TekstiArvo(data.getOid()));
			s.add(new TekstiArvo(new StringBuilder().append(data.getLastName())
					.append(", ").append(data.getFirstNames()).toString()));
			s.add(new TekstiArvo(Optional.ofNullable(
					oidToHetu.get(data.getOid())).orElse(StringUtils.EMPTY)));
			boolean syote = false;
			for (ValintaperusteDTO valintaperuste : valintaperusteet) {
				ValintakoeDTO valintakoe = Optional.ofNullable(tunnisteDTO.get(valintaperuste
						.getTunniste())).orElse(new ValintakoeDTO());
				boolean syotettavissaKaikille =
						kaikkiKutsutaanTunnisteet.contains(valintaperuste.getTunniste())
						|| Boolean.TRUE.equals(valintaperuste.getSyotettavissaKaikille());
				if (!syotettavissaKaikille && !osallistuja) {
					s.add(TekstiArvo.tyhja(false));
					s.add(TekstiArvo.tyhja(false));
					continue;
				}
				if (syotettavissaKaikille || (valintakoe != null && Osallistuminen.OSALLISTUU.equals(valintakoe.getOsallistuminenTulos().getOsallistuminen()))) {
					syote = true;
					if (Funktiotyyppi.LUKUARVOFUNKTIO.equals(valintaperuste
							.getFunktiotyyppi())) {
						if (valintaperuste.getArvot() != null
								&& !valintaperuste.getArvot().isEmpty()) {
							String value = null;
							if (data.getAdditionalData() == null) {
								value = null;
							} else {
								value = data.getAdditionalData().get(
										valintaperuste.getTunniste());
							}
							s.add(new MonivalintaArvo(value, valintaperuste
									.getArvot()));
						} else {
							Number value;
							if (data.getAdditionalData() == null) {
								value = asNumber(null);
							} else {
								value = asNumber(data.getAdditionalData().get(
										valintaperuste.getTunniste()));
							}
							Number max = asNumber(valintaperuste.getMax());
							Number min = asNumber(valintaperuste.getMin());

							s.add(new NumeroArvo(value, min, max));
						}
					} else if (Funktiotyyppi.TOTUUSARVOFUNKTIO
							.equals(valintaperuste.getFunktiotyyppi())) {
						String value = StringUtils.trimToEmpty(data
								.getAdditionalData().get(
										valintaperuste.getTunniste()));
						s.add(new BooleanArvo(value, TOTUUSARVO, TOSI, EPATOSI,
								TYHJA));
					} else {
						s.add(new TekstiArvo(data.getAdditionalData().get(
								StringUtils.trimToEmpty(valintaperuste
										.getTunniste())), false));
					}

					// s.add(new MonivalintaArvo(VAIHTOEHDOT_KONVERSIO
					// .get(VAKIO_EI_VAADITA), VAIHTOEHDOT));
					s.add(new MonivalintaArvo(VAIHTOEHDOT_KONVERSIO
							.get(StringUtils.trimToEmpty(data
									.getAdditionalData()
									.get(valintaperuste
											.getOsallistuminenTunniste()))),
							VAIHTOEHDOT));
				} else {
					s.add(TekstiArvo.tyhja(false));
					s.add(TekstiArvo.tyhja(false));
				}
			}
			if (syote) {
				sx.add(s);
			}
		}

		if (sx.isEmpty()) {
			throw new RuntimeException(
					"Hakukohteessa ei ole pistesyötettäviä hakijoita.");
		}
		rivit.add(new PistesyottoDataRivi(sx, kuuntelijat, dataArvot));

		this.excel = new Excel("Pistesyöttö", rivit, new int[] { 0 } // piilottaa
				// ensimmaisen
				// pysty
				// sarakkeen
				, new int[] { 4 });
	}

	private Double asNumber(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		} else {
			value = StringUtils.trimToEmpty(value).replace(",", ".");
			try {
				return Double.parseDouble(value);
			} catch (Exception e) {
				return null;
			}
		}
	}

	public Excel getExcel() {
		return excel;
	}
}
