package fi.vm.sade.valinta.kooste.excel;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;

public class SoluLukija {
    private final List<Solu> solut;
    private final boolean palautaTyhjaNullArvona;

    public SoluLukija(boolean palautaTyhjaNullArvona, Collection<Solu> solut) {
        this.solut = Lists.newArrayList(solut);
        this.palautaTyhjaNullArvona = palautaTyhjaNullArvona;
    }

    public String getArvoAt(int index) {
        Solu solu = get(index);
        String arvo = solu.toTeksti().getTeksti();
        if (StringUtils.isBlank(arvo)) {
            return tyhja();
        } else {
            return arvo;
        }
    }

    private String tyhja() {
        if (palautaTyhjaNullArvona) {
            return null;
        } else {
            return StringUtils.EMPTY;
        }
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

    public boolean isPalautaTyhjaNullArvona() {
        return palautaTyhjaNullArvona;
    }

    public Collection<Solu> getSolut() {
        return solut;
    }
}
