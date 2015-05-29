package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import static fi.vm.sade.valinta.kooste.sijoittelu.komponentti.ModuloiPaivamaaraJaTunnit.moduloiSeuraava;
import static fi.vm.sade.valinta.kooste.sijoittelu.komponentti.ModuloiPaivamaaraJaTunnit.seuraavaAskel;
import static org.junit.Assert.assertEquals;

import java.text.ParseException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.util.Formatter;

public class ModuloiPaivamaaraJaTunnitTest {
    private static final Logger LOG = LoggerFactory.getLogger(ModuloiPaivamaaraJaTunnitTest.class);

    @Test
    public void testaaTuntienModulointi() {
        assertEquals(14, seuraavaAskel(14, 14, 1));
        assertEquals(13, seuraavaAskel(14, 13, 1));
        assertEquals(15, seuraavaAskel(14, 15, 1));
        assertEquals(14, seuraavaAskel(14, 14, 2));
        assertEquals(16, seuraavaAskel(14, 15, 2));
        assertEquals(16, seuraavaAskel(14, 16, 2));
        assertEquals(14, seuraavaAskel(14, 14, 6));
        assertEquals(20, seuraavaAskel(14, 15, 6));
        assertEquals(20, seuraavaAskel(14, 20, 6));
        assertEquals(26, seuraavaAskel(14, 21, 6));
        assertEquals(8, seuraavaAskel(14, 7, 6));
        assertEquals(14, seuraavaAskel(14, 9, 6));
        assertEquals(8, seuraavaAskel(14, 7, 6));
        assertEquals(8, seuraavaAskel(14, 7, 2));
    }

    @Test
    public void testaaModulointiaVahaEnnen() throws ParseException {
        {
            DateTime start = dateTime("02.10.2014 14:05");
            DateTime now = dateTime("05.10.2015 14:03");

            assertEquals("05.10.2015 14:05", Formatter.paivamaara(moduloiSeuraava(start, now, 1).toDate()));
            assertEquals("05.10.2015 14:05", Formatter.paivamaara(moduloiSeuraava(start, now, 2).toDate()));
        }
    }

    @Test
    public void pelkkaSuurempiMinuuttiEiAiheutaLisaintervallia() throws ParseException {
        DateTime start = dateTime("02.10.2014 14:00");
        DateTime now = dateTime("02.10.2015 15:01");
        assertEquals("02.10.2015 16:00", Formatter.paivamaara(moduloiSeuraava(start, now, 2).toDate()));
    }

    @Test
    public void testaaModulointiaVahanJalkeen() throws ParseException {
        {
            DateTime start = dateTime("02.10.2014 14:05");
            DateTime now = dateTime("05.10.2015 14:07");

            assertEquals("05.10.2015 15:05", Formatter.paivamaara(moduloiSeuraava(start, now, 1).toDate()));
            assertEquals("05.10.2015 16:05", Formatter.paivamaara(moduloiSeuraava(start, now, 2).toDate()));
            assertEquals("05.10.2015 20:05", Formatter.paivamaara(moduloiSeuraava(start, now, 6).toDate()));
        }
    }

    @Test
    public void testaaModulointiaPaljonJalkeen() throws ParseException {
        {
            DateTime start = dateTime("02.10.2014 14:05");
            DateTime now = dateTime("05.10.2015 18:03");

            assertEquals("05.10.2015 18:05", Formatter.paivamaara(moduloiSeuraava(start, now, 1).toDate()));
            assertEquals("05.10.2015 18:05", Formatter.paivamaara(moduloiSeuraava(start, now, 2).toDate()));
            assertEquals("05.10.2015 20:05", Formatter.paivamaara(moduloiSeuraava(start, now, 6).toDate()));
        }
    }

    @Test
    public void testaaModulointiaYlipaivan() throws ParseException {
        {
            DateTime start = dateTime("02.10.2014 14:05");
            DateTime now = dateTime("05.10.2015 22:03");

            assertEquals("06.10.2015 02:05", Formatter.paivamaara(moduloiSeuraava(start, now, 6).toDate()));
        }
    }

    private static DateTime dateTime(final String time) {
        return DateTimeFormat.forPattern("dd.MM.yyyy HH:mm").parseDateTime(time);
    }
}
