package fi.vm.sade.valinta.kooste.mocks;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import fi.vm.sade.koodisto.service.GenericFault;
import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.SearchKoodisByKoodistoCriteriaType;
import fi.vm.sade.koodisto.service.types.SearchKoodisCriteriaType;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.koodisto.service.types.common.KoodiUriAndVersioType;
import fi.vm.sade.koodisto.service.types.common.SuhteenTyyppiType;

@Service
public class MockKoodiservice implements KoodiService {
    @Override
    public List<KoodiType> listKoodiByRelation(final KoodiUriAndVersioType koodiUriAndVersioType, final boolean b, final SuhteenTyyppiType suhteenTyyppiType) throws GenericFault {
        return Arrays.asList();
    }

    @Override
    public List<KoodiType> searchKoodisByKoodisto(final SearchKoodisByKoodistoCriteriaType searchKoodisByKoodistoCriteriaType) throws GenericFault {
        return Arrays.asList();
    }

    @Override
    public List<KoodiType> searchKoodis(final SearchKoodisCriteriaType searchKoodisCriteriaType) throws GenericFault {
        return Arrays.asList();
    }
}
