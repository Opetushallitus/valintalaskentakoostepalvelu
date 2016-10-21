package fi.vm.sade.valinta.kooste.hakemukset.service;

import static fi.vm.sade.valinta.kooste.hakemukset.service.SureenMigroitavatAmmatillisenKielikoeSuoritukset.create;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKUKOHDE1;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.osallistuminen;

import fi.vm.sade.valinta.kooste.hakemukset.service.SureenMigroitavatAmmatillisenKielikoeSuoritukset.YhdenHakukohteenTallennettavatTiedot;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SureenMigroitavatAmmatillisenKielikoeSuorituksetTest {
    private String fiTrueHakemus = "1.2.246.562.11.00005402933";
    private String fiFalseHakemus = "1.2.246.562.11.00004913821";

    private String svTrueHakemus = "1.2.246.562.11.00001928592";
    private String svFalseHakemus = "1.2.246.562.11.00001988118";

    @Test
    public void getParsed() throws Exception {
        List<ValintakoeOsallistuminenDTO> osallistumistiedot = Arrays.asList(
            createOsallistuminen(fiTrueHakemus, "kielikoe_fi"),
            createOsallistuminen(fiFalseHakemus, "kielikoe_fi"),
            createOsallistuminen(svTrueHakemus, "kielikoe_sv"),
            createOsallistuminen(svFalseHakemus, "kielikoe_sv")
        );

        SureenMigroitavatAmmatillisenKielikoeSuoritukset suoritukset = create(osallistumistiedot);

        YhdenHakukohteenTallennettavatTiedot hakukohteenTallennettavatTiedot = suoritukset.tallennettavatTiedotHakukohdeOidinMukaan.get(HAKUKOHDE1);

        Assert.assertEquals("kielikoe_fi", hakukohteenTallennettavatTiedot.kielikoeTuloksetHakemuksittain.get(fiTrueHakemus).get(0).kokeenTunnus);
        Assert.assertEquals("true", hakukohteenTallennettavatTiedot.kielikoeTuloksetHakemuksittain.get(fiTrueHakemus).get(0).arvioArvosana);

        Assert.assertEquals("kielikoe_fi", hakukohteenTallennettavatTiedot.kielikoeTuloksetHakemuksittain.get(fiFalseHakemus).get(0).kokeenTunnus);
        Assert.assertEquals("false", hakukohteenTallennettavatTiedot.kielikoeTuloksetHakemuksittain.get(fiFalseHakemus).get(0).arvioArvosana);

        Assert.assertEquals("kielikoe_sv", hakukohteenTallennettavatTiedot.kielikoeTuloksetHakemuksittain.get(svTrueHakemus).get(0).kokeenTunnus);
        Assert.assertEquals("true", hakukohteenTallennettavatTiedot.kielikoeTuloksetHakemuksittain.get(svTrueHakemus).get(0).arvioArvosana);

        Assert.assertEquals("kielikoe_sv", hakukohteenTallennettavatTiedot.kielikoeTuloksetHakemuksittain.get(svFalseHakemus).get(0).kokeenTunnus);
        Assert.assertEquals("false", hakukohteenTallennettavatTiedot.kielikoeTuloksetHakemuksittain.get(svFalseHakemus).get(0).arvioArvosana);
    }

    private ValintakoeOsallistuminenDTO createOsallistuminen(String hakemusOid, String tunniste) {
        return osallistuminen().setHakuOid("1.2.246.562.29.90697286251").setHakemusOid(hakemusOid)
                .hakutoive().setHakukohdeOid(HAKUKOHDE1)
                    .valinnanvaihe().
                        valintakoe().setTunniste(tunniste).setOsallistuu().build()
                    .build()
                .build()
            .build();
    }

}
