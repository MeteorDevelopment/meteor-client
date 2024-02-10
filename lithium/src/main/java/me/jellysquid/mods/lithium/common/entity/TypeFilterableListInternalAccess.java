package me.jellysquid.mods.lithium.common.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface TypeFilterableListInternalAccess<T> {
    <S extends T> List<S> lithium$getOrCreateAllOfTypeRaw(Class<S> type);

    <S extends T> List<S> lithium$replaceCollectionAndGet(Class<S> type, Function<ArrayList<S>, List<S>> listCtor);
    <S extends T> List<S> lithium$replaceCollectionAndGet(Class<S> type, ArrayList<S> list);
}
