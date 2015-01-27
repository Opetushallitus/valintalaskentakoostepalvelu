package fi.vm.sade.valinta.kooste.kela.dto;

import java.util.Collection;
import java.util.Date;

/**
 *
 * @author Jussi Jartamo
 *
 */
public class KelaLuonti {
        private final String uuid;
        private final Collection<String> hakuOids;
        private final String aineistonNimi;
        private final String organisaationNimi;
        private final KelaCache cache; // (koodiService));
        private final KelaProsessi prosessi;
        private final Date alkuPvm;
        private final Date loppuPvm;

        private boolean kkHaku = false;

        public KelaLuonti() {
                this.hakuOids = null;
                this.aineistonNimi = null;
                this.organisaationNimi = null;
                this.uuid = null;
                this.cache = null;
                this.prosessi = null;
                this.alkuPvm = null;
                this.loppuPvm = null;
        }

        public KelaLuonti(String uuid, Collection<String> hakuOids,
                        String aineistonNimi, String organisaationNimi, KelaCache cache,
                        KelaProsessi prosessi) {
                this.uuid = uuid;
                this.hakuOids = hakuOids;
                this.aineistonNimi = aineistonNimi;
                this.organisaationNimi = organisaationNimi;
                this.cache = cache;
                this.prosessi = prosessi;
                this.alkuPvm = null;
                this.loppuPvm = null;
        }

        public KelaLuonti(String uuid, Collection<String> hakuOids,
                        String aineistonNimi, String organisaationNimi, KelaCache cache,
                        KelaProsessi prosessi, Date alkuPvm, Date loppuPvm) {
                this.uuid = uuid;
                this.hakuOids = hakuOids;
                this.aineistonNimi = aineistonNimi;
                this.organisaationNimi = organisaationNimi;
                this.cache = cache;
                this.prosessi = prosessi;
                this.alkuPvm = alkuPvm;
                this.loppuPvm = loppuPvm;
        }

        public void setKkHaku(boolean kkHaku) {
                this.kkHaku = kkHaku;
        }
    
        public boolean isKkHaku() {
                return kkHaku;
        }

        public KelaCache getCache() {
                return cache;
        }

        public KelaProsessi getProsessi() {
                return prosessi;
        }

        public String getUuid() {
                return uuid;
        }

        public String getAineistonNimi() {
                return aineistonNimi;
        }

        public Collection<String> getHakuOids() {
                return hakuOids;
        }

        public String getOrganisaationNimi() {
                return organisaationNimi;
        }

        public Date getAlkuPvm() {
                return alkuPvm;
        }

        public Date getLoppuPvm() {
                return loppuPvm;
        }
}
