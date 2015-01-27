package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.util;

import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakemusDTO;
import fi.vm.sade.sijoittelu.tulos.dto.HakukohdeDTO;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.HakemusSijoitteluntulosMergeDto;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.MergeHakemusDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.MergeValinnanvaiheDTO;
import fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.dto.MergeValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;
import scala.annotation.meta.companionClass;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jussija on 26/01/15.
 */
public class HakemusSijoitteluntulosMergeUtil {

    public static List<MergeValinnanvaiheDTO> merge(
            List<Hakemus> hakemukset,
            HakukohdeDTO hakukohdeDTO,
            List<ValinnanVaiheJonoillaDTO> valinnanvaiheet,
            List<ValintatietoValinnanvaiheDTO> laskennantulokset,
            Map<Long,HakukohdeDTO> hakukohteetBySijoitteluAjoId
    // <- empty map jos ei erillissijoittelun hakukohteita
    ) {

        // Pelkät hakemukset tai ei mitään dataa
        if(laskennantulokset.isEmpty() && hakukohdeDTO.getValintatapajonot().isEmpty()) {
            MergeValinnanvaiheDTO dto = createValinnanvaihe(0);
            MergeValintatapajonoDTO jonoDTO = new MergeValintatapajonoDTO();
            List<MergeHakemusDTO> luodut = hakemukset.stream().map(h -> luo(Optional.of(h))).collect(Collectors.toList());
            jonoDTO.setHakemukset(luodut);
            dto.getValintatapajonot().add(jonoDTO);
            return Arrays.asList(dto);
        }
        // Ei laskennan tuloksia, generoidaan valinnanvaihe sijoittelun tuloksille ja mergataan hakemukset
        else if(laskennantulokset.isEmpty()) {
            MergeValinnanvaiheDTO dto = createValinnanvaihe(0);
            List<MergeValintatapajonoDTO> valintatapajonot = hakukohdeDTO
                    .getValintatapajonot()
                    .stream()
                    .map(jono -> {
                        MergeValintatapajonoDTO jonoDTO = luoSijoittelunTiedoista(jono);
                        jonoDTO.setHakemukset(mergaaSijoittelusta(hakemukset, jono.getHakemukset()));
                        return jonoDTO;
                    }).collect(Collectors.toList());
            dto.setValintatapajonot(valintatapajonot);
            return Arrays.asList(dto);
        }
        // Ei sijoittelun tuloksia, yhdistetään hakemukset ja laskennan tulokset
        else if(hakukohdeDTO == null || hakukohdeDTO.getValintatapajonot().isEmpty()) {
            List<MergeValinnanvaiheDTO> mergatutValinnanvaiheet = laskennantulokset.stream().map(vv -> {
                MergeValinnanvaiheDTO dto = createValinnanvaihe(vv);
                List<MergeValintatapajonoDTO> valintatapajonot = vv.getValintatapajonot().stream().map(jono -> {
                    MergeValintatapajonoDTO jonoDTO = luoLaskennanTiedoista(jono);
                    jonoDTO.setHakemukset(mergaaLaskennasta(hakemukset, jono.getJonosijat()));
                    // TODO mergaa erillissijoittelut
                    if(jonoDTO.getSijoitteluajoId() != null && hakukohteetBySijoitteluAjoId.containsKey(jonoDTO.getSijoitteluajoId())) {
                        Optional<ValintatapajonoDTO> sJono = hakukohteetBySijoitteluAjoId.get(jonoDTO.getSijoitteluajoId())
                                .getValintatapajonot()
                                .stream()
                                .filter(j -> j.getOid().equals(jonoDTO.getOid()))
                                .findFirst();
                        if(sJono.isPresent()) {
                            asetaSijoittelunTiedoista(jonoDTO, sJono.get());
                        }
                    }
                    return jonoDTO;
                }).collect(Collectors.toList());
                dto.setValintatapajonot(valintatapajonot);
                return dto;
            }).collect(Collectors.toList());

            return mergatutValinnanvaiheet;
        }
        // Laskenta ja sijoittelu löytyi
        else {
            List<MergeValinnanvaiheDTO> mergatutValinnanvaiheet = laskennantulokset.stream().map(vv -> {
                MergeValinnanvaiheDTO dto = createValinnanvaihe(vv);
                List<MergeValintatapajonoDTO> valintatapajonot = vv.getValintatapajonot().stream().map(jono -> {
                    MergeValintatapajonoDTO jonoDTO = luoLaskennanTiedoista(jono);
                    jonoDTO.setHakemukset(mergaaLaskennasta(hakemukset, jono.getJonosijat()));
                    Optional<ValintatapajonoDTO> sJono = hakukohdeDTO.getValintatapajonot()
                            .stream()
                            .filter(j -> j.getOid().equals(jonoDTO.getOid()))
                            .findFirst();

                    if (sJono.isPresent()) {
                        asetaSijoittelunTiedoista(jonoDTO, sJono.get());
                    }
                    return jonoDTO;
                }).collect(Collectors.toList());
                dto.setValintatapajonot(valintatapajonot);
                return dto;
            }).collect(Collectors.toList());

            return mergatutValinnanvaiheet;
        }

    }

    private static MergeValinnanvaiheDTO createValinnanvaihe(int jarjestysnumero) {
        MergeValinnanvaiheDTO dto = new MergeValinnanvaiheDTO();
        dto.setJarjestysnumero(jarjestysnumero);
        return dto;
    }

    private static MergeValinnanvaiheDTO createValinnanvaihe(ValintatietoValinnanvaiheDTO vv) {
        MergeValinnanvaiheDTO dto = new MergeValinnanvaiheDTO();
        dto.setJarjestysnumero(vv.getJarjestysnumero());
        dto.setHakuOid(vv.getHakuOid());
        dto.setNimi(vv.getNimi());
        dto.setValinnanvaiheoid(vv.getValinnanvaiheoid());
        return dto;
    }

    private static MergeValintatapajonoDTO luoSijoittelunTiedoista(ValintatapajonoDTO jono) {
        MergeValintatapajonoDTO jonoDTO = new MergeValintatapajonoDTO();
        jonoDTO.setAlinHyvaksyttyPistemaara(jono.getAlinHyvaksyttyPistemaara());
        jonoDTO.setAloituspaikat(jono.getAloituspaikat());
        jonoDTO.setEiVarasijatayttoa(jono.getEiVarasijatayttoa());
        jonoDTO.setHakeneet(jono.getHakeneet());
        jonoDTO.setHyvaksytty(jono.getHyvaksytty());
        jonoDTO.setKaikkiEhdonTayttavatHyvaksytaan(jono.getKaikkiEhdonTayttavatHyvaksytaan());
        jonoDTO.setNimi(jono.getNimi());
        jonoDTO.setOid(jono.getOid());
        jonoDTO.setPoissaOlevaTaytto(jono.getPoissaOlevaTaytto());
        jonoDTO.setPrioriteetti(jono.getPrioriteetti());
        jonoDTO.setTasasijasaanto(jono.getTasasijasaanto());
        jonoDTO.setVaralla(jono.getVaralla());
        return jonoDTO;
    }

    private static void asetaSijoittelunTiedoista(MergeValintatapajonoDTO jonoDTO, ValintatapajonoDTO jono) {
        jonoDTO.setAlinHyvaksyttyPistemaara(jono.getAlinHyvaksyttyPistemaara());
        jonoDTO.setAloituspaikat(jono.getAloituspaikat());
        jonoDTO.setEiVarasijatayttoa(jono.getEiVarasijatayttoa());
        jonoDTO.setHakeneet(jono.getHakeneet());
        jonoDTO.setHyvaksytty(jono.getHyvaksytty());
        jonoDTO.setKaikkiEhdonTayttavatHyvaksytaan(jono.getKaikkiEhdonTayttavatHyvaksytaan());
        jonoDTO.setNimi(jono.getNimi());
        jonoDTO.setOid(jono.getOid());
        jonoDTO.setPoissaOlevaTaytto(jono.getPoissaOlevaTaytto());
        jonoDTO.setPrioriteetti(jono.getPrioriteetti());
        jonoDTO.setTasasijasaanto(jono.getTasasijasaanto());
        jonoDTO.setVaralla(jono.getVaralla());

        mergaaLaskentaJaSijoittelu(jonoDTO.getHakemukset(), jono.getHakemukset());
    }

    private static MergeValintatapajonoDTO luoLaskennanTiedoista(ValintatietoValintatapajonoDTO jono) {
        MergeValintatapajonoDTO jonoDTO = new MergeValintatapajonoDTO();
        jonoDTO.setNimi(jono.getNimi());
        jonoDTO.setOid(jono.getOid());
        jonoDTO.setPrioriteetti(jono.getPrioriteetti());
        jonoDTO.setSiirretaanSijoitteluun(jono.isSiirretaanSijoitteluun());
        jonoDTO.setKaytetaanValintalaskentaa(jono.getKaytetaanValintalaskentaa());
        jonoDTO.setValmisSijoiteltavaksi(jono.getValmisSijoiteltavaksi());
        jonoDTO.setSijoitteluajoId(jono.getSijoitteluajoId());

        return jonoDTO;
    }

    private static List<MergeHakemusDTO> mergaaSijoittelusta(List<Hakemus> hakemukset, List<HakemusDTO> sijoittelunHakemukset) {
        final Map<String, Hakemus> hakemusOidMap = hakemusOidMap(hakemukset);
        final Set<String> sijoitteluOidSet = hakemusOiditSijoittelusta(sijoittelunHakemukset);

        // Hakemuspalvelun palauttamat hakemukset, jotka puuttuvat sijoittelun tuloksista
        Set<String> puuttuvatOidit =puuttuvatOidit(hakemusOidMap.keySet(), sijoitteluOidSet);

        // Asetetaan sijoittelun tulokset
        List<MergeHakemusDTO> mergatut = sijoittelunHakemukset
                .stream()
                .map(h -> asetaSijoittelunTiedot(luo(Optional.ofNullable(hakemusOidMap.get(h.getHakemusOid()))), h))
                .collect(Collectors.toList());

        // Luodaan sijoittelun tuloksista puuttuvat hakemukset
        List<MergeHakemusDTO> puuttuvat = puuttuvatOidit
                .stream()
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
        List<MergeHakemusDTO> mergatut = laskennanHakemukset
                .stream()
                .map(h -> asetaLaskennanTiedot(luo(Optional.ofNullable(hakemusOidMap.get(h.getHakemusOid()))), h))
                .collect(Collectors.toList());

        // Luodaan sijoittelun tuloksista puuttuvat hakemukset
        List<MergeHakemusDTO> puuttuvat = puuttuvatOidit
                .stream()
                .map(oid -> luo(Optional.of(hakemusOidMap.get(oid))))
                .collect(Collectors.toList());

        mergatut.addAll(puuttuvat);

        return mergatut;
    }

    private static void mergaaLaskentaJaSijoittelu(List<MergeHakemusDTO> hakemukset, List<HakemusDTO> sijoittelunHakemukset) {
        final Set<String> mergatutOidSet = hakemusOiditMergatuista(hakemukset);

        sijoittelunHakemukset.forEach(h -> {
            if(!mergatutOidSet.contains(h.getHakemusOid())) {
                hakemukset.add(asetaSijoittelunTiedot(luo(Optional.empty()), h));
            } else {
                MergeHakemusDTO mergeHakemusDTO = hakemukset.stream().filter(hakemus -> hakemus.getHakemusOid().equals(h.getHakemusOid())).findFirst().get();
                asetaSijoittelunTiedot(mergeHakemusDTO, h);
            }
        });

    }

    private static Set<String> puuttuvatOidit(Set<String> hakemusOidit, Set<String> vertailtava) {
        return hakemusOidit.stream().filter(oid -> !vertailtava.contains(oid)).collect(Collectors.toSet());
    }

    private static MergeHakemusDTO luo(Optional<Hakemus> hakemus) {
        MergeHakemusDTO dto = new MergeHakemusDTO();
        if(hakemus.isPresent()) {
            HakemusWrapper wrapper = new HakemusWrapper(hakemus.get());
            dto.setLoytyiHakemuksista(true);
            dto.setEtunimi(wrapper.getEtunimi());
            dto.setSukunimi(wrapper.getSukunimi());
            dto.setSahkoposti(wrapper.getSahkopostiOsoite());
            dto.setHakemusOid(hakemus.get().getOid());
            dto.setHakijaOid(hakemus.get().getPersonOid());
            dto.setHenkilotunnus(wrapper.getHenkilotunnus());
        }
        return dto;
    }

    private static MergeHakemusDTO asetaSijoittelunTiedot(MergeHakemusDTO dto, HakemusDTO h) {

        // Asetetaan perustiedot jos hakemusta ei löytynyt
        if(!dto.isLoytyiHakemuksista() && !dto.isLoytyiLaskennasta()) {
            dto.setEtunimi(h.getEtunimi());
            dto.setHakemusOid(h.getHakemusOid());
            dto.setHakijaOid(h.getHakijaOid());
            dto.setSukunimi(h.getSukunimi());
        }

        dto.setLoytyiSijoittelusta(true);
        dto.setJonosija(h.getJonosija());
        dto.setPrioriteetti(h.getPrioriteetti());
        dto.setTasasijaJonosija(h.getTasasijaJonosija());
        dto.setPaasyJaSoveltuvuusKokeenTulos(h.getPaasyJaSoveltuvuusKokeenTulos());
        dto.setPisteet(h.getPisteet());

        // TODO valintatulokset
        return dto;
    }

    private static MergeHakemusDTO asetaLaskennanTiedot(MergeHakemusDTO dto, JonosijaDTO j) {

        // Asetetaan perustiedot jos hakemusta ei löytynyt
        if(!dto.isLoytyiHakemuksista()) {
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
        return hakemukset
                .stream()
                .collect(Collectors.toMap(
                        Hakemus::getOid,
                        h -> h,
                        (h, n) -> n));
    }

    private static Set<String> hakemusOiditSijoittelusta(List<HakemusDTO> sijoittelunHakemukset) {
        return sijoittelunHakemukset
                .stream()
                .map(HakemusDTO::getHakemusOid)
                .collect(Collectors.toSet());
    }

    private static Set<String> hakemusOiditLaskennasta(List<JonosijaDTO> laskennanHakemukset) {
        return laskennanHakemukset
                .stream()
                .map(JonosijaDTO::getHakemusOid)
                .collect(Collectors.toSet());
    }

    private static Set<String> hakemusOiditMergatuista(List<MergeHakemusDTO> laskennanHakemukset) {
        return laskennanHakemukset
                .stream()
                .map(MergeHakemusDTO::getHakemusOid)
                .collect(Collectors.toSet());
    }

}
