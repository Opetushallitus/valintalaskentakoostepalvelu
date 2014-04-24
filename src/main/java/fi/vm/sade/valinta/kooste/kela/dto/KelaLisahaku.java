package fi.vm.sade.valinta.kooste.kela.dto;

import static fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource.HENKILOTUNNUS;
import static fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource.SYNTYMAAIKA;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import com.google.common.collect.Lists;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakemusSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakukohdeSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.LinjakoodiSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.OppilaitosSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.PaivamaaraSource;

public class KelaLisahaku extends KelaAbstraktiHaku {

	private static final String LISAHAKU_HYVAKSYTTY = "lisahaku-hyvaksytty";
	private final Collection<String> hakemusOids;

	public KelaLisahaku(Collection<String> hakemusOids, HakuV1RDTO haku,
			PaivamaaraSource paivamaaraSource) {
		super(haku, paivamaaraSource);
		this.hakemusOids = hakemusOids;
	}

	@Override
	public Collection<String> getHakemusOids() {
		return hakemusOids;
	}

	@Override
	public Collection<KelaHakijaRivi> createHakijaRivit(
			HakemusSource hakemusSource, HakukohdeSource hakukohdeSource,
			LinjakoodiSource linjakoodiSource, OppilaitosSource oppilaitosSource) {
		Collection<KelaHakijaRivi> valitut = Lists.newArrayList();
		for (String hakemusOid : hakemusOids) {
			Hakemus hakemus = hakemusSource.getHakemusByOid(hakemusOid);
			Map<String, String> additionalInfo = additionalInfo(hakemus);
			if (additionalInfo.containsKey(LISAHAKU_HYVAKSYTTY)) {
				String hakukohdeOid = additionalInfo.get(LISAHAKU_HYVAKSYTTY);
				Map<String, String> henkilotiedot = henkilotiedot(hakemus);
				HakukohdeDTO hakukohde = hakukohdeSource
						.getHakukohdeByOid(hakukohdeOid);
				final String etunimi = henkilotiedot.get(ETUNIMET);
				final String sukunimi = henkilotiedot.get(SUKUNIMI);
				final String henkilotunnus = henkilotiedot.get(HENKILOTUNNUS);
				final String syntymaaika = henkilotiedot.get(SYNTYMAAIKA);

				final Date lukuvuosi = getPaivamaaraSource().lukuvuosi(
						getHaku());
				final Date poimintapaivamaara = getPaivamaaraSource()
						.poimintapaivamaara(getHaku());
				final Date valintapaivamaara = getPaivamaaraSource()
						.valintapaivamaara(getHaku());
				final String linjakoodi = linjakoodiSource
						.getLinjakoodi(hakukohde.getHakukohdeNimiUri());
				final String oppilaitos = oppilaitosSource
						.getOppilaitosKoodi(hakukohde.getTarjoajaOid());

				valitut.add(new KelaHakijaRivi(etunimi, sukunimi,
						henkilotunnus, lukuvuosi, poimintapaivamaara,
						valintapaivamaara, linjakoodi, oppilaitos, syntymaaika));
			} else {
				continue;
			}
		}
		return valitut;
	}
}
