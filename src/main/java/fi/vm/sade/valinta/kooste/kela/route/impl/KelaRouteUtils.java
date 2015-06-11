package fi.vm.sade.valinta.kooste.kela.route.impl;

import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

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
}
