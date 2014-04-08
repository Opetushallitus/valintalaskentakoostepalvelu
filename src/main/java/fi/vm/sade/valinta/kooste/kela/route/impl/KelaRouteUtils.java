package fi.vm.sade.valinta.kooste.kela.route.impl;

import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Common methods for KelaFtpRoute and KelaRoute
 */
public class KelaRouteUtils {

	public static String fail() {
		return "bean:kelaValvomo?method=fail(*,*)";
	}

	public static String start() {
		return "bean:kelaValvomo?method=start(*)";
	}

	public static String finish() {
		return "bean:kelaValvomo?method=finish(*)";
	}

	public static String prosessi() {
		return ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI;
	}

	public static String kuvaus() {
		return ValvomoAdminService.PROPERTY_VALVOMO_PROSESSIKUVAUS;
	}

	// public static class PrepareKelaProcessDescription {
	//
	// public Prosessi
	// prepareProcess(@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSIKUVAUS)
	// String kuvaus,
	// @Property(OPH.HAKUOID) String hakuOid,
	// @Property(KelaRoute.PROPERTY_DOKUMENTTI_ID) String dokumenttiId) {
	// return new KelaProsessi(kuvaus, hakuOid, dokumenttiId);
	// }
	// }
}
