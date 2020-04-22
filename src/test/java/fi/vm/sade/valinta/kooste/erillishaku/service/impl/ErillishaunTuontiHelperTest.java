package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import static org.junit.Assert.assertEquals;

import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class ErillishaunTuontiHelperTest {

    private static final String PERSON_1_ETUNIMI = "Etunimi 1";
    private static final String PERSON_2_ETUNIMI = "Etunimi 2";
    private static final String PERSON_1_HETU = "010203-123X";
    private static final String PERSON_1_OID = "1.2.246.562.24.64735725450";
    private static final String PERSON_2_OID = "1.2.246.562.24.64735725451";
    private static final String PERSON_1_SUKUNIMI = "Sukunimi 1";
    private static final String PERSON_2_SUKUNIMI = "Sukunimi 2";
    private static final String PERSON_1_SYNTYMA_AIKA = "2000-01-01";
    private static final String PERSON_2_SYNTYMA_AIKA = "2000-01-02";
    private static final String PERSON_1_SYNTYMA_AIKA_SUOMALAINEN_MUOTO = "01.01.2000";
    private static final String PERSON_2_SYNTYMA_AIKA_SUOMALAINEN_MUOTO = "02.01.2000";

    private static final ErillishakuRivi ERILLISHAKU_RIVI_1 = new ErillishakuRivi(null, PERSON_1_SUKUNIMI, PERSON_1_ETUNIMI, null,
            null, PERSON_1_SYNTYMA_AIKA_SUOMALAINEN_MUOTO, Sukupuoli.MIES, PERSON_1_OID, null, null,
            false, null, null, null,
            null, null, null, null,
            null, null, null, false, false, null, null,
            null, null, null, null, null, null, null, null,
            null, null);
    private static final ErillishakuRivi ERILLISHAKU_RIVI_2 = new ErillishakuRivi(null, PERSON_2_SUKUNIMI, PERSON_2_ETUNIMI, null,
            null, PERSON_2_SYNTYMA_AIKA_SUOMALAINEN_MUOTO, Sukupuoli.NAINEN, PERSON_2_OID, null, null,
            false, null, null, null,
            null, null, null, null,
            null, null, null, false, false, null, null,
            null, null, null, null, null, null, null, null,
            null, null);

    /* Erillinen erillishakurivi jossa hetu mukana, koska hetusta päätellään sukupuoli joka yliajaa parametrina annetun sukupuolen */
    private static final ErillishakuRivi ERILLISHAKU_RIVI_1_HETULLINEN = new ErillishakuRivi(null, PERSON_1_SUKUNIMI, PERSON_1_ETUNIMI,
            PERSON_1_HETU, null, PERSON_1_SYNTYMA_AIKA_SUOMALAINEN_MUOTO, null, PERSON_1_OID, null, null,
            false, null, null, null,
            null, null, null, null,
            null, null, null, false, false, null, null,
            null, null, null, null, null, null, null, null,
            null, null);

    private static final List<ErillishakuRivi> ERILLISHAKU_RIVIT = Arrays.asList(ERILLISHAKU_RIVI_1, ERILLISHAKU_RIVI_2, ERILLISHAKU_RIVI_1_HETULLINEN);

    private HenkiloPerustietoDto henkiloPerustietoDto;


    @Before
    public void setup() {
        henkiloPerustietoDto = new HenkiloPerustietoDto();
    }

    @Test
    public void testaaEtsiHenkiloaVastaavaRiviOidinMukaan() {
        henkiloPerustietoDto.setOidHenkilo(PERSON_1_OID);
        ErillishakuRivi loytynytRivi = ErillishaunTuontiHelper.etsiHenkiloaVastaavaRivi(henkiloPerustietoDto, ERILLISHAKU_RIVIT);

        assertEquals(PERSON_1_OID, loytynytRivi.getPersonOid());
    }

    @Test
    public void testaaEtsiHenkiloaVastaavaRiviHetunMukaan() {
        henkiloPerustietoDto.setHetu(PERSON_1_HETU);
        ErillishakuRivi loytynytRivi = ErillishaunTuontiHelper.etsiHenkiloaVastaavaRivi(henkiloPerustietoDto, ERILLISHAKU_RIVIT);

        assertEquals(PERSON_1_OID, loytynytRivi.getPersonOid());
    }

    @Test
    public void testaaEtsiHenkiloaVastaavaRiviSyntymaajanSukupuolenJaNimenMukaan() {
        henkiloPerustietoDto.setSyntymaaika(LocalDate.parse(PERSON_1_SYNTYMA_AIKA));
        henkiloPerustietoDto.setSukupuoli(Sukupuoli.MIES.toString());
        henkiloPerustietoDto.setEtunimet(PERSON_1_ETUNIMI);
        henkiloPerustietoDto.setSukunimi(PERSON_1_SUKUNIMI);
        ErillishakuRivi loytynytRivi = ErillishaunTuontiHelper.etsiHenkiloaVastaavaRivi(henkiloPerustietoDto, ERILLISHAKU_RIVIT);

        assertEquals(PERSON_1_OID, loytynytRivi.getPersonOid());
    }

    @Test(expected = ErillishaunTuontiHelper.HenkilonRivinPaattelyEpaonnistuiException.class)
    public void testaaEtsiHenkiloaVastaavaRiviJotaEiLoydy() {
        ErillishaunTuontiHelper.etsiHenkiloaVastaavaRivi(henkiloPerustietoDto, ERILLISHAKU_RIVIT);
    }
}
