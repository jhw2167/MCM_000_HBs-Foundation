package com.holybuckets.foundation.util;

import net.minecraft.core.Holder;

import java.util.function.Supplier;

public class DeferredObject<T> implements Supplier<T> {
    private final Supplier<T> supplier;

    public DeferredObject(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static <T> DeferredObject<T> of(Holder<T> holder) {
        return new DeferredObject<>(holder::value);
    }

    @Override
    public T get() {
        return supplier.get();
    }
}
