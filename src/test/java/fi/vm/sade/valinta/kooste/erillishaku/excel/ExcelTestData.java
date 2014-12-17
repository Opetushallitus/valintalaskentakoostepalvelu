package fi.vm.sade.valinta.kooste.erillishaku.excel;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;

public class ExcelTestData {
    public static InputStream exampleExcelData() {
        try {
            return new ClassPathResource("kustom_erillishaku.xlsx").getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
