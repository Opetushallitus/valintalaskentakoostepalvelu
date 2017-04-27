package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.UuidHakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.AbstraktiPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.PalvelukutsuLaskuri;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategia;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


/**
 *         Abstrakti laskenta tekee tilapaivitykset ja palvelukutsut.
 *         Varsinainen laskennan toteuttaja tekee laskennan ja laskentaDTO:n
 *         luonnin.
 */
public abstract class AbstraktiLaskentaPalvelukutsu extends AbstraktiPalvelukutsu implements LaskentaPalvelukutsu {
    private final Logger LOG = LoggerFactory.getLogger(AbstraktiLaskentaPalvelukutsu.class);

    private final Collection<PalvelukutsuJaPalvelukutsuStrategia> palvelukutsut;
    private final Consumer<Palvelukutsu> laskuri;
    private final HakuV1RDTO haku;
    private final ParametritDTO parametritDTO;
    private final Observable<Map<String, List<String>>> hakukohdeRyhmasForHakukohdesObservable;
    private volatile HakukohdeTila tila = HakukohdeTila.TEKEMATTA;
    private final AtomicReference<Consumer<LaskentaPalvelukutsu>> takaisinkutsu;

    public AbstraktiLaskentaPalvelukutsu(HakuV1RDTO haku,
                                         ParametritDTO parametritDTO,
                                         UuidHakukohdeJaOrganisaatio kuvaus,
                                         Collection<PalvelukutsuJaPalvelukutsuStrategia> palvelukutsut,
                                         TarjontaAsyncResource tarjontaAsyncResource) {
        super(kuvaus);
        this.haku = haku;
        this.hakukohdeRyhmasForHakukohdesObservable = tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(haku.getOid());
        this.parametritDTO = parametritDTO;
        this.palvelukutsut = palvelukutsut;
        this.takaisinkutsu = new AtomicReference<>();
        final PalvelukutsuLaskuri palvelukutsulaskuri = new PalvelukutsuLaskuri(palvelukutsut.size());
        this.laskuri = pk -> {
            yksiVaiheValmistui();
            if (takaisinkutsu.get() == null) {
                return;
            }
            if (pk.onkoPeruutettu()) {
                peruuta();
                try {
                    takaisinkutsu.getAndUpdate(tk -> {
                        if (tk != null) {
                            tk.accept(AbstraktiLaskentaPalvelukutsu.this);
                        }
                        return null;
                    });
                } catch (Exception e) {
                    LOG.error("Laskentapalvelukutsun takaisinkutsu epaonnistui", e);
                    throw e;
                }

            } else {
                int yhteensa = palvelukutsulaskuri.getYhteensa();
                int laskuriNyt = palvelukutsulaskuri.palvelukutsuSaapui();

                LOG.info("Saatiin {} (uuid={},hakukohde/tunniste={}): {}/{}", StringUtils.rightPad(pk.getClass().getSimpleName(), 38), getUuid(), getHakukohdeOid(), (-laskuriNyt) + yhteensa, yhteensa);
                if (laskuriNyt == 0) {
                    tila = HakukohdeTila.VALMIS;
                    try {
                        takaisinkutsu.getAndUpdate(tk -> {
                            if (tk != null) {
                                tk.accept(AbstraktiLaskentaPalvelukutsu.this);
                            }
                            return null;
                        });
                    } catch (Exception e) {
                        LOG.error("Laskentapalvelukutsun takaisinkutsu epaonnistui", e);
                        throw e;
                    }
                } else if (laskuriNyt < 0) {
                    LOG.error("Laskenta sai enemman paluuarvoja palvelukutsuista kuin kutsuja tehtiin!");
                    throw new RuntimeException("Laskenta sai enemman paluuarvoja palvelukutsuista kuin kutsuja tehtiin!");
                }
            }
        };
    }

    protected void yksiVaiheValmistui() {

    }

    /**
     * Peruuttaa kaikki palvelukutsut esitoina talle palvelukutsulle.
     */
    @Override
    public void peruuta() {
        try {
            super.peruuta();
            palvelukutsut.forEach(PalvelukutsuJaPalvelukutsuStrategia::peruuta);
        } catch (Exception e) {
            LOG.error("AbstraktiLaskentaPalvelukutsun peruutus epaonnistui!", e);
        }
    }

    @Override
    public HakukohdeTila getHakukohdeTila() {
        return onkoPeruutettu() ? HakukohdeTila.KESKEYTETTY : tila;
    }

    public void laitaTyojonoon(Consumer<LaskentaPalvelukutsu> takaisinkutsu) {
        LOG.info("Laitetaan tyot tyojonoon hakukohteelle {}", getHakukohdeOid());
        this.takaisinkutsu.set(takaisinkutsu);
        palvelukutsut.forEach(tyojono -> tyojono.laitaPalvelukutsuTyojonoon(laskuri));
    }

    protected List<HakemusDTO> muodostaHakemuksetDTO(String hakukohdeOid, List<Hakemus> hakemukset, List<Oppija> oppijat) {
        Map<String, List<String>> hakukohdeRyhmasForHakukohdes = hakukohdeRyhmasForHakukohdesObservable.toBlocking().first();
        return HakemuksetConverterUtil.muodostaHakemuksetDTO(haku, hakukohdeOid, hakukohdeRyhmasForHakukohdes, hakemukset, oppijat, parametritDTO, true);
    }

}
