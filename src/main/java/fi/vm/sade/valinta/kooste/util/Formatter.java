package fi.vm.sade.valinta.kooste.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;

public class Formatter {

    public static final NumberFormat NUMERO_FORMAATTI = NumberFormat.getInstance(new Locale("FI"));
    public static final String ARVO_EROTIN = " / ";
    public static final String ARVO_VALI = " ";
    public static final String ARVO_VAKIO = "-";

    public static String suomennaNumero(BigDecimal arvo) {
        return suomennaNumero(arvo, StringUtils.EMPTY);
    }

    public static String suomennaNumero(BigDecimal arvo, String vakioArvo) {
        if (arvo == null) {
            return vakioArvo;
        }
        return NUMERO_FORMAATTI.format(arvo);
    }

    public static String suomennaNumero(Integer arvo, String vakioArvo) {
        if (arvo == null) {
            return vakioArvo;
        }
        return NUMERO_FORMAATTI.format(arvo);
    }

    public static String suomennaNumero(Integer arvo) {
        return suomennaNumero(arvo, StringUtils.EMPTY);
    }
}
