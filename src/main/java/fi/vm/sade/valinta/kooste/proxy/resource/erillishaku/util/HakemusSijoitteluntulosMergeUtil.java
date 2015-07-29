package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.util;

import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.MergeHakemusDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.MergeValinnanvaiheDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.MergeValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;

import java.util.*;
import java.util.stream.Collectors;

import static fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.util.PseudoSatunnainenOID.oidHaustaJaHakukohteesta;
import static fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.util.PseudoSatunnainenOID.trimToNull;

public class HakemusSijoitteluntulosMergeUtil {

    public static List<MergeValinnanvaiheDTO> merge(
            String hakuOid,
            String hakukohdeOid,
            List<Hakemus> hakemukset,
            HakukohdeDTO hakukohdeDTO,
            List<ValinnanVaiheJonoillaDTO> valinnanvaiheet,
            List<ValintatietoValinnanvaiheDTO> laskennantulokset,
            Map<Long, HakukohdeDTO> hakukohteetBySijoitteluAjoId,
            List<Valintatulos> valintatulosDtos) {
        List<MergeValinnanvaiheDTO> result = getMergeValinnanvaiheDTOs(hakuOid, hakukohdeOid, valinnanvaiheet);

        boolean laskennanTulosLoytyi = laskennantulokset.stream()
                .flatMap(v -> v.getValintatapajonot().stream())
                .flatMap(j -> j.getJonosijat().stream())
                .findAny()
                .isPresent();

        if (!laskennanTulosLoytyi && hakukohdeDTO.getValintatapajonot().isEmpty()) {
            handlePelkatHakemuksetTaiEiMitaanDataa(hakemukset, result);
        } else if (!laskennanTulosLoytyi) {
            handleEiLaskennanTuloksia(hakuOid, hakukohdeOid, hakemukset, hakukohdeDTO, valintatulosDtos, result);
        } else if (hakukohdeDTO == null || hakukohdeDTO.getValintatapajonot().isEmpty()) {
            handleEiSijoittelunTuloksia(hakuOid, hakukohdeOid, hakemukset, laskennantulokset, hakukohteetBySijoitteluAjoId, valintatulosDtos, result);
        } else {
            handleLaskentaJaSijoitteluOlemassa(hakuOid, hakukohdeOid, hakemukset, hakukohdeDTO, laskennantulokset, valintatulosDtos, result);
        }
        Collections.sort(result, (v1, v2) -> Integer.compare(v1.getJarjestysnumero(), v2.getJarjestysnumero()));
        result.get(result.size() - 1).setViimeinenVaihe(true);
        return result;
    }

    private static void handleLaskentaJaSijoitteluOlemassa(String hakuOid, String hakukohdeOid, List<Hakemus> hakemukset, HakukohdeDTO hakukohdeDTO, List<ValintatietoValinnanvaiheDTO> laskennantulokset, List<Valintatulos> valintatulosDtos, List<MergeValinnanvaiheDTO> result) {
        laskennantulokset.stream().forEach(vv -> {
            Optional<MergeValinnanvaiheDTO> opt = findVaihe(result, vv.getValinnanvaiheoid());

            MergeValinnanvaiheDTO dto;
            if (opt.isPresent()) {
                dto = opt.get();
                dto.setJarjestysnumero(vv.getJarjestysnumero());
            } else {
                result.add(createValinnanvaihe(hakuOid, hakukohdeOid, vv));
                dto = result.get(result.size() - 1);
            }
            vv.getValintatapajonot().stream().forEach(jono -> {
                Optional<MergeValintatapajonoDTO> jonoOpt = findJono(dto.getValintatapajonot(), jono.getOid());
                MergeValintatapajonoDTO jonoDTO;
                if (jonoOpt.isPresent()) {
                    jonoDTO = jonoOpt.get();
                    yhdistaLaskennanTiedoista(jonoDTO, jono);
                } else {
                    jonoDTO = luoLaskennanTiedoista(hakuOid, hakukohdeOid, jono);
                    dto.getValintatapajonot().add(jonoDTO);
                }
                jonoDTO.setHakemukset(mergaaLaskennasta(hakemukset, jono.getJonosijat()));
                Optional<ValintatapajonoDTO> sJono = hakukohdeDTO.getValintatapajonot()
                        .stream()
                        .filter(j -> j.getOid().equals(jonoDTO.getOid()))
                        .findFirst();

                if (sJono.isPresent()) {
                    asetaSijoittelunTiedoista(hakuOid, hakukohdeOid, jonoDTO, sJono.get(), valintatulosDtos);
                }
            });
        });
    }

    private static void handleEiSijoittelunTuloksia(String hakuOid, String hakukohdeOid, List<Hakemus> hakemukset, List<ValintatietoValinnanvaiheDTO> laskennantulokset, Map<Long, HakukohdeDTO> hakukohteetBySijoitteluAjoId, List<Valintatulos> valintatulosDtos, List<MergeValinnanvaiheDTO> result) {
        // Ei sijoittelun tuloksia, yhdistetään hakemukset ja laskennan tulokset
        laskennantulokset.stream().forEach(vv -> {
            Optional<MergeValinnanvaiheDTO> opt = findVaihe(result, vv.getValinnanvaiheoid());
            MergeValinnanvaiheDTO dto;
            if (opt.isPresent()) {
                dto = opt.get();
                dto.setJarjestysnumero(vv.getJarjestysnumero());
            } else {
                result.add(createValinnanvaihe(hakuOid, hakukohdeOid, vv));
                dto = result.get(result.size() - 1);
            }
            vv.getValintatapajonot().stream().forEach(jono -> {
                Optional<MergeValintatapajonoDTO> jonoOpt = findJono(dto.getValintatapajonot(), jono.getOid());
                MergeValintatapajonoDTO jonoDTO;
                if (jonoOpt.isPresent()) {
                    jonoDTO = jonoOpt.get();
                    yhdistaLaskennanTiedoista(jonoDTO, jono);
                } else {
                    jonoDTO = luoLaskennanTiedoista(hakuOid, hakukohdeOid, jono);
                    dto.getValintatapajonot().add(jonoDTO);
                }
                jonoDTO.setHakemukset(mergaaLaskennasta(hakemukset, jono.getJonosijat()));
                if (jonoDTO.getSijoitteluajoId() != null && hakukohteetBySijoitteluAjoId.containsKey(jonoDTO.getSijoitteluajoId())) {
                    Optional<ValintatapajonoDTO> sJono = hakukohteetBySijoitteluAjoId.get(jonoDTO.getSijoitteluajoId())
                            .getValintatapajonot()
                            .stream()
                            .filter(j -> j.getOid().equals(jonoDTO.getOid()))
                            .findFirst();
                    if (sJono.isPresent()) {
                        asetaSijoittelunTiedoista(hakuOid, hakukohdeOid, jonoDTO, sJono.get(), valintatulosDtos);
                    }
                }
            });
        });
    }

    private static void handleEiLaskennanTuloksia(String hakuOid, String hakukohdeOid, List<Hakemus> hakemukset, HakukohdeDTO hakukohdeDTO, List<Valintatulos> valintatulosDtos, List<MergeValinnanvaiheDTO> result) {
        List<MergeValintatapajonoDTO> valintatapajonot = hakukohdeDTO
                .getValintatapajonot()
                .stream()
                .map(jono -> {
                    MergeValintatapajonoDTO jonoDTO = luoSijoittelunTiedoista(hakuOid, hakukohdeOid, jono);
                    jonoDTO.setHakemukset(mergaaSijoittelusta(hakemukset, jono.getHakemukset(), valintatulosDtos));
                    jonoDTO.setKaytetaanValintalaskentaa(false);
                    return jonoDTO;
                }).collect(Collectors.toList());
        result.get(0).setValintatapajonot(valintatapajonot);
    }

    private static void handlePelkatHakemuksetTaiEiMitaanDataa(List<Hakemus> hakemukset, List<MergeValinnanvaiheDTO> result) {
        // Ei valintaperusteita
        if (result.get(0).getValintatapajonot().isEmpty()) {
            MergeValintatapajonoDTO jonoDTO = new MergeValintatapajonoDTO();
            jonoDTO.setKaytetaanValintalaskentaa(false);
            List<MergeHakemusDTO> luodut = hakemukset.stream().map(h -> luo(Optional.of(h))).collect(Collectors.toList());
            jonoDTO.setHakemukset(luodut);
            result.get(0).getValintatapajonot().add(jonoDTO);
        } else {
            result.forEach(v -> v.getValintatapajonot().forEach(j -> {
                List<MergeHakemusDTO> luodut = hakemukset.stream().map(h -> luo(Optional.of(h))).collect(Collectors.toList());
                j.setHakemukset(luodut);
            }));
        }
    }

    private static List<MergeValinnanvaiheDTO> getMergeValinnanvaiheDTOs(String hakuOid, String hakukohdeOid, List<ValinnanVaiheJonoillaDTO> valinnanvaiheet) {
        List<MergeValinnanvaiheDTO> result = new ArrayList<>();
        if (valinnanvaiheet.isEmpty()) {
            // Ei yhtään valinnanvaihetta, generoidaan yksi
            result.add(createValinnanvaihe(hakuOid, hakukohdeOid, 0));
        } else {
            valinnanvaiheet.forEach(vaihe -> {
                result.add(createValinnanvaihe(hakuOid, hakukohdeOid, vaihe));
            });
        }
        return result;
    }

    private static Optional<MergeValinnanvaiheDTO> findVaihe(List<MergeValinnanvaiheDTO> vaiheet, String valinnanvaiheOid) {
        return vaiheet.stream()
                .filter(Objects::nonNull)
                .filter(vaihe -> vaihe.getValinnanvaiheoid() != null && vaihe.getValinnanvaiheoid().equals(valinnanvaiheOid))
                .findFirst();
    }

    private static Optional<MergeValintatapajonoDTO> findJono(List<MergeValintatapajonoDTO> jonot, String jonoOid) {
        return jonot.stream()
                .filter(Objects::nonNull)
                .filter(j -> j.getOid() != null && j.getOid().equals(jonoOid))
                .findFirst();
    }

    private static MergeValinnanvaiheDTO createValinnanvaihe(String hakuOid, String hakukohdeOid, int jarjestysnumero) {
        MergeValinnanvaiheDTO dto = new MergeValinnanvaiheDTO();
        dto.setHakuOid(hakuOid);
        dto.setHakukohdeOid(hakukohdeOid);
        dto.setJarjestysnumero(jarjestysnumero);
        return dto;
    }

    private static MergeValinnanvaiheDTO createValinnanvaihe(String hakuOid, String hakukohdeOid, ValintatietoValinnanvaiheDTO vv) {
        MergeValinnanvaiheDTO dto = new MergeValinnanvaiheDTO();
        dto.setJarjestysnumero(vv.getJarjestysnumero());
        dto.setHakuOid(hakuOid);
        dto.setHakukohdeOid(hakukohdeOid);
        dto.setNimi(vv.getNimi());
        dto.setValinnanvaiheoid(vv.getValinnanvaiheoid());
        return dto;
    }

    private static MergeValinnanvaiheDTO createValinnanvaihe(String hakuOid, String hakukohdeOid, ValinnanVaiheJonoillaDTO vv) {
        MergeValinnanvaiheDTO dto = new MergeValinnanvaiheDTO();
        dto.setHakuOid(hakuOid);
        dto.setHakukohdeOid(hakukohdeOid);
        dto.setNimi(vv.getNimi());
        dto.setValinnanvaiheoid(vv.getOid());
        vv.getJonot().forEach(jono -> {
            MergeValintatapajonoDTO jonoDTO = new MergeValintatapajonoDTO();
            jonoDTO.setAloituspaikat(Optional.ofNullable(jono.getAloituspaikat()).orElse(0));
            jonoDTO.setEiVarasijatayttoa(Boolean.TRUE.equals(jono.getEiVarasijatayttoa()));
            jonoDTO.setKaikkiEhdonTayttavatHyvaksytaan(Boolean.TRUE.equals(jono.getKaikkiEhdonTayttavatHyvaksytaan()));
            jonoDTO.setNimi(jono.getNimi());
            jonoDTO.setOid(Optional.ofNullable(trimToNull(jono.getOid())).orElse(oidHaustaJaHakukohteesta(hakuOid, hakukohdeOid)));
            jonoDTO.setPoissaOlevaTaytto(Boolean.TRUE.equals(jono.getPoissaOlevaTaytto()));
            jonoDTO.setKaytetaanValintalaskentaa(Boolean.TRUE.equals(jono.getKaytetaanValintalaskentaa()));
            dto.getValintatapajonot().add(jonoDTO);
        });
        return dto;
    }

    private static MergeValintatapajonoDTO luoSijoittelunTiedoista(String hakuOid, String hakukohdeOid, ValintatapajonoDTO jono) {
        MergeValintatapajonoDTO jonoDTO = new MergeValintatapajonoDTO();
        jonoDTO.setAlinHyvaksyttyPistemaara(jono.getAlinHyvaksyttyPistemaara());
        jonoDTO.setAloituspaikat(jono.getAloituspaikat());
        jonoDTO.setEiVarasijatayttoa(Boolean.TRUE.equals(jono.getEiVarasijatayttoa()));
        jonoDTO.setHakeneet(jono.getHakeneet());
        jonoDTO.setHyvaksytty(jono.getHyvaksytty());
        jonoDTO.setKaikkiEhdonTayttavatHyvaksytaan(Boolean.TRUE.equals(jono.getKaikkiEhdonTayttavatHyvaksytaan()));
        jonoDTO.setNimi(jono.getNimi());
        jonoDTO.setOid(Optional.ofNullable(trimToNull(jono.getOid())).orElse(oidHaustaJaHakukohteesta(hakuOid, hakukohdeOid))); // "null" arvojen pois filtterointi
        jonoDTO.setPoissaOlevaTaytto(Boolean.TRUE.equals(jono.getPoissaOlevaTaytto()));
        jonoDTO.setPrioriteetti(jono.getPrioriteetti());
        jonoDTO.setTasasijasaanto(jono.getTasasijasaanto());
        jonoDTO.setVaralla(Optional.ofNullable(jono.getVaralla()).orElse(0));
        return jonoDTO;
    }

    private static void asetaSijoittelunTiedoista(String hakuOid, String hakukohdeOid, MergeValintatapajonoDTO jonoDTO, ValintatapajonoDTO jono, List<Valintatulos> valintatulosDtos) {
        jonoDTO.setAlinHyvaksyttyPistemaara(jono.getAlinHyvaksyttyPistemaara());
        jonoDTO.setAloituspaikat(jono.getAloituspaikat());
        jonoDTO.setEiVarasijatayttoa(Boolean.TRUE.equals(jono.getEiVarasijatayttoa()));
        jonoDTO.setHakeneet(jono.getHakeneet());
        jonoDTO.setHyvaksytty(Optional.ofNullable(jono.getHyvaksytty()).orElse(0));
        jonoDTO.setKaikkiEhdonTayttavatHyvaksytaan(Boolean.TRUE.equals(jono.getKaikkiEhdonTayttavatHyvaksytaan()));
        jonoDTO.setNimi(jono.getNimi());
        jonoDTO.setOid(Optional.ofNullable(trimToNull(jono.getOid())).orElse(oidHaustaJaHakukohteesta(hakuOid, hakukohdeOid)));
        jonoDTO.setPoissaOlevaTaytto(Boolean.TRUE.equals(jono.getPoissaOlevaTaytto()));
        jonoDTO.setPrioriteetti(Optional.ofNullable(jono.getPrioriteetti()).orElse(0));
        jonoDTO.setTasasijasaanto(jono.getTasasijasaanto());
        jonoDTO.setVaralla(Optional.ofNullable(jono.getVaralla()).orElse(0));

        mergaaLaskentaJaSijoittelu(jonoDTO.getHakemukset(), jono.getHakemukset(), valintatulosDtos);
    }

    private static MergeValintatapajonoDTO luoLaskennanTiedoista(String hakuOid, String hakukohdeOid, ValintatietoValintatapajonoDTO jono) {
        MergeValintatapajonoDTO jonoDTO = new MergeValintatapajonoDTO();
        jonoDTO.setNimi(jono.getNimi());
        jonoDTO.setOid(Optional.ofNullable(trimToNull(jono.getOid())).orElse(oidHaustaJaHakukohteesta(hakuOid, hakukohdeOid)));
        jonoDTO.setPrioriteetti(jono.getPrioriteetti());
        jonoDTO.setSiirretaanSijoitteluun(jono.isSiirretaanSijoitteluun());
        jonoDTO.setKaytetaanValintalaskentaa(true);
        jonoDTO.setValmisSijoiteltavaksi(jono.getValmisSijoiteltavaksi());
        jonoDTO.setSijoitteluajoId(jono.getSijoitteluajoId());

        return jonoDTO;
    }

    private static MergeValintatapajonoDTO yhdistaLaskennanTiedoista(MergeValintatapajonoDTO jonoDTO, ValintatietoValintatapajonoDTO jono) {
        jonoDTO.setPrioriteetti(jono.getPrioriteetti());
        jonoDTO.setSiirretaanSijoitteluun(Boolean.TRUE.equals(jono.isSiirretaanSijoitteluun()));
        jonoDTO.setKaytetaanValintalaskentaa(true);
        jonoDTO.setValmisSijoiteltavaksi(jono.getValmisSijoiteltavaksi());
        jonoDTO.setSijoitteluajoId(jono.getSijoitteluajoId());

        return jonoDTO;
    }

    private static List<MergeHakemusDTO> mergaaSijoittelusta(List<Hakemus> hakemukset, List<HakemusDTO> sijoittelunHakemukset, List<Valintatulos> valintatulosDtos) {
        final Map<String, Hakemus> hakemusOidMap = hakemusOidMap(hakemukset);
        final Set<String> sijoitteluOidSet = hakemusOiditSijoittelusta(sijoittelunHakemukset);

        // Hakemuspalvelun palauttamat hakemukset, jotka puuttuvat sijoittelun tuloksista
        Set<String> puuttuvatOidit = puuttuvatOidit(hakemusOidMap.keySet(), sijoitteluOidSet);

        // Asetetaan sijoittelun tulokset
        List<MergeHakemusDTO> mergatut = sijoittelunHakemukset.stream()
                .map(h -> asetaSijoittelunTiedot(luo(Optional.ofNullable(hakemusOidMap.get(h.getHakemusOid()))), h, haeValintatulos(valintatulosDtos, h)))
                .collect(Collectors.toList());

        // Luodaan sijoittelun tuloksista puuttuvat hakemukset
        List<MergeHakemusDTO> puuttuvat = puuttuvatOidit.stream()
                .map(oid -> luo(Optional.of(hakemusOidMap.get(oid))))
                .collect(Collectors.toList());

        mergatut.addAll(puuttuvat);
        return mergatut;
    }

    private static List<MergeHakemusDTO> mergaaLaskennasta(List<Hakemus> hakemukset, List<JonosijaDTO> laskennanHakemukset) {
        final Map<String, Hakemus> hakemusOidMap = hakemusOidMap(hakemukset);
        final Set<String> laskentaOidSet = hakemusOiditLaskennasta(laskennanHakemukset);

        // Hakemuspalvelun palauttamat hakemukset, jotka puuttuvat laskennan tuloksista
        Set<String> puuttuvatOidit = puuttuvatOidit(hakemusOidMap.keySet(), laskentaOidSet);

        // Asetetaan sijoittelun tulokset
        List<MergeHakemusDTO> mergatut = laskennanHakemukset.stream()
                .map(h -> asetaLaskennanTiedot(luo(Optional.ofNullable(hakemusOidMap.get(h.getHakemusOid()))), h))
                .collect(Collectors.toList());

        // Luodaan sijoittelun tuloksista puuttuvat hakemukset
        List<MergeHakemusDTO> puuttuvat = puuttuvatOidit.stream()
                .map(oid -> luo(Optional.of(hakemusOidMap.get(oid))))
                .collect(Collectors.toList());

        mergatut.addAll(puuttuvat);
        return mergatut;
    }

    private static void mergaaLaskentaJaSijoittelu(List<MergeHakemusDTO> hakemukset, List<HakemusDTO> sijoittelunHakemukset, List<Valintatulos> valintatulosDtos) {
        final Set<String> mergatutOidSet = hakemusOiditMergatuista(hakemukset);

        sijoittelunHakemukset.forEach(h -> {
            if (!mergatutOidSet.contains(h.getHakemusOid())) {
                hakemukset.add(asetaSijoittelunTiedot(luo(Optional.empty()), h, haeValintatulos(valintatulosDtos, h)));
            } else {
                MergeHakemusDTO mergeHakemusDTO = hakemukset.stream().filter(hakemus -> hakemus.getHakemusOid().equals(h.getHakemusOid())).findFirst().get();
                asetaSijoittelunTiedot(mergeHakemusDTO, h, haeValintatulos(valintatulosDtos, h));
            }
        });

    }

    private static Optional<Valintatulos> haeValintatulos(List<Valintatulos> valintatulosDtos, HakemusDTO h) {
        return valintatulosDtos.parallelStream()
                .filter(v -> v.getHakemusOid().equals(h.getHakemusOid()) && v.getValintatapajonoOid().equals(h.getValintatapajonoOid()))
                .findFirst();
    }

    private static Set<String> puuttuvatOidit(Set<String> hakemusOidit, Set<String> vertailtava) {
        return hakemusOidit.stream().filter(oid -> !vertailtava.contains(oid)).collect(Collectors.toSet());
    }

    private static MergeHakemusDTO luo(Optional<Hakemus> hakemus) {
        MergeHakemusDTO dto = new MergeHakemusDTO();
        if (hakemus.isPresent()) {
            HakemusWrapper wrapper = new HakemusWrapper(hakemus.get());
            dto.setEtunimi(wrapper.getEtunimi());
            dto.setSukunimi(wrapper.getSukunimi());
            dto.setSahkoposti(wrapper.getSahkopostiOsoite());
            dto.setHakemusOid(hakemus.get().getOid());
            dto.setSyntymaaika(wrapper.getSyntymaaika());
            dto.setHakijaOid(hakemus.get().getPersonOid());
            dto.setHenkilotunnus(wrapper.getHenkilotunnus());
            dto.setLoytyiHakemuksista(true);
        }
        return dto;
    }

    private static MergeHakemusDTO asetaSijoittelunTiedot(MergeHakemusDTO dto, HakemusDTO h, Optional<Valintatulos> valintatulos) {
        // Asetetaan perustiedot jos hakemusta ei löytynyt
        if (!dto.isLoytyiHakemuksista() && !dto.isLoytyiLaskennasta()) {
            dto.setEtunimi(h.getEtunimi());
            dto.setHakemusOid(h.getHakemusOid());
            dto.setHakijaOid(h.getHakijaOid());
            dto.setSukunimi(h.getSukunimi());
        }
        dto.setLoytyiSijoittelusta(true);
        dto.setJonosija(h.getJonosija());
        dto.setPrioriteetti(h.getPrioriteetti());
        dto.setTasasijaJonosija(Optional.ofNullable(h.getTasasijaJonosija()).orElse(0));
        dto.setPaasyJaSoveltuvuusKokeenTulos(h.getPaasyJaSoveltuvuusKokeenTulos());
        dto.setPisteet(h.getPisteet());
        dto.setHakemuksentila(h.getTila());
        if (valintatulos.isPresent()) {
            dto.setValintatuloksentila(valintatulos.get().getTila());
            dto.setIlmoittautumistila(valintatulos.get().getIlmoittautumisTila());
            dto.setJulkaistavissa(valintatulos.get().getJulkaistavissa());
            dto.setHyvaksyttyVarasijalta(valintatulos.get().getHyvaksyttyVarasijalta());
        }
        return dto;
    }

    private static MergeHakemusDTO asetaLaskennanTiedot(MergeHakemusDTO dto, JonosijaDTO j) {
        // Asetetaan perustiedot jos hakemusta ei löytynyt
        if (!dto.isLoytyiHakemuksista()) {
            dto.setEtunimi(j.getEtunimi());
            dto.setHakemusOid(j.getHakemusOid());
            dto.setHakijaOid(j.getHakijaOid());
            dto.setSukunimi(j.getSukunimi());
        }
        dto.setLoytyiLaskennasta(true);
        dto.setHylattyValisijoittelussa(j.isHylattyValisijoittelussa());
        dto.setMuokattu(j.isMuokattu());
        dto.setHarkinnanvarainen(j.isHarkinnanvarainen());
        dto.setJarjestyskriteerit(j.getJarjestyskriteerit());
        return dto;
    }

    private static Map<String, Hakemus> hakemusOidMap(List<Hakemus> hakemukset) {
        return hakemukset.stream()
                .collect(Collectors.toMap(Hakemus::getOid, h -> h, (h, n) -> n));
    }

    private static Set<String> hakemusOiditSijoittelusta(List<HakemusDTO> sijoittelunHakemukset) {
        return sijoittelunHakemukset.stream()
                .map(HakemusDTO::getHakemusOid)
                .collect(Collectors.toSet());
    }

    private static Set<String> hakemusOiditLaskennasta(List<JonosijaDTO> laskennanHakemukset) {
        return laskennanHakemukset.stream()
                .map(JonosijaDTO::getHakemusOid)
                .collect(Collectors.toSet());
    }

    private static Set<String> hakemusOiditMergatuista(List<MergeHakemusDTO> laskennanHakemukset) {
        return laskennanHakemukset.stream()
                .map(MergeHakemusDTO::getHakemusOid)
                .collect(Collectors.toSet());
    }
}
