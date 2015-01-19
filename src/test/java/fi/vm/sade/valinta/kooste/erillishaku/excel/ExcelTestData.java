package fi.vm.sade.valinta.kooste.erillishaku.excel;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;

public class ExcelTestData {
    public static InputStream erillisHakuHetullaJaSyntymaAjalla() {
        return getInputStream("kustom_erillishaku.xlsx");
    }

    public static InputStream erillisHakuHenkiloOidilla() {
        return getInputStream("erillishaku_oidilla.xlsx");
    }

    public static InputStream erillisHakuSyntymaAjalla() {
        return getInputStream("erillishaku_syntymaajalla.xlsx");
    }

    private static InputStream getInputStream(final String filename) {
        try {
            return new ClassPathResource(filename).getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
