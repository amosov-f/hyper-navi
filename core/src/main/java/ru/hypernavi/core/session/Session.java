package ru.hypernavi.core.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


import com.google.inject.ImplementedBy;

/**
 * Created by amosov-f on 21.11.15.
 */
@SuppressWarnings("ClassReferencesSubclass")
@ImplementedBy(SessionImpl.class)
public interface Session {
    <T> void set(@NotNull final Property<? super T> property, @NotNull final T value);

    default <T> void setIfNotNull(@NotNull final Property<? super T> property, @Nullable final T value) {
        if (value != null) {
            set(property, value);
        }
    }

    @Nullable
    <T> T get(@NotNull final Property<T> property);

    @NotNull
    default <T> T demand(@NotNull final Property<T> property) {
        return Objects.requireNonNull(get(property), "There is no '" + property + "' property in session!");
    }

    default boolean has(@NotNull final Property<?> property) {
        return get(property) != null;
    }
}
