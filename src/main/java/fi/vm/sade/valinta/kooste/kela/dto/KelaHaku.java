package fi.vm.sade.valinta.kooste.kela.dto;

import static fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource.HENKILOTUNNUS;
import static fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource.SYNTYMAAIKA;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakemusSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakukohdeSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.LinjakoodiSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.OppilaitosSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.PaivamaaraSource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HakutoiveenValintatapajonoComparator;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class KelaHaku extends KelaAbstraktiHaku {

	private final Collection<HakijaDTO> hakijat;

	public KelaHaku(Collection<HakijaDTO> hakijat, HakuDTO haku,
			PaivamaaraSource paivamaaraSource) {
		super(haku, paivamaaraSource);
		//
		// Varmistetaan etta ainoastaan hyvaksyttyja
		//
		this.hakijat = Collections2.filter(hakijat, new Predicate<HakijaDTO>() {
			public boolean apply(HakijaDTO hakija) {
				for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
					Collections.sort(
							hakutoive.getHakutoiveenValintatapajonot(),
							HakutoiveenValintatapajonoComparator.DEFAULT);
					for (HakutoiveenValintatapajonoDTO jono : hakutoive
							.getHakutoiveenValintatapajonot()) {
						if (HakemuksenTila.HYVAKSYTTY.equals(jono.getTila())) {
							return true;
						}
						return false;
					}
				}
				return false;
			}
		});
	}

	@Override
	public Collection<KelaHakijaRivi> createHakijaRivit(
			HakemusSource hakemusSource, HakukohdeSource hakukohdeSource,
			LinjakoodiSource linjakoodiSource, OppilaitosSource oppilaitosSource) {
		Collection<KelaHakijaRivi> valitut = Lists.newArrayList();
		for (HakijaDTO hakija : hakijat) {
			for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
				Collections.sort(hakutoive.getHakutoiveenValintatapajonot(),
						HakutoiveenValintatapajonoComparator.DEFAULT);
				for (HakutoiveenValintatapajonoDTO jono : hakutoive
						.getHakutoiveenValintatapajonot()) {
					if (HakemuksenTila.HYVAKSYTTY.equals(jono.getTila())) {
						Hakemus hakemus = hakemusSource.getHakemusByOid(hakija
								.getHakemusOid());
						Map<String, String> henkilotiedot = henkilotiedot(hakemus);
						String hakukohdeOid = hakutoive.getHakukohdeOid();

						HakukohdeDTO hakukohde = hakukohdeSource
								.getHakukohdeByOid(hakukohdeOid);
						final String etunimi = henkilotiedot.get(ETUNIMET);
						final String sukunimi = henkilotiedot.get(SUKUNIMI);
						final String henkilotunnus = henkilotiedot
								.get(HENKILOTUNNUS);
						final String syntymaaika = henkilotiedot
								.get(SYNTYMAAIKA);
						final Date lukuvuosi = getPaivamaaraSource().lukuvuosi(
								getHaku());
						final Date poimintapaivamaara = getPaivamaaraSource()
								.poimintapaivamaara(getHaku());
						final Date valintapaivamaara = getPaivamaaraSource()
								.valintapaivamaara(getHaku());
						final String linjakoodi = linjakoodiSource
								.getLinjakoodi(hakukohde.getHakukohdeNimiUri());
						final String oppilaitos = oppilaitosSource
								.getOppilaitosKoodi(hakutoive.getTarjoajaOid());

						valitut.add(new KelaHakijaRivi(etunimi, sukunimi,
								henkilotunnus, lukuvuosi, poimintapaivamaara,
								valintapaivamaara, linjakoodi, oppilaitos,
								syntymaaika));
					} else {
						break;
					}
				}
			}
		}
		return valitut;
	}

	@Override
	public Collection<String> getHakemusOids() {
		return Collections2.transform(hakijat,
				new Function<HakijaDTO, String>() {
					@Override
					public String apply(HakijaDTO input) {
						return input.getHakemusOid();
					}
				});
	}
}
