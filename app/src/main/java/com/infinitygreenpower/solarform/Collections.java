package com.infinitygreenpower.solarform;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

final class Collections {
    private Collections() {}

    static <E> HashSet<E> newSetFromMap(Map<E, Boolean> map) {
        return new HashSet<E>();
    }

    static <T extends Comparable<? super T>> void sort(List<T> list) {
        java.util.Collections.sort(list);
    }

    static <T> void sort(List<T> list, Comparator<? super T> comparator) {
        java.util.Collections.sort(list, comparator);
    }
}
