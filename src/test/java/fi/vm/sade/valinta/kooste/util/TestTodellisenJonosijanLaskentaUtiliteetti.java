package fi.vm.sade.valinta.kooste.util;

import junit.framework.Assert;

import org.junit.Test;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.TreeMultiset;

public class TestTodellisenJonosijanLaskentaUtiliteetti {

	@Test
	public void testaaJonosijanLaskentaUtiliteetti() {
		Multiset<Integer> hyvaksyttyjenJonosijat = TreeMultiset.<Integer>create();
		hyvaksyttyjenJonosijat.add(1);
		hyvaksyttyjenJonosijat.add(2);
		hyvaksyttyjenJonosijat.add(3);
		hyvaksyttyjenJonosijat.add(4);
		hyvaksyttyjenJonosijat.add(5);
		Assert.assertEquals(5, TodellisenJonosijanLaskentaUtiliteetti.laskeTodellinenJonosija(5, hyvaksyttyjenJonosijat));
	}
	
	@Test
	public void testaaJonosijanLaskentaUtiliteettiMontaSamaa() {
		Multiset<Integer> hyvaksyttyjenJonosijat = TreeMultiset.<Integer>create();
		hyvaksyttyjenJonosijat.add(1);
		hyvaksyttyjenJonosijat.add(2);
		hyvaksyttyjenJonosijat.add(3);
		hyvaksyttyjenJonosijat.add(4);
		hyvaksyttyjenJonosijat.add(4);
		hyvaksyttyjenJonosijat.add(4);
		Assert.assertEquals(4, TodellisenJonosijanLaskentaUtiliteetti.laskeTodellinenJonosija(4, hyvaksyttyjenJonosijat));
	}
	
	@Test
	public void testaaJonosijanLaskentaUtiliteettiMontaSamaaKunEiAlaYkkosesta() {
		Multiset<Integer> hyvaksyttyjenJonosijat = TreeMultiset.<Integer>create();
		hyvaksyttyjenJonosijat.add(11);
		hyvaksyttyjenJonosijat.add(12);
		hyvaksyttyjenJonosijat.add(13);
		hyvaksyttyjenJonosijat.add(14);
		hyvaksyttyjenJonosijat.add(14);
		hyvaksyttyjenJonosijat.add(14);
		Assert.assertEquals(4, TodellisenJonosijanLaskentaUtiliteetti.laskeTodellinenJonosija(14, hyvaksyttyjenJonosijat));
	}
	@Test
	public void testaaJonosijanLaskentaUtiliteettiMonenSamanJalkeenKunEiAlaYkkosesta() {
		Multiset<Integer> hyvaksyttyjenJonosijat = TreeMultiset.<Integer>create();
		hyvaksyttyjenJonosijat.add(11);
		hyvaksyttyjenJonosijat.add(12);
		hyvaksyttyjenJonosijat.add(13);
		hyvaksyttyjenJonosijat.add(14);
		hyvaksyttyjenJonosijat.add(14);
		hyvaksyttyjenJonosijat.add(14);
		hyvaksyttyjenJonosijat.add(15);
		Assert.assertEquals(7, TodellisenJonosijanLaskentaUtiliteetti.laskeTodellinenJonosija(15, hyvaksyttyjenJonosijat));
	}
}
