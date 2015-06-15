package fi.vm.sade.valinta.kooste.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multiset;

public class TodellisenJonosijanLaskentaUtiliteetti {
    private static final Logger LOG = LoggerFactory.getLogger(TodellisenJonosijanLaskentaUtiliteetti.class);

    public static int laskeTodellinenJonosija(int jonosija, Multiset<Integer> jonosijat) {
        int todellinenKkJonosija = 1;
        for (com.google.common.collect.Multiset.Entry<Integer> entry : jonosijat.entrySet()) {
            if (entry.getElement().equals(jonosija)) {
                System.err.println("Palautetaan " + todellinenKkJonosija);
                return todellinenKkJonosija;
            }
            todellinenKkJonosija += entry.getCount();
        }
        LOG.error("Jonosijaa {} ei ollut hakutoiveen jonosijoissa!", jonosija);
        throw new RuntimeException("Jonosijaa " + jonosija + " ei ollut hakutoiveen jonosijoissa!");
    }
}
