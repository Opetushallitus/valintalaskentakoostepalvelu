package fi.vm.sade.valinta.kooste.spec.valintalaskenta;

import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.*;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;

/**
 * @author Jussi Jartamo
 */
public class ValintalaskentaSpec {

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
                    public ValintakoeBuilder setValintakoeOid(String valintakoeOid) {
                        valintakoe.setValintakoeOid(valintakoeOid);
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
}
