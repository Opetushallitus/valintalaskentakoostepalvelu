package fi.vm.sade.valinta.kooste.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Memoizer {

    private static <T, U> Function<T, U> doMemoize(final Function<T, U> function, int time, TimeUnit timeunit) {
        Cache<T, U> cache = CacheBuilder.<T, U>newBuilder().expireAfterWrite(time, timeunit).build();
        return input -> {
            try {
                return cache.get(input, () -> function.apply(input));
            } catch (ExecutionException e) {
                return cache.getIfPresent(input);
            }
        };
    }

    public static <T, U> Function<T, U> memoize(final Function<T, U> function, int time, TimeUnit timeunit) {
        return doMemoize(function, time, timeunit);
    }
}