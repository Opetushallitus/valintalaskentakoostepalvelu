package fi.vm.sade.valinta.kooste.hakutoiveet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.camel.language.Simple;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakukohdeTyyppi;
import fi.vm.sade.service.hakemus.schema.HakutoiveTyyppi;
import fi.vm.sade.service.valintaperusteet.messages.PaasykoeHakukohdeTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("luoDokumenttiKomponentti")
public class LuoDokumenttiKomponentti {

    /**
     * Ketkä menee tekemään hakukoetta kyseessä olevaan hakukohteeseen
     * 
     * @param hakutoiveet
     * @param paasykokeet
     * @return
     * @throws IOException
     *             Annetaan Camelin hoitaa poikkeukset niin reittiin voidaan
     *             tehdä poikkeuskäsittelylogiikka tarpeeseen.
     */
    public byte[] suoritaLaskenta(@Simple("${property.hakutoiveet}") List<HakutoiveTyyppi> hakutoiveet,
            @Simple("${property.paasykokeet}") List<PaasykoeHakukohdeTyyppi> paasykokeet) throws IOException {
        Workbook wb = new XSSFWorkbook();

        CreationHelper createHelper = wb.getCreationHelper();
        Sheet sheet = wb.createSheet("Hakukokeelliset hakukohteet");

        for (HakutoiveTyyppi hakutoive : hakutoiveet) {
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue(createHelper.createRichTextString(hakutoive.getHakemusOid()));

            for (HakukohdeTyyppi hakukohde : hakutoive.getHakutoive()) {
                for (PaasykoeHakukohdeTyyppi paasykoe : paasykokeet) {
                    String hakukohdeoid = paasykoe.getHakukohdeOid();

                }
            }
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            wb.write(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        IOUtils.closeQuietly(output);
        return output.toByteArray();
    }

}
