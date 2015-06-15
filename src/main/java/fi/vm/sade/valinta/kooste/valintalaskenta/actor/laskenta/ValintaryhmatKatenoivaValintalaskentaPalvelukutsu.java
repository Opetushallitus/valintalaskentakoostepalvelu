package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.UuidHakukohdeJaOrganisaatio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.SuoritusrekisteriPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.ValintaperusteetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategiaImpl;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;

public class ValintaryhmatKatenoivaValintalaskentaPalvelukutsu extends AbstraktiLaskentaPalvelukutsu implements LaskentaPalvelukutsu {
    private static final Logger LOG = LoggerFactory.getLogger(ValintaryhmatKatenoivaValintalaskentaPalvelukutsu.class);

    private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
    private final List<ValintaryhmaPalvelukutsuYhdiste> valintaryhmaPalvelukutsuYhdiste;
    private final AtomicReference<Runnable> callback = new AtomicReference<>();
    private final boolean erillishaku;
    private final String uuid;

    @SuppressWarnings("unchecked")
    public ValintaryhmatKatenoivaValintalaskentaPalvelukutsu(
            HakuV1RDTO haku,
            ParametritDTO parametritDTO,
            boolean erillishaku,
            UuidHakukohdeJaOrganisaatio hakukohdeOid,
            ValintalaskentaAsyncResource valintalaskentaAsyncResource,
            List<ValintaryhmaPalvelukutsuYhdiste> valintaryhmaPalvelukutsuYhdiste,
            List<PalvelukutsuJaPalvelukutsuStrategiaImpl<HakemuksetPalvelukutsu>> hakemuksetPalvelukutsut,
            List<PalvelukutsuJaPalvelukutsuStrategiaImpl<ValintaperusteetPalvelukutsu>> valintaperusteetPalvelukutsut,
            List<PalvelukutsuJaPalvelukutsuStrategiaImpl<HakijaryhmatPalvelukutsu>> hakijaryhmatPalvelukutsut,
            List<PalvelukutsuJaPalvelukutsuStrategiaImpl<SuoritusrekisteriPalvelukutsu>> suoritusrekisteriPalvelukutsut
    ) {
        super(
                haku,
                parametritDTO,
                hakukohdeOid,
                Lists.newArrayList(Iterables.concat(hakemuksetPalvelukutsut, valintaperusteetPalvelukutsut, hakijaryhmatPalvelukutsut, suoritusrekisteriPalvelukutsut))
        );
        this.uuid = hakukohdeOid.getUuid();
        this.erillishaku = erillishaku;
        this.valintaryhmaPalvelukutsuYhdiste = valintaryhmaPalvelukutsuYhdiste;
        this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
    }

    @Override
    public void vapautaResurssit() {
        valintaryhmaPalvelukutsuYhdiste.forEach(ValintaryhmaPalvelukutsuYhdiste::vapautaResurssit);
    }

    public void setCallback(Runnable r) {
        callback.set(r);
    }

    protected void yksiVaiheValmistui() {
        callback.get().run();
    }

    private List<LaskeDTO> muodostaLaskeDTOs() {
        return valintaryhmaPalvelukutsuYhdiste.stream()
                .map(y -> {
                    try {
                        LaskeDTO l = new LaskeDTO(
                                uuid,
                                erillishaku,
                                y.getHakukohdeOid(),
                                muodostaHakemuksetDTO(y.getHakukohdeOid(), y.getHakemuksetPalvelukutsu().getHakemukset(), y.getSuoritusrekisteriPalvelukutsut().getOppijat()),
                                y.getValintaperusteetPalvelukutsu().getValintaperusteet(),
                                y.getHakijaryhmatPalvelukutsu().getHakijaryhmat()
                        );
                        y.vapautaResurssit();
                        return l;
                    } catch (Exception e) {
                        LOG.error("LaskeDTO:n muodostaminen epaonnistui {}", e.getMessage());
                        throw e;
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
        try {
            aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(() -> valintalaskentaAsyncResource.laskeJaSijoittele(muodostaLaskeDTOs(),
                    laskentaCallback -> takaisinkutsu.accept(ValintaryhmatKatenoivaValintalaskentaPalvelukutsu.this),
                    failureCallback(takaisinkutsu)
            ));
        } catch (Exception e) {
            LOG.error("ValintalaskentaPalvelukutsu palvelukutsun muodostus epaonnistui virheeseen {}", e.getMessage());
            failureCallback(takaisinkutsu).accept(e);
        }
        return this;
    }
}
