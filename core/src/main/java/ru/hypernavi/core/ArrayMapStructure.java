package ru.hypernavi.core;

import org.jetbrains.annotations.NotNull;


import java.util.*;


import javafx.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.hypernavi.commons.Hypermarket;
import ru.hypernavi.commons.Positioned;
import ru.hypernavi.util.GeoPoint;

/**
 * Created by Константин on 14.08.2015.
 */
public class ArrayMapStructure<T extends Positioned> implements MapStructure<T> {
    private static final Log LOG = LogFactory.getLog(ArrayMapStructure.class);
    private final List<T> listPoints = new ArrayList<>();

    ArrayMapStructure(final T[] points) {
        Collections.addAll(listPoints, points);
    }

    @NotNull
    @Override
    public List<T> find(final GeoPoint position, final int number) {
        final int k = number > size() ? size() : number;
        final List<T> result = new ArrayList<>();

        final double[] distance = new double[listPoints.size()];
        for (int i = 0; i < listPoints.size(); ++i) {
            distance[i] = GeoPoint.distance(position, listPoints.get(i).getLocation());
        }
        final List<Pair<Double, Integer>> sorted = new ArrayList<>();
        for (int i = 0; i < listPoints.size(); ++i) {
            sorted.add(new Pair<>(distance[i], i));
        }
        Collections.sort(sorted, Comparator.comparing(Pair::getKey));

        for (int i = 0; i < k; ++i) {
            result.add(listPoints.get(sorted.get(i).getValue()));
        }
        return result;
    }

    @Override
    public int size() {
        return listPoints.size();
    }

    @Override
    public void add(@NotNull final T hyper) {
        listPoints.add(hyper);
    }
}
