package fi.vm.sade.valinta.kooste.util;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import junit.framework.Assert;

import org.junit.Test;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

public class TestTodellisenJonosijanLaskentaUtiliteetti {

	@Test
	public void testaaJonosijanLaskentaUtiliteetti() {
		HakutoiveenValintatapajonoDTO valintatapajono = new HakutoiveenValintatapajonoDTO();
		valintatapajono.setJonosija(6);

		Multiset<Integer> hyvaksyttyjenJonosijat = TreeMultiset.<Integer>create();
		hyvaksyttyjenJonosijat.add(1);
		hyvaksyttyjenJonosijat.add(2);
		hyvaksyttyjenJonosijat.add(3);
		hyvaksyttyjenJonosijat.add(4);
		hyvaksyttyjenJonosijat.add(5);
		Assert.assertEquals(5, TodellisenJonosijanLaskentaUtiliteetti.laskeTodellinenJonosija(valintatapajono, hyvaksyttyjenJonosijat));
	}
	
	@Test
	public void testaaJonosijanLaskentaUtiliteettiMontaSamaa() {
		HakutoiveenValintatapajonoDTO valintatapajono = new HakutoiveenValintatapajonoDTO();
		valintatapajono.setJonosija(5);

		Multiset<Integer> hyvaksyttyjenJonosijat = TreeMultiset.<Integer>create();
		hyvaksyttyjenJonosijat.add(1);
		hyvaksyttyjenJonosijat.add(2);
		hyvaksyttyjenJonosijat.add(3);
		hyvaksyttyjenJonosijat.add(4);
		hyvaksyttyjenJonosijat.add(4);
		hyvaksyttyjenJonosijat.add(4);
		Assert.assertEquals(4, TodellisenJonosijanLaskentaUtiliteetti.laskeTodellinenJonosija(valintatapajono, hyvaksyttyjenJonosijat));
	}
	
	@Test
	public void testaaJonosijanLaskentaUtiliteettiMontaSamaaKunEiAlaYkkosesta() {
		HakutoiveenValintatapajonoDTO valintatapajono = new HakutoiveenValintatapajonoDTO();
		valintatapajono.setJonosija(14);
		valintatapajono.setTasasijaJonosija(1);

		Multiset<Integer> hyvaksyttyjenJonosijat = TreeMultiset.<Integer>create();
		hyvaksyttyjenJonosijat.add(11);
		hyvaksyttyjenJonosijat.add(12);
		hyvaksyttyjenJonosijat.add(13);
		hyvaksyttyjenJonosijat.add(14);
		hyvaksyttyjenJonosijat.add(14);
		hyvaksyttyjenJonosijat.add(14);
		Assert.assertEquals(4, TodellisenJonosijanLaskentaUtiliteetti.laskeTodellinenJonosija(valintatapajono, hyvaksyttyjenJonosijat));
	}
	@Test
	public void testaaJonosijanLaskentaUtiliteettiMonenSamanJalkeenKunEiAlaYkkosesta() {
		HakutoiveenValintatapajonoDTO valintatapajono = new HakutoiveenValintatapajonoDTO();
		valintatapajono.setJonosija(15);
		valintatapajono.setTasasijaJonosija(1);

		Multiset<Integer> hyvaksyttyjenJonosijat = TreeMultiset.<Integer>create();
		hyvaksyttyjenJonosijat.add(11);
		hyvaksyttyjenJonosijat.add(12);
		hyvaksyttyjenJonosijat.add(13);
		hyvaksyttyjenJonosijat.add(14);
		hyvaksyttyjenJonosijat.add(14);
		hyvaksyttyjenJonosijat.add(14);
		hyvaksyttyjenJonosijat.add(15);
		Assert.assertEquals(7, TodellisenJonosijanLaskentaUtiliteetti.laskeTodellinenJonosija(valintatapajono, hyvaksyttyjenJonosijat));
	}
}
