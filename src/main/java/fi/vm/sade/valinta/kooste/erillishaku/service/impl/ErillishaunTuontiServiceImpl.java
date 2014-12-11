package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import fi.vm.sade.authentication.model.Henkilo;
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;

import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.sijoittelu.domain.HakemuksenTila;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuExcel;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.service.ErillishaunTuontiService;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * @author Jussi Jartamo
 */
@Service
public class ErillishaunTuontiServiceImpl implements ErillishaunTuontiService {

    private static final Logger LOG = LoggerFactory
            .getLogger(ErillishaunTuontiServiceImpl.class);
    private final TilaAsyncResource tilaAsyncResource;
    private final ApplicationAsyncResource applicationAsyncResource;
    private final HenkiloAsyncResource henkiloAsyncResource;
    private final org.joda.time.format.DateTimeFormatter dtf = DateTimeFormat.forPattern("dd.MM.yyyy");


    @Autowired
    public ErillishaunTuontiServiceImpl(TilaAsyncResource tilaAsyncResource,
                                        ApplicationAsyncResource applicationAsyncResource,
                                        HenkiloAsyncResource henkiloAsyncResource) {
        this.applicationAsyncResource = applicationAsyncResource;
        this.tilaAsyncResource = tilaAsyncResource;
        this.henkiloAsyncResource = henkiloAsyncResource;
    }

    @Override
    public void tuo(KirjeProsessi prosessi, ErillishakuDTO erillishaku,
                    InputStream data) {
        LOG.error("Aloitetaan tuonti");
        Observable.just(erillishaku).subscribeOn(Schedulers.newThread()).subscribe(haku -> {
            ErillishakuExcel erillishakuExcel;
            final List<HenkiloCreateDTO> henkiloPrototyypit = Lists.newArrayList();
            final Map<String, ErillishakuRivi> hetuToRivi = Maps.newHashMap();
            try {
                erillishakuExcel = createExcel(haku, henkiloPrototyypit, hetuToRivi);
            } catch (Exception e) {
                LOG.error("Excelin muodostus epaonnistui! {}", e);
                throw new RuntimeException(e);
            }
            final Collection<ErillishaunHakijaDTO> hakijat;
            try {
                erillishakuExcel.getExcel().tuoXlsx(data);
                hakijat = applicationAsyncResource.putApplicationPrototypes(haku.getHakuOid(), haku.getHakukohdeOid(), haku.getTarjoajaOid(),
                        henkiloAsyncResource.haeHenkilot(henkiloPrototyypit)
                                .get()
                                .stream()
                                .map(personToHakemusPrototyyppi).collect(Collectors.toList()))
                        .get()
                        .stream()
                        .map(hakemusToHakija(haku, hetuToRivi)).collect(Collectors.toList());

            } catch (Throwable e) {
                LOG.error("Excelin tuonti epaonnistui", e);
                throw new RuntimeException(e);
            }
            LOG.error("Viedaan hakijoita {} jonoon {}", hakijat.size(), haku.getValintatapajononNimi());
            if (!hakijat.isEmpty()) {
                tilaAsyncResource.tuoErillishaunTilat(haku.getHakuOid(), haku.getHakukohdeOid(), haku.getValintatapajononNimi(), hakijat);
            } else {
                LOG.error("Taulukkolaskentatiedostosta ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
                throw new RuntimeException("Taulukkolaskentatiedostosta ei saatu poimittua yhtaan hakijaa sijoitteluun tuotavaksi!");
            }
            prosessi.vaiheValmistui();
            prosessi.valmistui("ok");
        }, poikkeus -> {
            if (poikkeus == null) {
                LOG.error("Suoritus keskeytyi tuntemattomaan NPE poikkeukseen!");
            } else {
                LOG.error("Erillishaun tuonti keskeytyi virheeseen", poikkeus);
            }
            prosessi.keskeyta();
        });
    }

    private ErillishakuExcel createExcel(ErillishakuDTO haku, List<HenkiloCreateDTO> henkiloPrototyypit, Map<String, ErillishakuRivi> hetuToRivi) {
        return new ErillishakuExcel(haku.getHakutyyppi(), rivi -> {
            if (rivi.getHenkilotunnus() == null || rivi.getSyntymaAika() == null) {
                LOG.warn("Käyttökelvoton rivi {}", rivi);
                return;
            }
            hetuToRivi.put(Optional.ofNullable(StringUtils.trimToNull(rivi.getHenkilotunnus())).orElse(rivi.getSyntymaAika()), rivi);
            henkiloPrototyypit.add(new HenkiloCreateDTO(rivi.getEtunimi(), rivi.getSukunimi(), rivi.getHenkilotunnus(), parseSyntymaAika(rivi), HenkiloTyyppi.OPPIJA));
        });
    }

    private Function<Henkilo, HakemusPrototyyppi> personToHakemusPrototyyppi = h -> {
        LOG.error("Hakija {}", new GsonBuilder().setPrettyPrinting().create().toJson(h));
        return new HakemusPrototyyppi(h.getOidHenkilo(), h.getEtunimet(), h.getSukunimi(), h.getHetu(), null);
    };

    private Function<Hakemus, ErillishaunHakijaDTO> hakemusToHakija(final ErillishakuDTO erillishaku, final Map<String, ErillishakuRivi> hetuToRivi) {
        return hakemus -> {
            HakemusWrapper wrapper = new HakemusWrapper(hakemus);
            ErillishakuRivi rivi = hetuToRivi.get(wrapper.getHenkilotunnusTaiSyntymaaika());

            ErillishaunHakijaDTO hakija = new ErillishaunHakijaDTO();
            hakija.setHakemuksenTila(hakemuksenTila(rivi));
            hakija.setHakemusOid(hakemus.getOid());
            hakija.setHakijaOid(hakemus.getPersonOid());
            hakija.setHakukohdeOid(erillishaku.getHakukohdeOid());
            hakija.setHakuOid(erillishaku.getHakuOid());
            hakija.setSukunimi(wrapper.getSukunimi());
            hakija.setEtunimi(wrapper.getEtunimi());
            hakija.setIlmoittautumisTila(ilmoittautumisTila(rivi));
            hakija.setValintatuloksenTila(valintatuloksenTila(rivi));
            hakija.setJulkaistavissa(rivi.isJulkaistaankoTiedot());
            hakija.setTarjoajaOid(erillishaku.getTarjoajaOid());
            hakija.setValintatapajonoOid(erillishaku.getValintatapajonoOid());
            return hakija;
        };
    }

    private HakemuksenTila hakemuksenTila(ErillishakuRivi rivi) {
        return Optional.ofNullable(parseField(() -> HakemuksenTila.valueOf(rivi.getHakemuksenTila()))).orElse(HakemuksenTila.HYLATTY);
    }

    private IlmoittautumisTila ilmoittautumisTila(ErillishakuRivi rivi) {
        return parseField(() -> IlmoittautumisTila.valueOf(rivi.getIlmoittautumisTila()));
    }

    private ValintatuloksenTila valintatuloksenTila(ErillishakuRivi rivi) {
        return parseField(() -> ValintatuloksenTila.valueOf(rivi.getVastaanottoTila()));
    }

    private <T> T parseField(Supplier<T> lambda) {
        try {
            return lambda.get();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Date parseSyntymaAika(ErillishakuRivi rivi) {
        try {
            return dtf.parseDateTime(rivi.getSyntymaAika()).toDate();
        } catch (Exception e) {
            LOG.error("Syntymäaikaa {} ei voitu paria muodolle dd.MM.yyyy", rivi.getSyntymaAika());
            return null;
        }
    }
}
