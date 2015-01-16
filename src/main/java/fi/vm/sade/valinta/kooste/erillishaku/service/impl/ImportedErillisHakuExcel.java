package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuExcel;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;

public class ImportedErillisHakuExcel {
    private static final Logger LOG = LoggerFactory
        .getLogger(ImportedErillisHakuExcel.class);
    private final static org.joda.time.format.DateTimeFormatter dtf = DateTimeFormat.forPattern("dd.MM.yyyy");
    public final List<HenkiloCreateDTO> henkiloPrototyypit;
    public final Map<String, ErillishakuRivi> hetuToRivi;

    public ImportedErillisHakuExcel(Hakutyyppi hakutyyppi, InputStream inputStream) throws IOException {
        hetuToRivi = Maps.newHashMap();
        henkiloPrototyypit = Lists.newArrayList();
        createExcel(hakutyyppi).getExcel().tuoXlsx(inputStream);
    }

    public ImportedErillisHakuExcel(Hakutyyppi hakutyyppi, List<ErillishakuRivi> erillishakuRivi) throws IOException {
        henkiloPrototyypit = erillishakuRivi.stream().map(rivi -> convert(rivi)).collect(Collectors.toList());
        hetuToRivi = erillishakuRivi.stream().collect(Collectors.
                toMap(rivi -> Optional.ofNullable(StringUtils.trimToNull(rivi.getHenkilotunnus())).orElse(rivi.getSyntymaAika()), rivi -> rivi));

    }

    private ErillishakuExcel createExcel(Hakutyyppi hakutyyppi) {
        try {
            return new ErillishakuExcel(hakutyyppi, rivi -> {
                if (rivi.getHenkilotunnus() == null || rivi.getSyntymaAika() == null) {
                    LOG.warn("Käyttökelvoton rivi {}", rivi);
                    return;
                }
                hetuToRivi.put(Optional.ofNullable(StringUtils.trimToNull(rivi.getHenkilotunnus())).orElse(rivi.getSyntymaAika()), rivi);
                henkiloPrototyypit.add(convert(rivi));
            });
        } catch (Exception e) {
            LOG.error("Excelin muodostus epaonnistui! {}", e);
            throw e;
        }
    }

    private HenkiloCreateDTO convert(final ErillishakuRivi rivi) {
        return new HenkiloCreateDTO(rivi.getEtunimi(), rivi.getSukunimi(), rivi.getHenkilotunnus(), parseSyntymaAika(rivi), rivi.getPersonOid(), HenkiloTyyppi.OPPIJA);
    }

    private static Date parseSyntymaAika(ErillishakuRivi rivi) {
        try {
            return dtf.parseDateTime(rivi.getSyntymaAika()).toDate();
        } catch (Exception e) {
            LOG.error("Syntymäaikaa {} ei voitu parsia muodossa dd.MM.yyyy", rivi.getSyntymaAika());
            return null;
        }
    }
}
