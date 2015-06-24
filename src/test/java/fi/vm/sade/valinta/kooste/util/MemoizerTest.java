package fi.vm.sade.valinta.kooste.util;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MemoizerTest {

    @Test
    public void testMemoize() throws Exception {

        Function<Integer, Integer> calculation = x -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            return x * 2;
        };

        Function<Integer, Integer> memoized = Memoizer.memoize(calculation, 12, TimeUnit.HOURS);
        long start = System.currentTimeMillis();
        Integer res1 = memoized.apply(1);
        long firstRes = System.currentTimeMillis();
        Integer res2 = memoized.apply(1);
        long secondRes = System.currentTimeMillis();

        assertAllEqual(2, res1, res2);
        assertTrue(firstRes - start > 400);
        assertTrue(secondRes - firstRes < 100);
    }
    private void assertAllEqual(Integer expected, Integer... actual) {
        for (Integer integer : actual) {
            assertEquals(expected, integer);
        }
    }
}
