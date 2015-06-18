package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;

public class PistesyotonTuontiTestBase {
    final Logger LOG = LoggerFactory.getLogger(getClass());

    String pistesyottoResurssi(String resurssi) throws IOException {
        InputStream i;
        String s = IOUtils.toString(i = new ClassPathResource("pistesyotto/" + resurssi).getInputStream(), "UTF-8");
        IOUtils.closeQuietly(i);
        return s;
    }

    List<Hakemus> lueHakemukset(final String tiedosto) throws IOException {
        return new Gson().fromJson(
            pistesyottoResurssi(tiedosto),
            new TypeToken<ArrayList<Hakemus>>() {
            }.getType());
    }

    List<ApplicationAdditionalDataDTO> luePistetiedot(final String tiedosto) throws IOException {
        return new Gson().fromJson(
            pistesyottoResurssi(tiedosto),
            new TypeToken<ArrayList<ApplicationAdditionalDataDTO>>() {
            }.getType());
    }

    List<ValintaperusteDTO> lueValintaperusteet(final String tiedosto) throws IOException {
        return new Gson().fromJson(
            pistesyottoResurssi(tiedosto),
            new TypeToken<ArrayList<ValintaperusteDTO>>() {
            }.getType());
    }

    List<ValintakoeOsallistuminenDTO> lueOsallistumisTiedot(final String tiedostonimi) throws IOException {
        List<ValintakoeOsallistuminenDTO> osallistumistiedot;
        String s = null;
        try {
            s = pistesyottoResurssi(tiedostonimi);
            osallistumistiedot = new GsonBuilder()
                .registerTypeAdapter(Date.class, new JsonDeserializer() {
                    @Override
                    public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        try {
                            return new Date(json.getAsJsonPrimitive().getAsLong());
                        } catch (Exception e) {
                            return new Gson().fromJson(json, Date.class);
                        }
                    }
                })
                .create()
                .fromJson(s, new TypeToken<ArrayList<ValintakoeOsallistuminenDTO>>() {
                }.getType());
        } catch (Exception e) {
            LOG.error("\r\n{}\r\n", s);
            throw e;
        }
        return osallistumistiedot;
    }

    Collection<String> getValintakoeTunnisteet(final List<ValintaperusteDTO> valintaperusteet) {
        return FluentIterable
            .from(valintaperusteet)
            .transform(
                new Function<ValintaperusteDTO, String>() {
                    @Override
                    public String apply(
                        ValintaperusteDTO input) {
                        return input.getTunniste();
                    }
                }).toList();
    }

    Map<String, ApplicationAdditionalDataDTO> asMap(
        Collection<ApplicationAdditionalDataDTO> datas) {
        Map<String, ApplicationAdditionalDataDTO> mapping = Maps.newHashMap();
        for (ApplicationAdditionalDataDTO data : datas) {
            mapping.put(data.getOid(), data);
        }
        return mapping;
    }

    PistesyottoDataRiviKuuntelija getPistesyottoDataRiviKuuntelija() {
        return new PistesyottoDataRiviKuuntelija() {
            @Override
            public void pistesyottoDataRiviTapahtuma(
                PistesyottoRivi pistesyottoRivi) {
                if (!pistesyottoRivi.isValidi()) {
                    for (PistesyottoArvo arvo : pistesyottoRivi.getArvot()) {
                        if (!arvo.isValidi()) {
                            String virheIlmoitus = new StringBuffer()
                                .append("Henkilöllä ")
                                .append(pistesyottoRivi.getNimi())
                                    //
                                .append(" (")
                                .append(pistesyottoRivi.getOid())
                                .append(")")
                                    //
                                .append(" oli virheellinen arvo '")
                                .append(arvo.getArvo()).append("'")
                                .append(" kohdassa ")
                                .append(arvo.getTunniste()).toString();

                            LOG.error("{}", virheIlmoitus);
                        }
                    }
                } else {
                    LOG.error("{}", pistesyottoRivi);
                }
            }
        };
    }

    void muplaa(final PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri, final Map<String, ApplicationAdditionalDataDTO> pistetiedotMapping) {
        List<ApplicationAdditionalDataDTO> uudetPistetiedot = Lists.newArrayList();

        for (PistesyottoRivi rivi : pistesyottoTuontiAdapteri
            .getRivit()) {
            ApplicationAdditionalDataDTO additionalData = pistetiedotMapping
                .get(rivi.getOid());
            Map<String, String> originalPistetiedot = additionalData
                .getAdditionalData();

            Map<String, String> newPistetiedot = rivi
                .asAdditionalData();
            if (originalPistetiedot.equals(newPistetiedot)) {
                LOG.debug("Ei muutoksia riville({},{})",
                    rivi.getOid(), rivi.getNimi());
            } else {
                if (rivi.isValidi()) {
                    LOG.debug("Rivi on muuttunut ja eheä. Tehdään päivitys hakupalveluun");
                    Map<String, String> uudetTiedot = Maps
                        .newHashMap(originalPistetiedot);
                    uudetTiedot.putAll(newPistetiedot);
                    additionalData
                        .setAdditionalData(uudetTiedot);
                    uudetPistetiedot.add(additionalData);
                } else {
                    LOG.warn("Rivi on muuttunut mutta viallinen joten ilmoitetaan virheestä!");

                    for (PistesyottoArvo arvo : rivi.getArvot()) {
                        if (!arvo.isValidi()) {
                            String virheIlmoitus = new StringBuffer()
                                .append("Henkilöllä ")
                                .append(rivi.getNimi())
                                    //
                                .append(" (")
                                .append(rivi.getOid())
                                .append(")")
                                    //
                                .append(" oli virheellinen arvo '")
                                .append(arvo.getArvo())
                                .append("'")
                                .append(" kohdassa ")
                                .append(arvo.getTunniste())
                                .toString();

                            throw new RuntimeException(
                                virheIlmoitus);
                        }
                    }

                }

            }
        }
    }

    void tallenna(final Excel excel) throws IOException {
        IOUtils.copy(excel.vieXlsx(), new FileOutputStream("pistesyotto.xlsx"));
    }

}
