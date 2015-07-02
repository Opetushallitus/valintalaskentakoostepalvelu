package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.util.*;
import fi.vm.sade.valinta.kooste.util.Formatter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.TilaHistoriaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.sijoittelu.exception.SijoittelultaEiSisaltoaPoikkeus;
import fi.vm.sade.valinta.kooste.util.excel.Highlight;
import fi.vm.sade.valinta.kooste.util.excel.Span;

/**
 *         Komponentti luo sijoittelun tulokset excel tiedostoksi!
 */
@Component("sijoittelunTulosXlsKomponentti")
public class SijoittelunTulosExcelKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(SijoittelunTulosExcelKomponentti.class);

    @Autowired
    private KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    public InputStream luoXls(List<Valintatulos> tilat, String preferoitukielikoodi, String hakukohdeNimi, String tarjoajaNimi, String hakukohdeOid, List<Hakemus> hakemuksetList, HakukohdeDTO hakukohde) {
        Map<String, Koodi> countryCodes = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        Map<String, Koodi> postCodes = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
        Map<String, Hakemus> hakemukset = hakemuksetList.stream().collect(Collectors.toMap(Hakemus::getOid, h -> h));
        if (hakukohde == null) {
            LOG.error("Hakukohteessa ei hakijoita tai hakukohdetta ei ole olemassa!");
            throw new SijoittelultaEiSisaltoaPoikkeus("Hakukohteessa ei hakijoita tai hakukohdetta ei ole olemassa!");
        }
        List<Object[]> rivit = new ArrayList<>();
        List<ValintatapajonoDTO> valintatapajonot = Optional.ofNullable(hakukohde.getValintatapajonot()).orElse(Collections.emptyList()).stream()
                .filter(v -> v.getHakemukset() != null && !v.getHakemukset().isEmpty())
                .collect(Collectors.toList());
        if (valintatapajonot.isEmpty()) {
            LOG.error("Yritettiin muodostaa sijoittelun tuloksista taulukkolaskenta kohteelle({}) jolla ei ole valintatapajonoja saatavilla!", hakukohdeOid);
            throw new RuntimeException("Yritettiin muodostaa sijoittelun tuloksista taulukkolaskenta kohteelle(" + hakukohdeOid + ") jolla ei ole valintatapajonoja saatavilla!");
        }
        Collections.sort(valintatapajonot,
                (o1, o2) -> {
                    if (o1.getPrioriteetti() == null || o2.getPrioriteetti() == 0) {
                        return 0;
                    }
                    return o1.getPrioriteetti().compareTo(o2.getPrioriteetti());
                });

        Set<String> hakemusOids = new HashSet<>();
        List<HakemusDTO> distinctHakemuksetFromAllQueues = new ArrayList<>();
        for (ValintatapajonoDTO jono : valintatapajonot) {
            for (HakemusDTO hakemus : jono.getHakemukset()) {
                String hakemusOid = hakemus.getHakemusOid();
                if (!hakemusOids.contains(hakemusOid)) {
                    distinctHakemuksetFromAllQueues.add(hakemus);
                    hakemusOids.add(hakemusOid);
                }
            }
        }

        Map<String, Map<String, IlmoittautumisTila>> valintatapajononTilat = valintatapajononTilat(tilat);
        rivit.add(new Object[]{tarjoajaNimi});
        rivit.add(new Object[]{hakukohdeNimi});
        rivit.add(new Object[]{});

        Collections.sort(distinctHakemuksetFromAllQueues,
                new Comparator<HakemusDTO>() {
                    private int ordinal(HakemusDTO h) {
                        switch (h.getTila()) {
                            case HARKINNANVARAISESTI_HYVAKSYTTY:
                                return 0;
                            case HYVAKSYTTY:
                                return h.isHyvaksyttyHarkinnanvaraisesti() ?  0 : 1;
                            case VARASIJALTA_HYVAKSYTTY:
                                return h.isHyvaksyttyHarkinnanvaraisesti() ? 0 : 1;
                            case VARALLA:
                                return 2;
                            case PERUUNTUNUT:
                                return 3;
                            case PERUNUT:
                                return 4;
                            case PERUUTETTU:
                                return 5;
                            case HYLATTY:
                                return 6;
                            default:
                                return 7;
                        }
                    }

                    @Override
                    public int compare(HakemusDTO o1, HakemusDTO o2) {
                        return new Integer(ordinal(o1)).compareTo(ordinal(o2));
                    }
                }
        );
        List<Object> valintatapajonoOtsikkoRivi = Lists.newArrayList();
        valintatapajonoOtsikkoRivi.addAll(Arrays.asList("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")); // alun tyhjat pystyrivit
        List<Object> otsikkoRivi = Lists.newArrayList();
        otsikkoRivi.addAll(Arrays.asList(
                "Hakemus",
                "Hakija",
                "Henkilötunnus",
                "Syntymäaika",
                "Sukupuoli",
                "Äidinkieli",
                "Kansalaisuus",
                "Lähiosoite",
                "Postinumero",
                "Postitoimipaikka",
                "Osoite (ulkomaa)",
                "Postinumero (ulkomaa)",
                "Kaupunki (ulkomaa)",
                "Asuinmaa",
                "Kansallinen ID",
                "Passin numero",
                "Sähköposti",
                "Puhelinnumero",
                "Lupa julkaisuun",
                "Hakutoive"
        ));
        {
            int index = 0;
            for (ValintatapajonoDTO jono : valintatapajonot) {
                ++index;
                boolean highlight = index % 2 == 1;
                valintatapajonoOtsikkoRivi.add(new Span("Valintatapajono: " + jono.getNimi(), 6, highlight));
                List<Object> otsikot = Arrays.asList("Jonosija", "Pisteet", "Sijoittelun tila", "Vastaanottotieto", "Ilmoittautumistieto", "Muokattu");
                if (highlight) {
                    otsikot = otsikot.stream().map(Highlight::new).collect(Collectors.toList());
                }
                otsikkoRivi.addAll(otsikot);
            }
        }
        rivit.add(valintatapajonoOtsikkoRivi.toArray());
        rivit.add(otsikkoRivi.toArray());

        Map<String, Map<String, HakemusDTO>> jonoOidHakemusOidHakemusDto = valintatapajonot.stream()
                .collect(Collectors.toMap(ValintatapajonoDTO::getOid, v -> v.getHakemukset().stream().collect(Collectors.toMap(HakemusDTO::getHakemusOid, h -> h))));

        for (HakemusDTO hDto : distinctHakemuksetFromAllQueues) {
            HakemusWrapper wrapper = new HakemusWrapper(hakemukset.get(hDto.getHakemusOid()));
            String nimi = wrapper.getSukunimi() + ", " + wrapper.getEtunimi();
            List<Object> hakemusRivi = Lists.newArrayList();

            hakemusRivi.addAll(Arrays.asList(
                    hDto.getHakemusOid(),
                    nimi,
                    wrapper.getHenkilotunnus(),
                    wrapper.getSyntymaaika(),
                    wrapper.getSukupuoli(),
                    wrapper.getAidinkieli(),
                    wrapper.getKansalaisuus(),
                    wrapper.getSuomalainenLahiosoite(),
                    wrapper.getSuomalainenPostinumero(),
                    postitoimipaikka(postCodes, wrapper),
                    wrapper.getUlkomainenLahiosoite(),
                    wrapper.getUlkomainenPostinumero(),
                    wrapper.getKaupunkiUlkomaa(),
                    countryNameInEnglish(countryCodes, wrapper),
                    wrapper.getKansallinenId(),
                    wrapper.getPassinnumero(),
                    wrapper.getSahkopostiOsoite(),
                    wrapper.getPuhelinnumero(),
                    HakemusUtil.lupaJulkaisuun(wrapper.getLupaJulkaisuun()),
                    wrapper.getHakutoiveenPrioriteetti(hakukohdeOid)
            ));
            int index = 0;
            for (ValintatapajonoDTO jono : valintatapajonot) {
                ++index;
                Map<String, HakemusDTO> jonoOidToHakemusOid = jonoOidHakemusOidHakemusDto.get(jono.getOid());
                if (jonoOidToHakemusOid.containsKey(hDto.getHakemusOid())) {
                    HakemusDTO hakemusDto = jonoOidHakemusOidHakemusDto.get(jono.getOid()).get(hDto.getHakemusOid());
                    String hakemusOid = hakemusDto.getHakemusOid();
                    Map<String, IlmoittautumisTila> hakemusTilat = Collections.emptyMap();
                    if (valintatapajononTilat.containsKey(jono.getOid())) {
                        hakemusTilat = valintatapajononTilat.get(jono.getOid());
                        if (hakemusTilat == null) {
                            hakemusTilat = Collections.emptyMap();
                        }
                    }
                    String ilmoittautumistieto = StringUtils.EMPTY;
                    try {
                        ilmoittautumistieto = HakemusUtil.tilaConverter(hakemusTilat.get(hakemusDto.getHakemusOid()), preferoitukielikoodi);
                    } catch (Exception e) {
                    }
                    List<Valintatulos> valintaTulos = tilat.stream().filter(
                            t -> hakemusOid.equals(t.getHakemusOid())
                    ).collect(Collectors.toList());
                    String valintaTieto = StringUtils.EMPTY;
                    for (Valintatulos valinta : valintaTulos) {
                        if (jono.getOid().equals(valinta.getValintatapajonoOid())) {
                            if (valinta.getTila() != null) {
                                valintaTieto = HakemusUtil.tilaConverter(valinta.getTila(), preferoitukielikoodi);
                            }
                            break;
                        }
                    }
                    List<Object> jonoHakemusSarakkeet = Arrays.asList(
                            hakemusDto.getJonosija(),
                            Formatter.suomennaNumero(hakemusDto.getPisteet()),
                            HakemusUtil.tilaConverter(
                                    hakemusDto.getTila(),
                                    preferoitukielikoodi,
                                    hakemusDto.isHyvaksyttyHarkinnanvaraisesti(),
                                    true,
                                    hakemusDto.getVarasijanNumero()
                            ),
                            valintaTieto,
                            ilmoittautumistieto,
                            muokattu(hakemusDto.getTilaHistoria())
                    );

                    if (index % 2 == 1) {
                        jonoHakemusSarakkeet = jonoHakemusSarakkeet.stream()
                                .map(Highlight::new)
                                .collect(Collectors.toList());
                    }
                    hakemusRivi.addAll(jonoHakemusSarakkeet);
                } else {
                    hakemusRivi.addAll(Arrays.asList("", "", "", "", "", ""));
                }
            }
            rivit.add(hakemusRivi.toArray());
        }
        return ExcelExportUtil.exportGridAsXls(rivit.toArray(new Object[][]{}));
    }

    private String muokattu(List<TilaHistoriaDTO> h) {
        if (h == null || h.isEmpty()) {
            return StringUtils.EMPTY;
        } else {
            Collections.sort(h, (o1, o2) -> {
                if (o1 == null || o2 == null || o1.getLuotu() == null || o2.getLuotu() == null) {
                    return 0;
                }
                return -1 * o1.getLuotu().compareTo(o2.getLuotu());
            });
            return Formatter.paivamaara(h.get(0).getLuotu());
        }
    }

    private Map<String, Map<String, IlmoittautumisTila>> valintatapajononTilat(List<Valintatulos> tilat) {
        Map<String, Map<String, IlmoittautumisTila>> t = Maps.newHashMap();
        try {
            for (Valintatulos tulos : tilat) {
                Map<String, IlmoittautumisTila> jono;
                if (!t.containsKey(tulos.getValintatapajonoOid())) {
                    t.put(tulos.getValintatapajonoOid(), jono = Maps.<String, IlmoittautumisTila>newHashMap());
                } else {
                    jono = t.get(tulos.getValintatapajonoOid());
                }
                jono.put(tulos.getHakemusOid(), tulos.getIlmoittautumisTila());
            }
        } catch (Exception e) {
            LOG.error("Ilmoittautumistiloja ei saatu luettua sijoittelusta!", e);
        }
        return t;
    }

    private String postitoimipaikka(Map<String, Koodi> postCodes, HakemusWrapper wrapper) {
        return KoodistoCachedAsyncResource.haeKoodistaArvo(postCodes.get(wrapper.getSuomalainenPostinumero()), KieliUtil.SUOMI, wrapper.getSuomalainenPostinumero());
    }

    private String countryNameInEnglish(Map<String, Koodi> countryCodes, HakemusWrapper wrapper) {
        return KoodistoCachedAsyncResource.haeKoodistaArvo(countryCodes.get(wrapper.getAsuinmaa()), KieliUtil.ENGLANTI, wrapper.getAsuinmaa());
    }
}
