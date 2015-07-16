package fi.vm.sade.valinta.kooste.excel;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;

public class SoluLukija {
    private final List<Solu> solut;

    public SoluLukija(Collection<Solu> solut) {
        this.solut = Lists.newArrayList(solut);
    }

    public String getArvoAt(int index) {
        return StringUtils.trimToEmpty(get(index).toTeksti().getTeksti());
    }

    public Solu get(int index) {
        if (solut.size() <= index) {
            return Teksti.tyhja();
        } else {
            Solu solu = solut.get(index);
            if (solu == null) {
                return Teksti.tyhja();
            }
            return solu;
        }
    }

    public Collection<Solu> getSolut() {
        return solut;
    }
}
