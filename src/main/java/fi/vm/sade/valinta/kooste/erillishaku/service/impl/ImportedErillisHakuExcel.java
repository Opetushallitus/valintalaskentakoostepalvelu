package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuExcel;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;

public class ImportedErillisHakuExcel {
    private static final Logger LOG = LoggerFactory.getLogger(ImportedErillisHakuExcel.class);

    public final List<ErillishakuRivi> rivit;

    public ImportedErillisHakuExcel(Hakutyyppi hakutyyppi, InputStream inputStream) {
        rivit = new ArrayList<>();
        try {
            new ErillishakuExcel(hakutyyppi, rivi -> {
                if (rivi.getHenkilotunnus() == null || rivi.getSyntymaAika() == null) {
                    LOG.warn("Käyttökelvoton rivi {}", rivi);
                    return;
                }
                rivit.add(rivi);
            }).getExcel().tuoXlsx(inputStream);
        } catch (Exception e) {
            LOG.error("Excelin muodostus epaonnistui! {}", e);
            throw new RuntimeException(e);
        }
    }

    public ImportedErillisHakuExcel(Hakutyyppi hakutyyppi, List<ErillishakuRivi> erillishakuRivi) {
        rivit = erillishakuRivi;
    }
}
