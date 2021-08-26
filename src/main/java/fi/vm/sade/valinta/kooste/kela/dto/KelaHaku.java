package fi.vm.sade.valinta.kooste.kela.dto;

import com.google.common.collect.Lists;
import fi.vm.sade.organisaatio.resource.api.TasoJaLaajuusDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Hakukohde;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakukohdeSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.HenkilotietoSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.LinjakoodiSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.OppilaitosSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.PaivamaaraSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.TilaSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.TutkinnontasoSource;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KelaHaku extends KelaAbstraktiHaku {
  private static final Logger LOG = LoggerFactory.getLogger(KelaHaku.class);
  private final Collection<ValintaTulosServiceDto> hakijat;

  public KelaHaku(
      Collection<ValintaTulosServiceDto> hakijat, Haku haku, PaivamaaraSource paivamaaraSource) {
    super(haku, paivamaaraSource);
    this.hakijat = hakijat;
  }

  @Override
  public Collection<KelaHakijaRivi> createHakijaRivit(
      Date alkuPvm,
      Date loppuPvm,
      String hakuOid,
      KelaProsessi prosessi,
      HenkilotietoSource henkilotietoSource,
      HakukohdeSource hakukohdeSource,
      LinjakoodiSource linjakoodiSource,
      OppilaitosSource oppilaitosSource,
      TutkinnontasoSource tutkinnontasoSource,
      TilaSource tilaSource) {

    Collection<KelaHakijaRivi> valitut = Lists.newArrayList();
    prosessi.setKokonaistyo(hakijat.size() - 1);
    for (ValintaTulosServiceDto hakija : hakijat) {
      prosessi.inkrementoiTehtyjaToita();
      hakija.getHakutoiveet().stream()
          .filter(h -> h != null && h.getValintatila() != null && h.getVastaanottotila() != null)
          .filter(
              hakutoive ->
                  hakutoive.getValintatila().isHyvaksytty()
                      && hakutoive.getVastaanottotila().isVastaanottanut())
          .findFirst()
          .ifPresent(
              hakutoive -> {
                HenkiloPerustietoDto henkilotiedot =
                    henkilotietoSource.getByPersonOid(hakija.getHakijaOid());
                String hakukohdeOid = hakutoive.getHakukohdeOid();
                Hakukohde hakuKohde = hakukohdeSource.getHakukohdeByOid(hakukohdeOid);
                final String etunimi = henkilotiedot.getEtunimet();
                final String sukunimi = henkilotiedot.getSukunimi();
                final String henkilotunnus = henkilotiedot.getHetu();
                final String syntymaaika =
                    henkilotiedot.getSyntymaaika() == null
                        ? ""
                        : henkilotiedot.getSyntymaaika().toString();
                final Date lukuvuosi = getPaivamaaraSource().lukuvuosi(getHaku(), hakukohdeOid);
                final Date poimintapaivamaara = getPaivamaaraSource().poimintapaivamaara(getHaku());

                Date valintapaivamaara =
                    tilaSource.getVastaanottopvm(
                        hakija.getHakemusOid(),
                        hakuOid,
                        hakukohdeOid,
                        hakutoive.getValintatapajonoOid());

                if (valintapaivamaara == null) {
                  LOG.error(
                      "ERROR vastaanottopaivamaaraa ei löytynyt (tila ei VASTAANOTTANUT_SITOVASTI tai EHDOLLISESTI_VASTAANOTTANUT) :"
                          + hakutoive.getTarjoajaOid()
                          + ":"
                          + sukunimi
                          + " "
                          + etunimi
                          + "("
                          + henkilotunnus
                          + ") hakukohde:"
                          + hakukohdeOid
                          + " ");
                  return;
                }

                if (valintapaivamaara == null
                    || valintapaivamaara.compareTo(alkuPvm) < 0
                    || valintapaivamaara.compareTo(loppuPvm) >= 0) {
                  return;
                }

                Iterator<String> i = hakuKohde.tarjoajaOids.iterator();
                String organisaatioOid = i.hasNext() ? i.next() : null;
                String oppilaitosnumero = "XXXXX";

                if (organisaatioOid == null || organisaatioOid.equalsIgnoreCase("undefined")) {
                  LOG.error(
                      "ERROR : tarjoaja :'"
                          + hakutoive.getTarjoajaOid()
                          + "':"
                          + sukunimi
                          + " "
                          + etunimi
                          + "("
                          + henkilotunnus
                          + ") hakukohde:"
                          + hakukohdeOid
                          + " "
                          + " oppilaitosnumero will be marked as X");
                } else {
                  oppilaitosnumero = oppilaitosSource.getOppilaitosnumero(organisaatioOid);
                }

                final TasoJaLaajuusDTO tutkinnontaso =
                    tutkinnontasoSource.getTutkinnontaso(hakukohdeOid);
                final String siirtotunnus = tutkinnontasoSource.getKoulutusaste(hakukohdeOid);

                valitut.add(
                    new KelaHakijaRivi(
                        hakija.getHakemusOid(),
                        siirtotunnus,
                        etunimi,
                        sukunimi,
                        henkilotunnus,
                        lukuvuosi,
                        poimintapaivamaara,
                        valintapaivamaara,
                        oppilaitosnumero,
                        organisaatioOid,
                        hakuOid,
                        hakukohdeOid,
                        syntymaaika,
                        tutkinnontaso));
              });
    }
    return valitut;
  }

  @Override
  public Collection<String> getHakemusOids() {
    return hakijat.stream().map(h -> h.getHakemusOid()).collect(Collectors.toList());
  }

  @Override
  public List<String> getPersonOids() {
    return hakijat.stream().map(h -> h.getHakijaOid()).collect(Collectors.toList());
  }
}
