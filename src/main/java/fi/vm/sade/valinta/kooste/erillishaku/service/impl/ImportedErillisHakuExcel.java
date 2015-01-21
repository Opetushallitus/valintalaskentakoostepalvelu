package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import fi.vm.sade.valinta.kooste.exception.ErillishaunDataException;
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
    public final List<ErillishakuRivi> rivit;
    public final Map<String, ErillishakuRivi> hetuToRivi;
    private final boolean kasitteleVirheetDatassaVaroituksinaPoikkeuksenSijaan; // default false, poikkeus lentaa jos virheita. Viennissa voisi olla pois päältä harkinnan mukaan.

    public ImportedErillisHakuExcel(Hakutyyppi hakutyyppi, InputStream inputStream, boolean kasitteleVirheetDatassaVaroituksinaPoikkeuksenSijaan) {
        this.kasitteleVirheetDatassaVaroituksinaPoikkeuksenSijaan = kasitteleVirheetDatassaVaroituksinaPoikkeuksenSijaan;
        hetuToRivi = Maps.newHashMap();
        henkiloPrototyypit = Lists.newArrayList();
        try {
            rivit = createExcel(hakutyyppi, inputStream);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    public ImportedErillisHakuExcel(Hakutyyppi hakutyyppi, InputStream inputStream) {
        this(hakutyyppi,inputStream, true);
    }
    public ImportedErillisHakuExcel(Hakutyyppi hakutyyppi, List<ErillishakuRivi> erillishakuRivi, boolean kasitteleVirheetDatassaVaroituksinaPoikkeuksenSijaan) {
        this.kasitteleVirheetDatassaVaroituksinaPoikkeuksenSijaan = kasitteleVirheetDatassaVaroituksinaPoikkeuksenSijaan;
        LOG.info("Muodostetaan erillishaunriveistä ({}kpl) henkilönluotioliot", erillishakuRivi.size());
        henkiloPrototyypit = erillishakuRivi.stream().map(rivi -> convert(rivi)).collect(Collectors.toList());
        hetuToRivi = erillishakuRivi.stream().collect(Collectors.
                toMap(rivi -> Optional.ofNullable(StringUtils.trimToNull(rivi.getHenkilotunnus())).orElse(rivi.getSyntymaAika()), rivi -> rivi));
        this.rivit = erillishakuRivi;

    }
    public ImportedErillisHakuExcel(Hakutyyppi hakutyyppi, List<ErillishakuRivi> erillishakuRivi) {
        this(hakutyyppi,erillishakuRivi, true);
    }

    private List<ErillishakuRivi> createExcel(Hakutyyppi hakutyyppi, InputStream inputStream) throws IOException {
        final List<ErillishakuRivi> rivit = Lists.newArrayList();
        new ErillishakuExcel(hakutyyppi, rivi -> {
            rivit.add(rivi);
        }).getExcel().tuoXlsx(inputStream);
        try {
            rivit.forEach(rivi -> {
                hetuToRivi.put(Optional.ofNullable(StringUtils.trimToNull(rivi.getHenkilotunnus())).orElse(rivi.getSyntymaAika()), rivi);
                henkiloPrototyypit.add(convert(rivi));
            });
        } catch (Exception e) {
            LOG.error("Excelin muodostus epaonnistui! {}", e);
            throw e;
        }
        return rivit;
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
