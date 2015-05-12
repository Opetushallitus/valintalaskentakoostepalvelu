package fi.vm.sade.valinta.kooste.valintatapajono.service;

        import com.google.common.collect.Lists;
        import com.google.common.collect.Maps;
        import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
        import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
        import fi.vm.sade.valinta.kooste.util.EnumConverter;
        import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRivi;
        import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoRiviAsJonosijaConverter;
        import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
        import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
        import fi.vm.sade.valintalaskenta.domain.dto.ValintatapajonoDTO;
        import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.Tasasijasaanto;
        import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
        import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValintatapajonoDTO;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        import java.util.*;
        import java.util.stream.Collectors;

/**
 * @author Jussi Jartamo
 */
public class ValintatapajonoTuontiConverter {
    private static final Logger LOG = LoggerFactory.getLogger(ValintatapajonoTuontiConverter.class);

    public static ValinnanvaiheDTO konvertoi(
            String hakuOid,
            String hakukohdeOid,
            String valintatapajonoOid,
            List<ValinnanVaiheJonoillaDTO> valintaperusteet,
            List<Hakemus> hakemukset,
            List<ValintatietoValinnanvaiheDTO> valinnanvaiheet,
            Collection<ValintatapajonoRivi> rivit) {
        ValintatietoValinnanvaiheDTO vaihe = haeValinnanVaihe(
                valintatapajonoOid, valinnanvaiheet);
        if (vaihe == null) {
            vaihe = luoValinnanVaihe(valintaperusteet, hakukohdeOid, hakuOid,
                    Optional.ofNullable(valintatapajonoOid));

        }
        ValintatietoValintatapajonoDTO jono = haeValintatapajono(
                valintatapajonoOid, vaihe);
        vaihe.setValintatapajonot(Arrays.asList(jono));
        List<JonosijaDTO> jonosijat = Lists.newArrayList();
        Map<String, Hakemus> hakemusmappaus = mapHakemukset(hakemukset);
        for (ValintatapajonoRivi rivi : rivit) {

            if (rivi.isValidi()) {
                jonosijat
                        .add(ValintatapajonoRiviAsJonosijaConverter
                                .convert(
                                        hakukohdeOid,
                                        rivi,
                                        hakemusmappaus.get(rivi
                                                .getOid())));
            } else {
                LOG.warn("Rivi ei ole validi {} {} {}",
                        rivi.getOid(), rivi.getJonosija(),
                        rivi.getNimi());

            }
        }
        jono.setJonosijat(jonosijat);
        // LOG.error("\r\n{}", new GsonBuilder()
        // .setPrettyPrinting().create().toJson(vaihe));
        vaihe.setHakuOid(hakuOid);// jos valintalaskenta
        // osaa palauttaa
        // hakuOidittoman
        // vastauksen mutta ei
        // ottaa vastaan
        // sellaista
        vaihe.setCreatedAt(null); // ei konvertteria
        return vaihe;
    }
    private static ValintatietoValinnanvaiheDTO haeValinnanVaihe(
            String valintatapajonoOid,
            Collection<ValintatietoValinnanvaiheDTO> v) {
        for (ValintatietoValinnanvaiheDTO v0 : v) {
            if (haeValintatapajono(valintatapajonoOid, v0) != null) {
                return v0;
            }
        }
        return null;
    }

    private static ValintatietoValintatapajonoDTO haeValintatapajono(String valintatapajonoOid,
                                                  ValintatietoValinnanvaiheDTO v) {
        for (ValintatietoValintatapajonoDTO vx : v.getValintatapajonot()) {
            if (valintatapajonoOid.equals(vx.getValintatapajonooid())) {
                return vx;
            }
        }
        return null;
    }

    private static ValintatietoValinnanvaiheDTO luoValinnanVaihe(
            final List<ValinnanVaiheJonoillaDTO> ilmanLaskentaaVaiheet,
            String hakukohdeOid,
            String hakuOid, Optional<String> valintatapajonoOid) {

        // = valintaperusteetResource.ilmanLaskentaa(hakukohdeOid).get();
        List<fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO> ilmanLaskentaaJonot;

        String jonoOid;

        if(!valintatapajonoOid.isPresent()) {
            ilmanLaskentaaJonot =
                    ilmanLaskentaaVaiheet
                            .stream()
                            .flatMap(v -> v.getJonot().stream())
                            .collect(Collectors.toList());
            if(ilmanLaskentaaJonot.isEmpty()) {
                throw new RuntimeException(
                        "Yhtään valintatapajonoa ilman laskentaa ei löytynyt");
            }

            if(ilmanLaskentaaJonot.size() > 1) {
                throw new RuntimeException(
                        "ValintatapajonoOidia ei annettu ja löytyi useampia kuin yksi valintatapajono ilman laskentaa");
            }

            jonoOid = ilmanLaskentaaJonot.get(0).getOid();
        } else {
            jonoOid = valintatapajonoOid.get();
        }

        ValinnanVaiheJonoillaDTO vaihe = haeVaihe(jonoOid,
                ilmanLaskentaaVaiheet);
        if (vaihe == null) {
            throw new RuntimeException(
                    "Tälle valintatapajonolle ei löydy valintaperusteista valinnanvaihetta!");
        }
        fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO jono = haeJono(
                jonoOid, vaihe);

        // luodaan uusi
        LOG.warn(
                "Valinnanvaihetta ei löytynyt valintatapajonolle({}) joten luodaan uusi!",
                valintatapajonoOid);
        ValintatietoValinnanvaiheDTO v0 = new ValintatietoValinnanvaiheDTO();
        v0.setCreatedAt(new Date());
        v0.setHakuOid(hakuOid);
        v0.setJarjestysnumero(0);
        v0.setNimi(vaihe.getNimi());
        v0.setValinnanvaiheoid(vaihe.getOid());
        ValintatietoValintatapajonoDTO vx = new ValintatietoValintatapajonoDTO();
        vx.setAloituspaikat(jono.getAloituspaikat());
        vx.setEiVarasijatayttoa(jono.getEiVarasijatayttoa());
        vx.setKaikkiEhdonTayttavatHyvaksytaan(jono
                .getKaikkiEhdonTayttavatHyvaksytaan());
        vx.setKaytetaanValintalaskentaa(jono.getKaytetaanValintalaskentaa());
        vx.setNimi(jono.getNimi());
        vx.setOid(jonoOid);
        vx.setPoissaOlevaTaytto(jono.getPoissaOlevaTaytto());
        vx.setPrioriteetti(jono.getPrioriteetti());
        vx.setSiirretaanSijoitteluun(jono.getSiirretaanSijoitteluun());
        vx.setTasasijasaanto(EnumConverter.convert(Tasasijasaanto.class,
                jono.getTasapistesaanto()));
        vx.setValintatapajonooid(jonoOid);
        v0.getValintatapajonot().add(vx);
        return v0;
    }

    private static Map<String, Hakemus> mapHakemukset(Collection<Hakemus> hakemukset) {
        Map<String, Hakemus> tmp = Maps.newHashMap();
        for (Hakemus h : hakemukset) {
            tmp.put(h.getOid(), h);
        }
        return tmp;
    }

    private static ValinnanVaiheJonoillaDTO haeVaihe(String oid,
                                              List<ValinnanVaiheJonoillaDTO> jonot) {
        for (ValinnanVaiheJonoillaDTO jonoilla : jonot) {
            for (fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO v : jonoilla
                    .getJonot()) {
                if (oid.equals(v.getOid())) {
                    return jonoilla;
                }
            }
        }
        return null;
    }

    private static fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO haeJono(
            String oid, ValinnanVaiheJonoillaDTO vaihe) {

        for (fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO v : vaihe
                .getJonot()) {
            if (oid.equals(v.getOid())) {
                return v;
            }
        }

        return null;
    }
}
