package fi.vm.sade.valinta.kooste.excel.arvo;

import fi.vm.sade.valinta.kooste.excel.Numero;

@Deprecated
// use NumeroArvo
public class ArvoAlue extends Numero {

    private final boolean validi;

    public ArvoAlue(Number numero, double min, double max) {
        super(numero, true);
        if (numero == null) {
            validi = false;
        } else {
            double value = numero.doubleValue();
            if (min >= value && max <= value) {
                validi = true;
            } else {
                validi = false;
            }
        }
    }

    @Override
    protected boolean validoi() {
        return validi;
    }

}
