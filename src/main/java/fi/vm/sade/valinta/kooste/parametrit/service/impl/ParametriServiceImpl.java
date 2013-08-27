package fi.vm.sade.valinta.kooste.parametrit.service.impl;

import fi.vm.sade.valinta.kooste.parametrit.Parametrit;
import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

/**
 * User: tommiha
 * Date: 8/21/13
 * Time: 10:05 AM
 */
@Service
public class ParametriServiceImpl implements ParametriService {

    @Autowired
    private Parametrit parametrit;

    @Value("${root.organisaatio.oid}")
    private String rootOrganisaatioOid;

    @Override
    public boolean pistesyottoEnabled(String hakuOid) {
        Date now = Calendar.getInstance().getTime();
        Date koetulokset = new Date(parametrit.getKoetuloksetPvm() * 1000);
        Date hakuLoppupvm = new Date(parametrit.getHakuLoppupvm() * 1000);
        return isOPH() || (now.before(koetulokset) && now.after(hakuLoppupvm));
    }

    @Override
    public boolean hakeneetEnabled(String hakuOid) {
        Date now = Calendar.getInstance().getTime();
        Date hakuAlkupvm = new Date(parametrit.getHakuAlkupvm() * 1000);
        return now.after(hakuAlkupvm) || isOPH();
    }

    @Override
    public boolean harkinnanvaraisetEnabled(String hakuOid) {
        Date now = Calendar.getInstance().getTime();
        Date hakuLoppupvm = new Date(parametrit.getHakuLoppupvm() * 1000);
        Date koetuloksetPvm = new Date(parametrit.getKoetuloksetPvm() * 1000);
        return isOPH() || (now.after(hakuLoppupvm) && now.before(koetuloksetPvm));
    }

    @Override
    public boolean valintakoekutsutEnabled(String hakuOid) {
        Date now = Calendar.getInstance().getTime();
        Date hakuLoppupvm = new Date(parametrit.getHakuLoppupvm() * 1000);
        return now.after(hakuLoppupvm) || isOPH();
    }

    @Override
    public boolean valintalaskentaEnabled(String hakuOid) {
        Date now = Calendar.getInstance().getTime();
        Date hakuAlkupvm = new Date(parametrit.getHakuAlkupvm() * 1000);
        return now.after(hakuAlkupvm) && isOPH();
    }

    @Override
    public boolean valinnanhallintaEnabled(String hakuOid) {
        Date now = Calendar.getInstance().getTime();
        Date valintaesitysPvm = new Date(parametrit.getValintaesitysPvm() * 1000);
        return isOPH() || now.after(valintaesitysPvm);
    }

    private boolean isOPH() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        for(GrantedAuthority authority : authentication.getAuthorities()) {
            if(authority.getAuthority().contains(rootOrganisaatioOid)) {
                return true;
            }
        }

        return false;
    }
}
