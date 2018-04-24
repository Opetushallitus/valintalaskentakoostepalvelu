package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloTyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuExcel;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.excel.ExcelValidointiPoikkeus;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloCreateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class ImportedErillisHakuExcel {
    private static final Logger LOG = LoggerFactory.getLogger(ImportedErillisHakuExcel.class);
    private final List<HenkiloCreateDTO> henkiloPrototyypit;
    public final List<ErillishakuRivi> rivit;

    ImportedErillisHakuExcel(List<ErillishakuRivi> erillishakuRivi) {
        henkiloPrototyypit = Lists.newArrayList();
        this.rivit = erillishakuRivi;
        try {
            rivit.forEach(rivi -> henkiloPrototyypit.add(convert(rivi)));
        } catch (Exception e) {
            LOG.error("Erillishaunrivien muodostus epaonnistui!", e);
            throw e;
        }

    }

    public ImportedErillisHakuExcel(Hakutyyppi hakutyyppi, InputStream inputStream, KoodistoCachedAsyncResource koodistoCachedAsyncResource) {
        this(createExcel(hakutyyppi, inputStream, koodistoCachedAsyncResource));
    }

    private static List<ErillishakuRivi> createExcel(Hakutyyppi hakutyyppi, InputStream inputStream, KoodistoCachedAsyncResource koodistoCachedAsyncResource) throws ExcelValidointiPoikkeus {
        try {
            final List<ErillishakuRivi> rivit = Lists.newArrayList();
            new ErillishakuExcel(hakutyyppi, rivi -> rivit.add(rivi.withAidinkieli(resolveAidinkieli(rivi.getAidinkieli()))), koodistoCachedAsyncResource)
                    .getExcel()
                    .tuoXlsx(inputStream);
            return rivit;
        } catch (ExcelValidointiPoikkeus e) {
            throw e;
        } catch (Throwable t) {
            LOG.error("Excelin muodostus epaonnistui!", t);
            throw new RuntimeException(t);
        }
    }

    private HenkiloCreateDTO convert(final ErillishakuRivi rivi) {
        return new HenkiloCreateDTO(
                Optional.ofNullable(rivi.getAidinkieli()).map(ImportedErillisHakuExcel::resolveAidinkieli).orElse("fi"),
                rivi.getSukupuoli().name(),
                rivi.getEtunimi(),
                rivi.getSukunimi(),
                rivi.getHenkilotunnus(),
                rivi.getSyntymaAika(),
                rivi.getPersonOid(),
                HenkiloTyyppi.OPPIJA,
                rivi.getAsiointikieli(),
                rivi.getKansalaisuus()
        );
    }

    private static String resolveAidinkieli(String aidinkieli) {
        return isFloatingPointNumber(aidinkieli) ? aidinkieli.substring(0, aidinkieli.indexOf(".")) : aidinkieli;
    }

    private static boolean isFloatingPointNumber(String str) {
        return str.matches("\\d+\\.\\d+");
    }
}
