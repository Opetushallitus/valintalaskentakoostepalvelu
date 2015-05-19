package fi.vm.sade.valinta.kooste.spec.valintalaskenta;

import com.google.common.collect.Lists;
import fi.vm.sade.valintalaskenta.domain.dto.OsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.*;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.*;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;

/**
 * @author Jussi Jartamo
 */
public class ValintalaskentaSpec {

    public static class HakemusOsallistuminenBuilder {
        private final HakemusOsallistuminenDTO hakemusOsallistuminen;
        public HakemusOsallistuminenBuilder() {
            this.hakemusOsallistuminen = new HakemusOsallistuminenDTO();
            this.hakemusOsallistuminen.setOsallistumiset(Lists.newArrayList());
        }
        public HakemusOsallistuminenBuilder setHakemusOid(String hakemusOid) {
            this.hakemusOsallistuminen.setHakemusOid(hakemusOid);
            return this;
        }
        public HakemusOsallistuminenBuilder setHakutoive(String hakutoiveOid) {
            this.hakemusOsallistuminen.setHakukohdeOid(hakutoiveOid);
            return this;
        }
        public HakemusOsallistuminenBuilder addOsallistuminen(String tunniste) {
            fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintakoeOsallistuminenDTO v = new fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintakoeOsallistuminenDTO();
            //v.setValintakoeOid(valintakoeOid);
            v.setValintakoeTunniste(tunniste);
            v.setOsallistuminen(OsallistuminenDTO.OSALLISTUU);
            this.hakemusOsallistuminen.getOsallistumiset().add(v);
            return this;
        }
        public HakemusOsallistuminenBuilder addOsallistuminen(String tunniste, OsallistuminenDTO o) {
            fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintakoeOsallistuminenDTO v = new fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintakoeOsallistuminenDTO();
            //v.setValintakoeOid(valintakoeOid);
            v.setValintakoeTunniste(tunniste);
            v.setOsallistuminen(o);
            this.hakemusOsallistuminen.getOsallistumiset().add(v);
            return this;
        }

        public HakemusOsallistuminenDTO build() {
            return hakemusOsallistuminen;
        }
    }

    public static class ValintakoeOsallistuminenBuilder {
        private final ValintakoeOsallistuminenDTO osallistuminenDTO = new ValintakoeOsallistuminenDTO();

        public static class HakutoiveBuilder {
            private final HakutoiveDTO hakutoive = new HakutoiveDTO();
            private final ValintakoeOsallistuminenBuilder builder;

            public static class ValinnanvaiheBuilder {
                private final ValintakoeValinnanvaiheDTO valinnanvaihe = new ValintakoeValinnanvaiheDTO();
                private final HakutoiveBuilder hbuilder;

                public static class ValintakoeBuilder {
                    private final ValintakoeDTO valintakoe = new ValintakoeDTO();
                    private final ValinnanvaiheBuilder vbuilder;
                    public ValintakoeBuilder(ValinnanvaiheBuilder vbuilder) {
                        this.vbuilder = vbuilder;
                    }
                    public ValintakoeBuilder setOsallistuu() {
                        OsallistuminenTulosDTO o = new OsallistuminenTulosDTO();
                        o.setOsallistuminen(Osallistuminen.OSALLISTUU);
                        valintakoe.setOsallistuminenTulos(o);
                        return this;
                    }
                    public ValintakoeBuilder setTunniste(String tunniste) {
                        valintakoe.setValintakoeTunniste(tunniste);
                        return this;
                    }
                    public ValintakoeBuilder setValintakoeOid(String valintakoeOid) {
                        valintakoe.setValintakoeOid(valintakoeOid);
                        return this;
                    }
                    public ValintakoeBuilder setValintakoeTunniste(String valintakoeTunniste) {
                        valintakoe.setValintakoeTunniste(valintakoeTunniste);
                        return this;
                    }
                    public ValintakoeBuilder setKutsutaankoKaikki(Boolean kutsutaankoKaikki) {
                        valintakoe.setKutsutaankoKaikki(kutsutaankoKaikki);
                        return this;
                    }
                    public ValinnanvaiheBuilder build() {
                        vbuilder.valinnanvaihe.getValintakokeet().add(valintakoe);
                        return vbuilder;
                    }
                }

                public ValinnanvaiheBuilder(HakutoiveBuilder hbuilder){
                    this.hbuilder = hbuilder;
                }
                public ValintakoeBuilder valintakoe() {
                    return new ValintakoeBuilder(this);
                }
                public HakutoiveBuilder build() {
                    hbuilder.hakutoive.getValinnanVaiheet().add(valinnanvaihe);
                    return hbuilder;
                }
            }

            public HakutoiveBuilder(ValintakoeOsallistuminenBuilder builder) {
                this.builder = builder;
            }
            public HakutoiveBuilder setHakukohdeOid(String hakukohdeOid) {
                hakutoive.setHakukohdeOid(hakukohdeOid);
                return this;
            }
            public ValintakoeOsallistuminenBuilder build() {
                builder.osallistuminenDTO.getHakutoiveet().add(hakutoive);
                return builder;
            }
            public ValinnanvaiheBuilder valinnanvaihe() {
                return new ValinnanvaiheBuilder(this);
            }
        }

        public HakutoiveBuilder hakutoive() {
            return new HakutoiveBuilder(this);
        }

        public ValintakoeOsallistuminenBuilder setHakemusOid(String hakemusOid) {
            osallistuminenDTO.setHakemusOid(hakemusOid);
            return this;
        }
        public ValintakoeOsallistuminenDTO build() {
            return osallistuminenDTO;
        }
    }

    public static ValintakoeOsallistuminenBuilder osallistuminen() {
        return new ValintakoeOsallistuminenBuilder();
    }

    public static HakemusOsallistuminenBuilder hakemusOsallistuminen() {
        return new HakemusOsallistuminenBuilder();
    }
}
