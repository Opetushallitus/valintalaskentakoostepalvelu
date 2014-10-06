package fi.vm.sade.valinta.kooste.parametrit.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import fi.vm.sade.valinta.kooste.parametrit.Parametrit;
import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;

/**
 * User: tommiha Date: 8/21/13 Time: 10:05 AM
 */
@Service
public class ParametriServiceImpl implements ParametriService {

	@Autowired
	private Parametrit parametrit;

	@Value("${root.organisaatio.oid:1.2.246.562.10.00000000001}")
	private String rootOrganisaatioOid;

	@Override
	public boolean pistesyottoEnabled(String hakuOid) {
		if (isOPH()) {
			return true;
		}
		Date now = Calendar.getInstance().getTime();
		Date koetuloksetAlkupvm = parseDate(parametrit.getKoetuloksetAlkupvm());
		Date koetuloksetLoppupvm = parseDate(parametrit
				.getKoetuloksetLoppupvm());
		return now.before(koetuloksetLoppupvm) && now.after(koetuloksetAlkupvm);
	}

	@Override
	public boolean hakeneetEnabled(String hakuOid) {
		if (isOPH()) {
			return true;
		}
		Date now = Calendar.getInstance().getTime();
		Date hakuAlkupvm = parseDate(parametrit.getHakuAlkupvm());
		return now.after(hakuAlkupvm);
	}

	@Override
	public boolean harkinnanvaraisetEnabled(String hakuOid) {
		if (isOPH()) {
			return true;
		}
		Date now = Calendar.getInstance().getTime();
		Date hakuLoppupvm = parseDate(parametrit.getHakuLoppupvm());
		Date koetuloksetPvm = parseDate(parametrit.getKoetuloksetLoppupvm());
		return now.after(hakuLoppupvm) && now.before(koetuloksetPvm);
	}

	@Override
	public boolean valintakoekutsutEnabled(String hakuOid) {
		if (isOPH()) {
			return true;
		}
		Date now = Calendar.getInstance().getTime();
		Date koetuloksetAlkupvm = parseDate(parametrit.getKoetuloksetAlkupvm());
		return now.after(koetuloksetAlkupvm);
	}

	@Override
	public boolean valintalaskentaEnabled(String hakuOid) {
		// Date now = Calendar.getInstance().getTime();
		// Date hakuAlkupvm = new Date(parametrit.getHakuAlkupvm());
		return isOPH(); // now.after(hakuAlkupvm) && isOPH();
	}

    @Override
    public boolean hakijaryhmatEnabled(String hakuOid) {
        if (isOPH()) {
            return true;
        }
        Date now = Calendar.getInstance().getTime();
        Date hakuAlkupvm = parseDate(parametrit.getHakuAlkupvm());
        return now.after(hakuAlkupvm);
    }

	@Override
	public boolean valinnanhallintaEnabled(String hakuOid) {
		if (isOPH()) {
			return true;
		}
		Date now = Calendar.getInstance().getTime();
		Date valintaesitysPvm = parseDate(parametrit.getValintaesitysPvm());
		return now.after(valintaesitysPvm);
	}

	private boolean isOPH() {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (authority.getAuthority().contains(rootOrganisaatioOid)) {
				return true;
			}
		}

		return false;
	}

	private Date parseDate(String source) {
		try {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(source);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
