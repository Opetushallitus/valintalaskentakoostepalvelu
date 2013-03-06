package fi.vm.sade.valinta.kooste.paasykokeet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.language.Simple;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.hakemus.schema.HakukohdeTyyppi;
import fi.vm.sade.service.valintaperusteet.messages.PaasykoeHakukohdeTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("luoDokumenttiKomponentti")
public class LuoDokumenttiKomponentti {

    // suodattaa pääsykokeettomat hakukohteet oletuksena. käytä spring
    // propertyplaceholderia sovelluskontekstissa ylikirjoittamaan tämän arvon
    // tarvittaessa.
    @Value("${valintalaskenta.kooste.dokumentti.suodataPaasykokeettomatHakukohteet:true}")
    private Boolean suodataPaasykokeettomatHakukohteet;

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
    public byte[] suoritaLaskenta(@Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset,
            @Simple("${property.paasykokeet}") List<PaasykoeHakukohdeTyyppi> paasykokeet) throws IOException {
        Workbook wb = new XSSFWorkbook();

        CreationHelper createHelper = wb.getCreationHelper();
        Sheet sheet = wb.createSheet("Hakukokeelliset hakukohteet");

        Map<String, List<String>> hakukohdeHakemukset = createHakemustenHakukohteet(suodataPaasykokeettomatHakukohteet,
                hakemukset, createHakukohdeTunnisteet(paasykokeet));

        int rivi = 0;
        for (Entry<String, List<String>> hk : hakukohdeHakemukset.entrySet()) {
            Row row = sheet.createRow(rivi);
            Cell cell = row.createCell(0);
            RichTextString richText = createHelper.createRichTextString(hk.getKey());
            cell.setCellValue(richText);

            int indeksi = 1;
            for (String hakemus : hk.getValue()) {
                row.createCell(indeksi).setCellValue(createHelper.createRichTextString(hakemus));
                ++indeksi;
            }
            ++rivi;
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        wb.write(output);
        IOUtils.closeQuietly(output);
        return output.toByteArray();
    }

    private static Map<String, List<String>> createHakemustenHakukohteet(boolean filtteroiHakemuksettomat,
            List<HakemusTyyppi> hakemukset, Map<String, List<String>> hakukohdeTunnisteet) {
        Map<String, List<String>> hakukohdeHakemukset = new HashMap<String, List<String>>();
        for (HakemusTyyppi hakemus : hakemukset) {
            for (HakukohdeTyyppi hakukohde : hakemus.getHakukohde()) {
                if (hakukohdeTunnisteet.containsKey(hakukohde.getHakukohdeOid())) {
                    String hakukohdeoid = hakukohde.getHakukohdeOid();
                    List<String> hakemuksetHakukohteelle = null;
                    if (hakukohdeHakemukset.containsKey(hakukohdeoid)) {
                        hakemuksetHakukohteelle = hakukohdeHakemukset.get(hakukohdeoid);
                    } else {
                        hakemuksetHakukohteelle = new ArrayList<String>();
                    }
                    hakemuksetHakukohteelle.add(hakemus.getHakemusOid());
                    hakukohdeHakemukset.put(hakukohde.getHakukohdeOid(), hakemuksetHakukohteelle);
                }
            }
        }
        return hakukohdeHakemukset;
    }

    private static Map<String, List<String>> createHakukohdeTunnisteet(List<PaasykoeHakukohdeTyyppi> paasykokeet) {
        Map<String, List<String>> hakukohdeTunniste = new HashMap<String, List<String>>();
        for (PaasykoeHakukohdeTyyppi paasykoe : paasykokeet) {
            hakukohdeTunniste.put(paasykoe.getHakukohdeOid(), paasykoe.getTunniste());
        }
        return hakukohdeTunniste;
    }
}
