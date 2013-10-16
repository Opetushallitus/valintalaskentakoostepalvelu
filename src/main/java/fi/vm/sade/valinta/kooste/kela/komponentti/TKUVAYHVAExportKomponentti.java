package fi.vm.sade.valinta.kooste.kela.komponentti;

import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYVAKSYTTY;
import static fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource.HENKILOTUNNUS;

import java.util.Date;

import org.apache.camel.Body;
import org.apache.camel.Property;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.organisaatio.api.model.types.OrganisaatioDTO;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.haku.HakemusProxy;
import fi.vm.sade.valinta.kooste.tarjonta.OrganisaatioProxy;

@Component("TKUVAYHVAKomponentti")
public class TKUVAYHVAExportKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(TKUVAYHVAExportKomponentti.class);
    private static final Integer KESAKUU = 6;

    @Autowired
    private HakemusProxy hakemusProxy;

    @Autowired
    private OrganisaatioProxy organisaatioProxy;

    public TKUVAYHVA luoTKUVAYHVA(@Body HakijaDTO hakija, // @Property("hakuOid")
                                                          // String hakuOid,
            @Property("lukuvuosi") Date lukuvuosi, @Property("poimintapaivamaara") Date poimintapaivamaara) {
        String linjakoodi = "000";
        for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
            for (HakutoiveenValintatapajonoDTO valintatapajono : hakutoive.getHakutoiveenValintatapajonot()) {
                if (HYVAKSYTTY.equals(valintatapajono.getTila())) {
                    TKUVAYHVA.Builder builder = new TKUVAYHVA.Builder();
                    // organisaatioService.findByOid(arg0)
                    try {
                        OrganisaatioDTO organisaatio = organisaatioProxy.haeOrganisaatio(hakutoive.getTarjoajaOid());
                        if (organisaatio == null) {
                            // new
                            // OrganisaatioException("Organisaatio ei palauttanut yhteishaun koulukoodia!");
                            LOG.error("Yhteishaun koulukoodia ei voitu hakea organisaatiolle {}",
                                    hakutoive.getTarjoajaOid());
                            builder.setOppilaitos("0000");
                        } else {
                            if (organisaatio.getYhteishaunKoulukoodi() == null) {
                                // new
                                // OrganisaatioException("Organisaatio ei palauttanut yhteishaun koulukoodia!");
                                LOG.error("Yhteishaun koulukoodia ei voitu hakea organisaatiolle {}",
                                        hakutoive.getTarjoajaOid());
                                builder.setOppilaitos("0000");
                            } else {
                                builder.setOppilaitos(organisaatio.getYhteishaunKoulukoodi());
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("Yhteishaun koulukoodia ei voitu hakea organisaatiolle {}",
                                hakutoive.getTarjoajaOid());
                        // OrganisaatioException("Organisaatio ei palauttanut yhteishaun koulukoodia!");
                        builder.setOppilaitos("0000");
                    }

                    builder.setLinjakoodi(linjakoodi);
                    builder.setValintapaivamaara(new Date()); // TODO:
                                                              // Sijoittelun
                                                              // täytyy
                                                              // osata
                                                              // kertoa
                                                              // tämä!
                    builder.setSukunimi(hakija.getSukunimi());
                    builder.setEtunimet(hakija.getEtunimi());

                    try {
                        Hakemus hakemus = hakemusProxy.haeHakemus(hakija.getHakemusOid());
                        String standardinMukainenHenkilotunnus = hakemus.getAnswers().getHenkilotiedot()
                                .get(HENKILOTUNNUS);
                        // KELA ei halua vuosisata merkkia
                        // henkilotunnukseen!
                        StringBuilder kelanVaatimaHenkilotunnus = new StringBuilder();
                        kelanVaatimaHenkilotunnus.append(standardinMukainenHenkilotunnus.substring(0, 6)).append(
                                standardinMukainenHenkilotunnus.substring(7, 11));
                        builder.setHenkilotunnus(kelanVaatimaHenkilotunnus.toString());
                    } catch (Exception e) {
                        LOG.error("Henkilötunnuksen hakeminen hakemuspalvelulta hakemukselle {} epäonnistui!",
                                hakija.getHakemusOid());
                        // e.printStackTrace();
                        builder.setHenkilotunnus("XXXXXXXXXX");
                    }
                    builder.setLukuvuosi(lukuvuosi);
                    builder.setPoimintapaivamaara(poimintapaivamaara);
                    DateTime dateTime = new DateTime(lukuvuosi);
                    if (dateTime.getMonthOfYear() > KESAKUU) { // myohemmin
                                                               // kuin
                                                               // kesakuussa!
                        builder.setSyksyllaAlkavaKoulutus();
                    } else {
                        builder.setKevaallaAlkavaKoulutus();
                    }
                    LOG.info("Tietue KELA-tiedostoon luotu onnistuneesti henkilölle {}", hakija.getHakemusOid());
                    return builder.build();
                }
            }
        }
        // Integer count = streams.size();
        // streams.addFirst(new ByteArrayInputStream(new
        // TKUVAALKU.Builder().setAjopaivamaara(new Date())
        // .setAineistonnimi(StringUtils.EMPTY).setOrganisaationimi(StringUtils.EMPTY).build().toByteArray()));
        // streams.addLast(new ByteArrayInputStream(new
        // TKUVALOPPU.Builder().setAjopaivamaara(new Date())
        // .setTietuelukumaara(count).build().toByteArray()));
        // return new SequenceInputStream(Collections.enumeration(streams));
        throw new SijoittelupalveluException("Sijoittelulta palautui hakija (hakemusoid: " + hakija.getHakemusOid()
                + ") joka ei oikeasti ollut hyväksyttynä koulutukseen!");
    }
}
