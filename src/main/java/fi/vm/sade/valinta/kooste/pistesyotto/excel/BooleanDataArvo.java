package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class BooleanDataArvo extends TilaDataArvo {
    private final Map<String, String> konvertteri;
    private final String tunniste;
    private final String osallistuminenTunniste;

    public BooleanDataArvo(Map<String, String> konvertteri, Map<String, String> tilaKonvertteri, String tunniste,
                           String asetettuTila, String osallistuminenTunniste) {
        super(tilaKonvertteri, asetettuTila);
        this.konvertteri = konvertteri;
        this.tunniste = tunniste;
        this.osallistuminenTunniste = osallistuminenTunniste;
    }

    protected boolean isValidi(String arvo) {
        return StringUtils.isBlank(arvo) || konvertteri.containsKey(arvo);
    }

    protected String konvertoi(String arvo) {
        if (konvertteri.containsKey(arvo)) {
            return konvertteri.get(arvo);
        } else {
            return null;
        }
    }

    private boolean isAsetettu(String arvo) {
        return StringUtils.isNotBlank(arvo) && konvertteri.containsKey(arvo) && !PistesyottoExcel.TYHJA.equals(arvo);
    }

    public PistesyottoArvo asPistesyottoArvo(String arvo, String tila) {
        String lopullinenTila;
        if (isAsetettu(arvo)) {
            lopullinenTila = getAsetettuTila();
        } else {
            lopullinenTila = konvertoiTila(tila);
            if (getAsetettuTila().equals(lopullinenTila)) {
                if (!(lopullinenTila.equals(PistesyottoExcel.VAKIO_OSALLISTUI) && !isAsetettu(arvo))) {
                    lopullinenTila = PistesyottoExcel.VAKIO_MERKITSEMATTA;
                }
            }
        }
        return new PistesyottoArvo(konvertoi(arvo), lopullinenTila, isValidi(arvo) && isValidiTila(tila), tunniste, osallistuminenTunniste);
    }
}
