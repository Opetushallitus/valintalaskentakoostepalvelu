package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakuUuidHakukohdeJaOrganisaatio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static fi.vm.sade.valinta.kooste.util.SuoritusrekisteriUtil.getEnsikertalaisuudenRajapvm;

public class SuoritusrekisteriPalvelukutsu extends AbstraktiPalvelukutsu implements Palvelukutsu {
    private final static Logger LOG = LoggerFactory.getLogger(HakijaryhmatPalvelukutsu.class);
    private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
    private final AtomicReference<List<Oppija>> oppijat;
    private final HakuV1RDTO haku;

    public SuoritusrekisteriPalvelukutsu(HakuUuidHakukohdeJaOrganisaatio hakuData, SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource) {
        super(hakuData);
        this.haku = hakuData.getHaku();
        this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
        this.oppijat = new AtomicReference<>();
    }

    @Override
    public void vapautaResurssit() {
        oppijat.set(null);
    }

    public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
        aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(() ->
                        suoritusrekisteriAsyncResource.getOppijatByHakukohde(getHakukohdeOid(), getEnsikertalaisuudenRajapvm(haku), // referenssiPvm ensikertalaisuutta varten
                            oppijat -> {
                                SuoritusrekisteriPalvelukutsu.this.oppijat.set(oppijat);
                                takaisinkutsu.accept(SuoritusrekisteriPalvelukutsu.this);
                            }, failureCallback(takaisinkutsu))
        );
        return this;
    }

    public List<Oppija> getOppijat() {
        return oppijat.get();
    }
}