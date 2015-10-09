package fi.vm.sade.valinta.kooste.parametrit;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuaikaV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.parametrit.dto.ParametritUIDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ParametritParser {

    private static final Logger LOG = LoggerFactory.getLogger(ParametritParser.class);

    private ParametritDTO parametrit;
    private HakuV1RDTO haku;
    private String rootOrganisaatioOid;

    public ParametritParser(ParametritDTO parametrit,HakuV1RDTO haku, String rootOrganisaatioOid) {
        this.parametrit = parametrit;
        this.haku = haku;
        this.rootOrganisaatioOid = rootOrganisaatioOid;
    }

    public boolean pistesyottoEnabled() {
        if (isOPH()) {
            return true;
        }
        Date now = Calendar.getInstance().getTime();
        ParametriDTO pdto = parametrit.getPH_KTT();
        Date koetuloksetAlkupvm = pdto.getDateStart();
        Date koetuloksetLoppupvm =  pdto.getDateEnd();
        return now.before(koetuloksetLoppupvm) && now.after(koetuloksetAlkupvm);
    }

    public boolean hakeneetEnabled() {
        if (isOPH()) {
            return true;
        }
        Date now = Calendar.getInstance().getTime();
        if(this.haku.getHakuaikas().size() > 0 && this.haku.getHakuaikas().get(0).getAlkuPvm() != null) {
            return now.after(this.haku.getHakuaikas().get(0).getAlkuPvm());
        } else {
            return false;
        }
    }

    public boolean harkinnanvaraisetEnabled() {
        if (isOPH()) {
            return true;
        }
        //TODO
        return false;
/*        Date now = Calendar.getInstance().getTime();
        Date hakuLoppupvm = parseDate(parametrit.getHakuLoppupvm());
        Date koetuloksetPvm = parseDate(parametrit.getKoetuloksetLoppupvm());
        return now.after(hakuLoppupvm) && now.before(koetuloksetPvm);
*/
    }

    public boolean valintakoekutsutEnabled() {
        if (isOPH()) {
            return true;
        }
        Date now = Calendar.getInstance().getTime();

        ParametriDTO koekutsujenmuodostaminen = parametrit.getPH_KKM();
        if(koekutsujenmuodostaminen == null || koekutsujenmuodostaminen.getDateStart() == null) {
            return false;
        }
        return now.after(koekutsujenmuodostaminen.getDateStart());
    }

    public boolean valintalaskentaEnabled() {
        // Date now = Calendar.getInstance().getTime();
        // Date hakuAlkupvm = new Date(parametrit.getHakuAlkupvm());
        return isOPH(); // now.after(hakuAlkupvm) && isOPH();
    }

    public boolean hakijaryhmatEnabled() {
        if (isOPH()) {
            return true;
        }
        Date now = Calendar.getInstance().getTime();
        if(this.haku.getHakuaikas().size() > 0 && this.haku.getHakuaikas().get(0).getAlkuPvm() != null) {
            return now.after(this.haku.getHakuaikas().get(0).getAlkuPvm());
        } else {
            return false;
        }
    }

    public boolean valinnanhallintaEnabled() {
        if (isOPH()) {
            return true;
        }
        return false;
        /*
        Date now = Calendar.getInstance().getTime();
        Date valintaesitysPvm = parseDate(parametrit.getValintaesitysPvm());
        return now.after(valintaesitysPvm);
        */
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
