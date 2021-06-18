package com.example.redisbloom.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * @author ZhuYX
 * @date 2021/05/24
 */
public class Iterators {

    public static <E> void forEach(Iterable<? extends E> elements,
                                   BiConsumer<Integer, ? super E> action) {
        Objects.requireNonNull(elements);
        Objects.requireNonNull(action);
        int index = 0;
        for (E element : elements) {
            action.accept(index++, element);
        }
    }

    public static <E, R> List<R> forEach(Iterable<? extends E> elements,
                                      BiFunction<Integer, ? super E, R> action) {
        Objects.requireNonNull(elements);
        Objects.requireNonNull(action);
        int index = 0;
        List<R> r = new ArrayList<>();
        for (E element : elements) {
            r.add(action.apply(index++, element));
        }
        return r.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

}
