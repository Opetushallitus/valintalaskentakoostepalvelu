package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuExcel;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ImportedErillisHakuExcel {
    private static final Logger LOG = LoggerFactory.getLogger(ImportedErillisHakuExcel.class);
    private final static org.joda.time.format.DateTimeFormatter dtf = DateTimeFormat.forPattern("dd.MM.yyyy");
    public final List<HenkiloCreateDTO> henkiloPrototyypit;
    public final List<ErillishakuRivi> rivit;
    public final Map<String, ErillishakuRivi> hetuToRivi;

    public ImportedErillisHakuExcel(Hakutyyppi hakutyyppi, List<ErillishakuRivi> erillishakuRivi) {
        hetuToRivi = Maps.newHashMap();
        henkiloPrototyypit = Lists.newArrayList();
        this.rivit = erillishakuRivi;
        try {
            rivit.forEach(rivi -> {
                hetuToRivi.put(Optional.ofNullable(StringUtils.trimToNull(rivi.getHenkilotunnus())).orElse(rivi.getSyntymaAika()), rivi);
                henkiloPrototyypit.add(convert(rivi));
            });
        } catch (Exception e) {
            LOG.error("Erillishaunrivien muodostus epaonnistui! {}", e);
            throw e;
        }

    }
    public ImportedErillisHakuExcel(Hakutyyppi hakutyyppi, InputStream inputStream) {
        this(hakutyyppi,createExcel(hakutyyppi, inputStream));
    }

    private static List<ErillishakuRivi> createExcel(Hakutyyppi hakutyyppi, InputStream inputStream) {
        try {
            final List<ErillishakuRivi> rivit = Lists.newArrayList();
            new ErillishakuExcel(hakutyyppi, rivi -> rivit.add(rivi)).getExcel().tuoXlsx(inputStream);
            return rivit;
        } catch(Throwable t) {
            LOG.error("Excelin muodostus epaonnistui! {}", t);
            throw new RuntimeException(t);
        }
    }

    private HenkiloCreateDTO convert(final ErillishakuRivi rivi) {
        return new HenkiloCreateDTO(
                rivi.getAidinkieli(),
                rivi.getSyntymaAika(),
                rivi.getEtunimi(), rivi.getSukunimi(), rivi.getHenkilotunnus(), parseSyntymaAika(rivi), rivi.getPersonOid(), HenkiloTyyppi.OPPIJA);
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
