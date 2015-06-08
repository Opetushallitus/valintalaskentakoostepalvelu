package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoJarjestyskriteereillaDTO;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.UuidHakukohdeJaOrganisaatio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;


/**
 * @author Jussi Jartamo
 */
public class ValintaperusteetPalvelukutsu extends AbstraktiPalvelukutsu implements Palvelukutsu {
    private final Logger LOG = LoggerFactory.getLogger(ValintaperusteetPalvelukutsu.class);

    private final Integer valinnanVaiheJarjestysluku;
    private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
    private final AtomicReference<List<ValintaperusteetDTO>> valintaperusteet;

    public ValintaperusteetPalvelukutsu(UuidHakukohdeJaOrganisaatio hakukohdeOid, Integer valinnanVaiheJarjestysluku, ValintaperusteetAsyncResource valintaperusteetAsyncResource) {
        super(hakukohdeOid);
        this.valinnanVaiheJarjestysluku = valinnanVaiheJarjestysluku;
        this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
        this.valintaperusteet = new AtomicReference<>();
    }

    @Override
    public void vapautaResurssit() {
        valintaperusteet.set(null);
    }

    public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
        aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(() -> valintaperusteetAsyncResource.haeValintaperusteet(
                getHakukohdeOid(),
                valinnanVaiheJarjestysluku,
                valintaperusteet -> {
                    if (valintaperusteet == null) {
                        LOG.error("Valintaperusteetpalvelu palautti null datajoukon!");
                        failureCallback(takaisinkutsu);
                        return;
                    }

                    // Filteröidään pois jonot, joissa ei käytetä laskentaa
                    final List<ValintaperusteetDTO> filteroidyt = valintaperusteet.stream()
                            .map(vp -> {
                                List<ValintatapajonoJarjestyskriteereillaDTO> laskentaJonot = vp.getValinnanVaihe().getValintatapajono().stream()
                                        .filter(j -> j.getKaytetaanValintalaskentaa() == null || j.getKaytetaanValintalaskentaa())
                                        .collect(Collectors.toList());
                                vp.getValinnanVaihe().setValintatapajono(laskentaJonot);
                                return vp;
                            }).collect(Collectors.toList());

                    ValintaperusteetPalvelukutsu.this.valintaperusteet.set(filteroidyt);
                    takaisinkutsu.accept(ValintaperusteetPalvelukutsu.this);
                },
                failureCallback(takaisinkutsu))
        );
        return this;
    }

    public List<ValintaperusteetDTO> getValintaperusteet() {
        return valintaperusteet.get();
    }

}
