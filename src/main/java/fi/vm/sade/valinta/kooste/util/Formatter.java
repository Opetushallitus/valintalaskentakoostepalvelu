package fi.vm.sade.valinta.kooste.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;

public class Formatter {

    public static final NumberFormat NUMERO_FORMAATTI = NumberFormat.getInstance(new Locale("FI"));

    public static String suomennaNumero(BigDecimal arvo) {
        if (arvo == null) {
            return StringUtils.EMPTY;
        }
        return NUMERO_FORMAATTI.format(arvo);
    }
}
