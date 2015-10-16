package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuaikaV1RDTO;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

public class SuoritusrekisteriUtil {

    public static final String ENSIKERTALAISUUS_RAJAPVM_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

    public static String getEnsikertalaisuudenRajapvm(final HakuV1RDTO haku) {
        if (haku == null || haku.getHakuaikas() == null) {
            return null;
        }

        return haku.getHakuaikas().stream()
                .map(HakuaikaV1RDTO::getLoppuPvm)
                .sorted(Comparator.nullsFirst(Comparator.<Date>reverseOrder()))
                .findFirst()
                .map(date -> new SimpleDateFormat(ENSIKERTALAISUUS_RAJAPVM_FORMAT).format(date))
                .orElse(null);
    }

}
