package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockForward;
import static javax.ws.rs.HttpMethod.POST;
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
import fi.vm.sade.valinta.kooste.excel.ExcelValidointiPoikkeus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import fi.vm.sade.valinta.kooste.server.MockServer;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class PistesyotonTuontiTestBase {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private String pistesyottoResurssi(String resurssi) throws IOException {
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
        return valintaperusteet.stream().map(ValintaperusteDTO::getTunniste).collect(Collectors.toList());
    }

    void tuoExcel(final List<ValintakoeOsallistuminenDTO> osallistumistiedot, final List<ValintaperusteDTO> valintaperusteet, final List<ApplicationAdditionalDataDTO> pistetiedot, final String tiedosto, final String hakuOid, final String hakukohdeOid) throws IOException, ExcelValidointiPoikkeus {
        List<Hakemus> hakemukset = Collections.emptyList();
        Collection<String> valintakoeTunnisteet = getValintakoeTunnisteet(valintaperusteet);
        PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri = new PistesyottoDataRiviListAdapter();
        PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(hakuOid, hakukohdeOid, null, "Haku",
            "hakukohdeNimi", "tarjoajaNimi", hakemukset,
            Collections.emptySet(),
            valintakoeTunnisteet, osallistumistiedot,
            valintaperusteet, pistetiedot,
            Collections.singletonList(pistesyottoTuontiAdapteri));
        pistesyottoExcel.getExcel().tuoXlsx(new ClassPathResource("pistesyotto/" + tiedosto).getInputStream());
        muplaa(pistesyottoTuontiAdapteri, pistetiedot);
    }

    private void muplaa(final PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri, final List<ApplicationAdditionalDataDTO> pistetiedot) {
        Map<String, ApplicationAdditionalDataDTO> pistetiedotMapping = mapByOid(pistetiedot);
        List<ApplicationAdditionalDataDTO> uudetPistetiedot = Lists.newArrayList();
        for (PistesyottoRivi rivi : pistesyottoTuontiAdapteri.getRivit()) {
            ApplicationAdditionalDataDTO additionalData = pistetiedotMapping.get(rivi.getOid());
            Map<String, String> originalPistetiedot = additionalData.getAdditionalData();
            Map<String, String> newPistetiedot = rivi.asAdditionalData(t -> true);
            if (originalPistetiedot.equals(newPistetiedot)) {
                LOG.debug("Ei muutoksia riville({},{})", rivi.getOid(), rivi.getNimi());
            } else {
                if (rivi.isValidi()) {
                    Map<String, String> uudetTiedot = Maps.newHashMap(originalPistetiedot);
                    uudetTiedot.putAll(newPistetiedot);
                    additionalData.setAdditionalData(uudetTiedot);
                    uudetPistetiedot.add(additionalData);
                } else {
                    for (PistesyottoArvo arvo : rivi.getArvot()) {
                        if (!arvo.isValidi()) {
                            throw new RuntimeException("Henkilöllä " + rivi.getNimi() + " (" + rivi.getOid() + ")" + " oli virheellinen arvo '" + arvo.getArvo() + "' kohdassa " + arvo.getTunniste());
                        }
                    }
                }
            }
        }
    }

    private Map<String, ApplicationAdditionalDataDTO> mapByOid(Collection<ApplicationAdditionalDataDTO> datas) {
        Map<String, ApplicationAdditionalDataDTO> mapping = Maps.newHashMap();
        for (ApplicationAdditionalDataDTO data : datas) {
            mapping.put(data.getOid(), data);
        }
        return mapping;
    }

    void tallenna(final Excel excel) throws IOException {
        IOUtils.copy(excel.vieXlsx(), new FileOutputStream("pistesyotto.xlsx"));
    }

    public static class Result<T> {
        private T result;

        public Result(T result) {
            this.result = result;
        }

        public T getResult() {
            return result;
        }
    }

    public static void mockSuoritusrekisteri(final Semaphore suoritusCounter, final Semaphore arvosanaCounter) {
        MockServer fakeSure = new MockServer();
        mockForward(POST,
                fakeSure.addHandler("/suoritusrekisteri/rest/v1/suoritukset", exchange -> {
                    try {
                        Suoritus suoritus = new Gson().fromJson(
                                IOUtils.toString(exchange.getRequestBody()), new TypeToken<Suoritus>() {
                                }.getType()
                        );
                        suoritus.setId("suoritus" + suoritus.getHenkiloOid());
                        exchange.sendResponseHeaders(200, 0);
                        OutputStream responseBody = exchange.getResponseBody();
                        IOUtils.write(new Gson().toJson(suoritus), responseBody);
                        responseBody.close();
                        suoritusCounter.release();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }).addHandler("/suoritusrekisteri/rest/v1/arvosanat", exchange -> {
                    try {
                        Arvosana arvosana = new Gson().fromJson(
                                IOUtils.toString(exchange.getRequestBody()), new TypeToken<Arvosana>() {
                                }.getType()
                        );
                        exchange.sendResponseHeaders(200, 0);
                        OutputStream responseBody = exchange.getResponseBody();
                        IOUtils.write(new Gson().toJson(arvosana), responseBody);
                        responseBody.close();
                        arvosanaCounter.release();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }));
    }
}
