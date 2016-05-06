package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel.VAKIO_EI_OSALLISTUNUT;
import static fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel.VAKIO_EI_VAADITA;
import static fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel.VAKIO_MERKITSEMATTA;
import static fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel.VAKIO_OSALLISTUI;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;

public class PistesyottoRivi {
    private final String oid;
    private final String nimi;
    private final String hetu;
    private final String pvm;
    private final Collection<PistesyottoArvo> arvot;

    public PistesyottoRivi(String oid, String nimi, String hetu, String pvm, Collection<PistesyottoArvo> arvot) {
        this.oid = oid;
        this.nimi = nimi;
        this.hetu = hetu;
        this.pvm = pvm;
        this.arvot = arvot;
    }

    public Map<String, String> asAdditionalData(Predicate<String> valintakoetunnisteFiltteri) {
        Map<String, String> data = Maps.newHashMap();
        for (PistesyottoArvo pisteSyottoArvo : arvot) {
            if (!isBlank(pisteSyottoArvo.getTila())) {
                if(valintakoetunnisteFiltteri.test(pisteSyottoArvo.getTunniste())) {
                    String arvo = pisteSyottoArvo.getArvo();
                    String tila = pisteSyottoArvo.getTila();

                    if (isBlank(arvo) && tila.equals(VAKIO_OSALLISTUI)) {
                        tila = VAKIO_MERKITSEMATTA;
                    }

                    if (Arrays.asList(VAKIO_EI_OSALLISTUNUT, VAKIO_MERKITSEMATTA).contains(tila)) {
                        arvo = "";
                    }

                    data.put(pisteSyottoArvo.getTunniste(), arvo);
                    data.put(pisteSyottoArvo.getOsallistuminenTunniste(), tila);
                }
            }
        }
        return data;
    }

    public boolean isValidi() {
        for (PistesyottoArvo a : arvot) {
            if (!a.isValidi()) {
                return false;
            }
        }
        return true;
    }

    public Collection<PistesyottoArvo> getArvot() {
        return arvot;
    }

    public String getNimi() {
        return nimi;
    }

    public String getOid() {
        return oid;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[oid=").append(oid).append(",nimi=").append(nimi).append(",hetu=").append(hetu)
                .append(",pvm=").append(pvm).append(",");
        for (PistesyottoArvo arvo : arvot) {
            sb.append("\r\n\t[tunniste=").append(arvo.getTunniste()).append(",arvo=").append(arvo.getArvo())
                    .append(",tila=").append(arvo.getTila())
                    .append(",osallistumisenTunniste=").append(arvo.getOsallistuminenTunniste()).append("]");
        }
        sb.append("]");
        return sb.toString();
    }
}
