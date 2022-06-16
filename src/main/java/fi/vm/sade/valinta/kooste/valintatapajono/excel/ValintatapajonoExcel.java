package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.RiviBuilder;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;
import fi.vm.sade.valinta.kooste.excel.arvo.MonivalintaArvo;
import fi.vm.sade.valinta.kooste.excel.arvo.NumeroArvo;
import fi.vm.sade.valinta.kooste.excel.arvo.TekstiArvo;
import fi.vm.sade.valinta.kooste.util.*;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valintalaskenta.domain.dto.JonosijaDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValintatapajonoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import java.math.BigDecimal;
import java.util.*;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValintatapajonoExcel {
  private static final Logger LOG = LoggerFactory.getLogger(ValintatapajonoExcel.class);

  public static final String MAARITTELEMATON = "Määrittelemätön";
  public static final String HYVAKSYTTAVISSA = "Hyväksyttävissä";
  public static final String HYLATTY = "Hylätty";
  public static final String HYVAKSYTTY_HARKINNANVARAISESTI = "Hyväksytty harkinnanvaraisesti";

  public static final String VAKIO_MAARITTELEMATON = "MAARITTELEMATON";
  public static final String VAKIO_HYVAKSYTTAVISSA = "HYVAKSYTTAVISSA";
  public static final String VAKIO_HYLATTY = "HYLATTY";
  public static final String VAKIO_HYVAKSYTTY_HARKINNANVARAISESTI = "HYVAKSYTTY_HARKINNANVARAISESTI";

  private static final Collection<String> VAIHTOEHDOT = Arrays.asList(MAARITTELEMATON, HYVAKSYTTAVISSA, HYLATTY);
  public static final Map<String, String> VAIHTOEHDOT_KONVERSIO = new KonversioBuilder()
      .addKonversio(StringUtils.EMPTY, MAARITTELEMATON).addKonversio(VAKIO_MAARITTELEMATON, MAARITTELEMATON)
      .addKonversio(VAKIO_HYVAKSYTTAVISSA, HYVAKSYTTAVISSA).addKonversio(VAKIO_HYLATTY, HYLATTY)
      .addKonversio(VAKIO_HYVAKSYTTY_HARKINNANVARAISESTI, HYVAKSYTTY_HARKINNANVARAISESTI).build();
  public static final Map<String, String> VAIHTOEHDOT_TAKAISINPAIN_KONVERSIO = new KonversioBuilder()
      .addKonversio(StringUtils.EMPTY, VAKIO_MAARITTELEMATON).addKonversio(MAARITTELEMATON, VAKIO_MAARITTELEMATON)
      .addKonversio(HYVAKSYTTAVISSA, VAKIO_HYVAKSYTTAVISSA).addKonversio(HYLATTY, VAKIO_HYLATTY)
      .addKonversio(HYVAKSYTTY_HARKINNANVARAISESTI, VAKIO_HYVAKSYTTY_HARKINNANVARAISESTI).build();

  private final Excel excel;

  public ValintatapajonoExcel(String hakuOid, String hakukohdeOid, String valintatapajonoOid, String hakuNimi,
      String hakukohdeNimi, List<ValintatietoValinnanvaiheDTO> valinnanvaihe, List<HakemusWrapper> hakemukset) {
    this(hakuOid, hakukohdeOid, valintatapajonoOid, hakuNimi, hakukohdeNimi, valinnanvaihe, hakemukset,
        Collections.<ValintatapajonoDataRiviKuuntelija>emptyList());
  }

  @SuppressWarnings("unchecked")
  public ValintatapajonoExcel(String hakuOid, String hakukohdeOid, String valintatapajonoOid, String hakuNimi,
      String hakukohdeNimi, List<ValintatietoValinnanvaiheDTO> valinnanvaihe, List<HakemusWrapper> hakemukset,
      Collection<? extends ValintatapajonoDataRiviKuuntelija> kuuntelijat) {
    // Jonosija (13) Hakija Valintatieto Kuvaus (FI) Kuvaus (SV) Kuvaus (EN)
    List<Rivi> rivit = Lists.newArrayList();
    rivit.add(new RiviBuilder().addOid(hakuOid).addTeksti(hakuNimi, 4).build());
    rivit.add(new RiviBuilder().addOid(hakukohdeOid).addTeksti(hakukohdeNimi, 4).build());
    rivit.add(Rivi.tyhjaRivi());

    final RiviBuilder otsikkoRiviBuilder = new RiviBuilder().addKeskitettyTeksti("Hakemus OID")
        .addKeskitettyTeksti("Jonosija (" + hakemukset.size() + ")").addKeskitettyTeksti("Hakija")
        .addKeskitettyTeksti("Valintatieto").addKeskitettyTeksti("Kokonaispisteet")
        .addKeskitettyTeksti("Kuvaus (FI)").addKeskitettyTeksti("Kuvaus (SV)")
        .addKeskitettyTeksti("Kuvaus (EN)");

    rivit.add(otsikkoRiviBuilder.build());

    final Map<String, Integer> jonosijat = Maps.newHashMap();
    final Map<String, String> valintatiedot = Maps.newHashMap();
    final Map<String, Map<String, String>> avaimet = Maps.newHashMap();
    final Map<String, BigDecimal> kokonaispisteet = Maps.newHashMap();
    for (ValintatietoValinnanvaiheDTO vaihe : valinnanvaihe) {
      for (ValintatapajonoDTO jono : vaihe.getValintatapajonot()) {
        if (valintatapajonoOid.equals(jono.getOid())) {
          for (JonosijaDTO jonosija : jono.getJonosijat()) {
            String hakemusOid = jonosija.getHakemusOid();
            jonosijat.put(hakemusOid, jonosija.getJonosija());
            valintatiedot.put(hakemusOid, jonosija.getTuloksenTila().toString());
            if (!jonosija.getJarjestyskriteerit().isEmpty()) {
              avaimet.put(hakemusOid, jonosija.getJarjestyskriteerit().last().getKuvaus());
              if (null != jono.getKaytetaanKokonaispisteita() && jono.getKaytetaanKokonaispisteita()) {
                kokonaispisteet.put(hakemusOid, jonosija.getJarjestyskriteerit().last().getArvo());
              }
            }
          }
        }
      }
    }
    ComparatorChain jonosijaAndHakijaNameComparator = new ComparatorChain((Comparator<HakemusWrapper>) (o1, o2) -> {
      Integer i1 = jonosijat.get(o1.getOid());
      Integer i2 = jonosijat.get(o2.getOid());
      if (i1 == null) {
        i1 = Integer.MAX_VALUE;
      }
      if (i2 == null) {
        i2 = Integer.MAX_VALUE;
      }
      return i1.compareTo(i2);
    });
    jonosijaAndHakijaNameComparator.addComparator(HakemusComparator.DEFAULT);
    hakemukset.sort(jonosijaAndHakijaNameComparator);

    Collection<Collection<Arvo>> sx = Lists.newArrayList();
    for (HakemusWrapper hakemus : hakemukset) {
      String hakemusOid = hakemus.getOid();
      Collection<Arvo> s = Lists.newArrayList();
      s.add(new TekstiArvo(hakemusOid));

      if (!kokonaispisteet.containsKey(hakemusOid)) {
        s.add(new NumeroArvo(jonosijat.get(hakemusOid), 0, hakemukset.size()));
      } else {
        s.add(new NumeroArvo(null, 0, hakemukset.size()));
      }
      Osoite osoite = OsoiteHakemukseltaUtil.osoiteHakemuksesta(hakemus, Maps.newHashMap(), Maps.newHashMap(),
          new NimiPaattelyStrategy());
      s.add(new TekstiArvo(osoite.getLastName() + " " + osoite.getFirstName()));
      s.add(new MonivalintaArvo(VAIHTOEHDOT_KONVERSIO.get(StringUtils.trimToEmpty(valintatiedot.get(hakemusOid))),
          VAIHTOEHDOT));
      if (kokonaispisteet.containsKey(hakemusOid) && null != kokonaispisteet.get(hakemusOid)) {
        s.add(new NumeroArvo(kokonaispisteet.get(hakemusOid)));
      } else {
        s.add(new NumeroArvo(null));
      }
      if (avaimet.containsKey(hakemusOid)) {
        Map<String, String> a = avaimet.get(hakemusOid);
        s.add(new TekstiArvo(StringUtils.trimToEmpty(a.get("FI")), false, true));
        s.add(new TekstiArvo(StringUtils.trimToEmpty(a.get("SV")), false, true));
        s.add(new TekstiArvo(StringUtils.trimToEmpty(a.get("EN")), false, true));
      } else {
        s.add(new TekstiArvo(StringUtils.EMPTY, false, true));
        s.add(new TekstiArvo(StringUtils.EMPTY, false, true));
        s.add(new TekstiArvo(StringUtils.EMPTY, false, true));
      }
      sx.add(s);
    }
    rivit.add(new ValintatapajonoDataRivi(sx, kuuntelijat));
    this.excel = new Excel("Valintatapajono", rivit, new int[] { 0 } // piilottaa ensimmaisen pysty sarakkeen
        , new int[] {});
  }

  public Excel getExcel() {
    return excel;
  }
}
