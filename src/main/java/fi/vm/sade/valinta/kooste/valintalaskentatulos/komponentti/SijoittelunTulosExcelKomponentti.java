package fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.*;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Lukuvuosimaksu;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Maksuntila;
import fi.vm.sade.valinta.kooste.sijoittelu.exception.SijoittelultaEiSisaltoaPoikkeus;
import fi.vm.sade.valinta.kooste.util.*;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.kooste.util.excel.Highlight;
import fi.vm.sade.valinta.kooste.util.excel.Span;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 *         Komponentti luo sijoittelun tulokset excel tiedostoksi!
 */
@Component("sijoittelunTulosXlsKomponentti")
public class SijoittelunTulosExcelKomponentti {
    private static final Logger LOG = LoggerFactory.getLogger(SijoittelunTulosExcelKomponentti.class);

    @Autowired
    private KoodistoCachedAsyncResource koodistoCachedAsyncResource;

    public InputStream luoXls(List<Valintatulos> valintatulokset, String preferoitukielikoodi, String hakukohdeNimi, String tarjoajaNimi, String hakukohdeOid,
                              List<HakemusWrapper> hakemuksetList, List<Lukuvuosimaksu> lukuvuosimaksut, HakukohdeDTO hakukohde, HakuV1RDTO hakuDTO, List<ValintatietoValinnanvaiheDTO> valinnanVaiheet) {
        Map<String, Koodi> countryCodes = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.MAAT_JA_VALTIOT_1);
        Map<String, Koodi> postCodes = koodistoCachedAsyncResource.haeKoodisto(KoodistoCachedAsyncResource.POSTI);
        Map<String, HakemusWrapper> hakemukset = hakemuksetList.stream().collect(Collectors.toMap(HakemusWrapper::getOid, h -> h));
        boolean isKkHaku = hakuDTO.getKohdejoukkoUri().startsWith("haunkohdejoukko_12");
        if (hakukohde == null) {
            LOG.error("Hakukohteessa ei hakijoita tai hakukohdetta ei ole olemassa!");
            throw new SijoittelultaEiSisaltoaPoikkeus("Hakukohteessa ei hakijoita tai hakukohdetta ei ole olemassa!");
        }
        List<Object[]> rivit = new ArrayList<>();
        List<ValintatapajonoDTO> valintatapajonot = ofNullable(hakukohde.getValintatapajonot()).orElse(Collections.emptyList()).stream()
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

        // Hakijaryhmien nimet
        List<HakijaryhmaDTO> hakijaryhmat = getHakijaryhmatWithHakemuksia(hakukohde);
        Map<String, List<String>> mapHakemusOidToHakijaryhmaNimet = mapHakemusOidToHakijaryhmaNimet(hakijaryhmat);

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
        rivit.add(new Object[]{tarjoajaNimi});
        rivit.add(new Object[]{hakukohdeNimi});
        rivit.add(new Object[]{});

        // Järjestetään hakemukset tilan mukaan
        sortHakemuksetByTila(distinctHakemuksetFromAllQueues);

        List<Object> otsikkoRivi = Lists.newArrayList();
        otsikkoRivi.addAll(Arrays.asList(
                "Hakemus",
                "Hakija",
                "Oppijanumero",
                "Henkilötunnus",
                "Syntymäaika",
                "Sukupuoli",
                "Äidinkieli",
                "Asiointikieli",
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
                "Maksun tila",
                "Sähköposti",
                "Puhelinnumero",
                "Lupa julkaisuun",
                "Lupa sähköiseen asiointiin",
                "Hakutoive",
                "Hakijaryhmät"
        ));
        List<Object> valintatapajonoOtsikkoRivi = new ArrayList<>(Collections.nCopies(otsikkoRivi.size(), "")); // alun tyhjat pystyrivit
        {
            int index = 0;
            for (ValintatapajonoDTO jono : valintatapajonot) {
                ++index;
                boolean highlight = index % 2 == 1;
                int spanColumns = isKkHaku ? 12 : 7;
                valintatapajonoOtsikkoRivi.add(new Span("Valintatapajono: " + jono.getNimi(), spanColumns, highlight));
                List<Object> otsikot = isKkHaku ?
                        Arrays.asList(
                    "Jonosija",
                    "Pisteet",
                    "Sijoittelun tila",
                    "Hylkäyksen syy",
                    "Vastaanottotieto",
                    "Ilmoittautumistieto",
                    "Ehdollinen valinta",
                    "Ehdollisen hyväksymisen ehto",
                    "Ehdollisen hyväksymisen FI",
                    "Ehdollisen hyväksymisen SV",
                    "Ehdollisen hyväksymisen EN",
                    "Muokattu") :
                Arrays.asList(
                        "Jonosija",
                        "Pisteet",
                        "Sijoittelun tila",
                        "Hylkäyksen syy",
                        "Vastaanottotieto",
                        "Ilmoittautumistieto",
                        "Muokattu");
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

        Map<String, Lukuvuosimaksu> personOidToLukuvuosimaksu = lukuvuosimaksut.stream().collect(Collectors.toMap(Lukuvuosimaksu::getPersonOid, l -> l));

        Map<String, IlmoittautumisTila> hakemusJaJonoMappaus = valintatapajononTilat(valintatulokset);
        for (HakemusDTO hDto : distinctHakemuksetFromAllQueues) {
            HakemusWrapper wrapper = hakemukset.get(hDto.getHakemusOid());
            String nimi = wrapper.getSukunimi() + ", " + wrapper.getEtunimet();
            List<Object> hakemusRivi = Lists.newArrayList();
            Maksuntila maksuntila = ofNullable(personOidToLukuvuosimaksu.get(hDto.getHakijaOid())).map(Lukuvuosimaksu::getMaksuntila).orElse(Maksuntila.MAKSAMATTA);
            hakemusRivi.addAll(Arrays.asList(
                    hDto.getHakemusOid(),
                    nimi,
                    wrapper.getPersonOid(),
                    wrapper.getHenkilotunnus(),
                    wrapper.getSyntymaaika(),
                    wrapper.getSukupuoli(),
                    wrapper.getAidinkieli(),
                    wrapper.getAsiointikieli(),
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
                    wrapper.isMaksuvelvollinen(hakukohdeOid) ? maksuntila.toString() : "",
                    wrapper.getSahkopostiOsoite(),
                    wrapper.getPuhelinnumero(),
                    HakemusUtil.lupaJulkaisuun(wrapper.getLupaJulkaisuun()),
                    HakemusUtil.lupaSahkoiseenAsiointiin(wrapper.getLupaSahkoiseenAsiointiin()),
                    wrapper.getHakutoiveenPrioriteetti(hakukohdeOid),
                    StringUtils.join(mapHakemusOidToHakijaryhmaNimet.get(hDto.getHakemusOid()), ", ")
            ));
            int index = 0;
            for (ValintatapajonoDTO jono : valintatapajonot) {
                ++index;
                Map<String, HakemusDTO> jonoOidToHakemusOid = jonoOidHakemusOidHakemusDto.get(jono.getOid());
                if (jonoOidToHakemusOid.containsKey(hDto.getHakemusOid())) {

                    HakemusDTO hakemusDto = jonoOidHakemusOidHakemusDto.get(jono.getOid()).get(hDto.getHakemusOid());
                    String hakemusOid = hakemusDto.getHakemusOid();

                    String ilmoittautumistieto =
                            HakemusUtil.tilaConverter(hakemusJaJonoMappaus.get(hakemusOidJaValintatapajonoOidYhdiste(hakemusOid, jono.getOid())), preferoitukielikoodi);

                    List<Valintatulos> hakemuksenValintatulokset = valintatulokset.stream()
                            .filter(t -> hakemusOid.equals(t.getHakemusOid()))
                            .collect(Collectors.toList());

                    //Etsitään nyt käsiteltävää sijoittelujonoa vastaava valintalaskennan valintatietojono. Huom. Eri DTO kuin ylempänä samalla nimellä, eri sisältö.
                    List<fi.vm.sade.valintalaskenta.domain.dto.ValintatapajonoDTO> valintatietoJono = valinnanVaiheet.stream()
                            .flatMap(vaihe -> vaihe.getValintatapajonot().stream().filter(j -> j.getOid().equals(jono.getOid())))
                            .collect(Collectors.toList());
                    String hylkayksenSyy = StringUtils.EMPTY;
                    if (!valintatietoJono.isEmpty() && hakemusDto.getTila() == HakemuksenTila.HYLATTY) {
                        JonosijaDTO jonosija = valintatietoJono.get(0).getJonosijat().stream()
                                .filter(sija -> hakemusOid.equals(sija.getHakemusOid())).findFirst().orElse(null);
                        //Näytetään löytynyt kuvaus preferoidulla kielellä jos mahdollista, mutta fallback muille kielille tarvittaessa: FI > SV > EN
                        if(jonosija != null ) {
                            Map<String, String> hylkayksenSyyt = jonosija.getJarjestyskriteerit().first().getKuvaus();
                            hylkayksenSyy = hylkayksenSyyt.get(preferoitukielikoodi);
                            if (StringUtils.isEmpty(hylkayksenSyy)) {
                                String fi = hylkayksenSyyt.get("FI");
                                String sv = hylkayksenSyyt.get("SV");
                                String en = hylkayksenSyyt.get("EN");
                                if (StringUtils.isNotEmpty(fi)) {
                                    hylkayksenSyy = fi;
                                } else if (StringUtils.isNotEmpty(sv)) {
                                    hylkayksenSyy = sv;
                                } else {
                                    hylkayksenSyy = en;
                                }
                            }
                        }
                    }

                    String valintaTieto = StringUtils.EMPTY;
                    String ehdollinenValinta = StringUtils.EMPTY;
                    String ehdollisenHyvaksymisenEhto = StringUtils.EMPTY;
                    String ehdollisenHyvaksymisenEhtoFI = StringUtils.EMPTY;
                    String ehdollisenHyvaksymisenEhtoSV = StringUtils.EMPTY;
                    String ehdollisenHyvaksymisenEhtoEN = StringUtils.EMPTY;
                    for (Valintatulos valintatulos : hakemuksenValintatulokset) {
                        if (jono.getOid().equals(valintatulos.getValintatapajonoOid())) {
                            if (valintatulos.getTila() != null) {
                                valintaTieto = HakemusUtil.tilaConverter(valintatulos.getTila(), preferoitukielikoodi);
                                ehdollinenValinta = HakemusUtil.ehdollinenValinta(valintatulos.getEhdollisestiHyvaksyttavissa());
                                ehdollisenHyvaksymisenEhto = valintatulos.getEhdollisenHyvaksymisenEhtoKoodi();
                                ehdollisenHyvaksymisenEhtoFI = valintatulos.getEhdollisenHyvaksymisenEhtoFI();
                                ehdollisenHyvaksymisenEhtoSV = valintatulos.getEhdollisenHyvaksymisenEhtoSV();
                                ehdollisenHyvaksymisenEhtoEN = valintatulos.getEhdollisenHyvaksymisenEhtoEN();
                            }
                            break;
                        }
                    }
                    List<Object> jonoHakemusSarakkeet = isKkHaku ?
                            Arrays.asList(
                                hakemusDto.getJonosija(),
                                Formatter.suomennaNumero(hakemusDto.getPisteet()),
                                HakemusUtil.tilaConverter(
                                        hakemusDto.getTila(),
                                        preferoitukielikoodi,
                                        hakemusDto.isHyvaksyttyHarkinnanvaraisesti(),
                                        false,
                                        true,
                                        hakemusDto.getVarasijanNumero(),
                                        ehdollisenHyvaksymisenEhto
                                ),
                                hylkayksenSyy,
                                valintaTieto,
                                ilmoittautumistieto,
                                ehdollinenValinta,
                                ehdollisenHyvaksymisenEhto,
                                ehdollisenHyvaksymisenEhtoFI,
                                ehdollisenHyvaksymisenEhtoSV,
                                ehdollisenHyvaksymisenEhtoEN,
                                muokattu(hakemusDto.getTilaHistoria()
                            )) :
                            Arrays.asList(
                            hakemusDto.getJonosija(),
                                Formatter.suomennaNumero(hakemusDto.getPisteet()),
                                HakemusUtil.tilaConverter(
                                        hakemusDto.getTila(),
                                        preferoitukielikoodi,
                                        hakemusDto.isHyvaksyttyHarkinnanvaraisesti(),
                                        false,
                                        true,
                                        hakemusDto.getVarasijanNumero(),
                                        ehdollisenHyvaksymisenEhto
                                ),
                                hylkayksenSyy,
                                valintaTieto,
                                ilmoittautumistieto,
                                muokattu(hakemusDto.getTilaHistoria()
                            )
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

    private String hakemusOidJaValintatapajonoOidYhdiste(String hakemusOid, String valintatapajonoOid) {
        return new StringBuilder().append(hakemusOid).append("_").append(valintatapajonoOid).toString();
    }
    private Map<String, IlmoittautumisTila> valintatapajononTilat(List<Valintatulos> tilat) {
        Map<String, IlmoittautumisTila> t = Maps.newHashMap();
        try {
            for (Valintatulos tulos : tilat) {
                t.put(hakemusOidJaValintatapajonoOidYhdiste(tulos.getHakemusOid(), tulos.getValintatapajonoOid()), tulos.getIlmoittautumisTila());
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

    /**
     * Palauttaa hakukohteen hakijaryhmät, joilla on hakemuksia.
     *
     * @param hakukohde HakukohdeDTO
     * @return Lista HakijaryhmaDTO-olioita
     */
    private List<HakijaryhmaDTO> getHakijaryhmatWithHakemuksia(HakukohdeDTO hakukohde) {
        return ofNullable(hakukohde.getHakijaryhmat()).orElse(Collections.emptyList())
                .stream()
                .filter(h -> h.getHakemusOid() != null && !h.getHakemusOid().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Palauttaa mäppäyksen Hakijaryhmän hakemusOid:n ja siihen liittyvien hakijaryhmien nimien välillä.
     *
     * @param hakijaryhmat Lista HakijaryhmaDTO-olioita
     * @return Map, jossa hakemusOid mäpätty listaan, jossa valintaryhmien nimet
     */
    private Map<String,List<String>> mapHakemusOidToHakijaryhmaNimet(List<HakijaryhmaDTO> hakijaryhmat) {
        Map<String, List<String>> mapHakemusOidToHakijaryhmaNimet = new HashMap<>();
        for (HakijaryhmaDTO hakijaryhma : hakijaryhmat) {
            for (String hakemusOid : hakijaryhma.getHakemusOid()) {
                List<String> currentHakemusHakijaryhmat = mapHakemusOidToHakijaryhmaNimet.get(hakemusOid);
                if (currentHakemusHakijaryhmat == null) {
                    currentHakemusHakijaryhmat = new ArrayList<>();
                }
                currentHakemusHakijaryhmat.add(hakijaryhma.getNimi());
                mapHakemusOidToHakijaryhmaNimet.put(hakemusOid, currentHakemusHakijaryhmat);
            }
        }
        return mapHakemusOidToHakijaryhmaNimet;
    }

    /**
     * Järjestää listan hakemuksia niiden tilan mukaan.
     *
     * @param hakemukset Lista HakemusDTO-olioita
     */
    private void sortHakemuksetByTila(List<HakemusDTO> hakemukset) {
        Collections.sort(hakemukset,
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
    }
}
