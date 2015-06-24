package fi.vm.sade.valinta.kooste.exception;

import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;

import java.util.Collection;

public class ErillishaunDataException extends RuntimeException {
    private final Collection<PoikkeusRivi> poikkeusRivit;

    public ErillishaunDataException(Collection<PoikkeusRivi> poikkeusRivit) {
        super(formatoi(poikkeusRivit));
        this.poikkeusRivit = poikkeusRivit;
    }

    public static String formatoi(Collection<PoikkeusRivi> poikkeusRivit) {
        StringBuilder sb = new StringBuilder();
        sb.append("Erillishaun datassa oli virheitä! \r\n");
        for(PoikkeusRivi rivi : poikkeusRivit) {
            sb.append(rivi).append("\r\n");
        }
        return sb.toString();
    }

    public Collection<PoikkeusRivi> getPoikkeusRivit() {
        return poikkeusRivit;
    }

    public static class PoikkeusRivi {
        private final int indeksi;
        private final String selite;

        public PoikkeusRivi() {
            this.indeksi = 0;
            this.selite = null;
        }
        public PoikkeusRivi(int indeksi, String selite) {
            this.indeksi = indeksi;
            this.selite = selite;
        }

        public int getIndeksi() {
            return indeksi;
        }

        public String getSelite() {
            return selite;
        }

        public String toString() {
            return "Rivi " + indeksi + ": " + selite;
        }
    }
}
