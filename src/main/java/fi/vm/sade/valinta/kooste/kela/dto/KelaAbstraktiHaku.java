package fi.vm.sade.valinta.kooste.kela.dto;

import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakukohdeSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.HenkilotietoSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.LinjakoodiSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.OppilaitosSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.PaivamaaraSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.TilaSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.TutkinnontasoSource;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public abstract class KelaAbstraktiHaku {

  public static final String SUKUNIMI = "sukunimi";
  public static final String ETUNIMET = "Etunimet";

  private final Haku haku;
  private final PaivamaaraSource paivamaaraSource;

  public KelaAbstraktiHaku(Haku haku, PaivamaaraSource paivamaaraSource) {
    this.haku = haku;
    this.paivamaaraSource = paivamaaraSource;
  }

  public Haku getHaku() {
    return haku;
  }

  protected PaivamaaraSource getPaivamaaraSource() {
    return paivamaaraSource;
  }

  /** @return kela hakuun liittyvat hakemus oidit */
  public abstract Collection<String> getHakemusOids();

  public abstract List<String> getPersonOids();

  public abstract Collection<KelaHakijaRivi> createHakijaRivit(
      Date alkuPvm,
      Date loppuPvm,
      String hakuOid,
      KelaProsessi prosessi,
      HenkilotietoSource henkilotietoSource,
      HakukohdeSource hakukohdeSource,
      LinjakoodiSource linjakoodiSource,
      OppilaitosSource oppilaitosSource,
      TutkinnontasoSource tutkinnontasoSource,
      TilaSource tilaSource);
}
