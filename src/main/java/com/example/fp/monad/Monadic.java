package com.example.fp.monad;

import com.example.fp.common.LogHelper;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Monadic<W extends WitnessType<W>, T> extends WitnessType<W> {
    boolean isPresent();
    <R> Monadic<W, R> map(final Function<? super T, ? extends R> f);
    <R> Monadic<W, R> flatMap(final Function<? super T, ? extends Monadic<W, ? extends R>> f);
    // TODO, have to find appropriate way to avoid this kind of casting
    static <W extends WitnessType<W>, R> Monadic<W, R> cast(final Monadic<W, ? extends R> m) {
        return m.map(id -> id);
    }


    /*
     * Filterable
     */
    default Monadic<W, T> filter(final Predicate<? super T> cond, final Monadic<W, ? extends T> other) {
        return flatMap(t -> {
            try {
                if (cond.test(t)) {
                    return this;
                } else if (Objects.nonNull(other)) {
                    return other;
                } else {
                    LogHelper.logger(Monadic.class).warn("Failed to return other case, the other object is null");
                    return adapter().empty();
                }
            } catch (Exception e) {
                LogHelper.logger(Monadic.class).error("Failed to execute a condition predicate, ", e);
                return adapter().empty();
            }
        });
    }

    default Monadic<W, T> filter(final Predicate<? super T> cond) {
        return filter(cond, adapter().empty());
    }


    /*
     * Recoverable
     */
    default Monadic<W, T> orElse(final T other) {
        return isPresent() ? this : adapter().unit(other);
    }
    default Monadic<W, T> orElse(final Monadic<W, T> other) {
        return isPresent() ? this : other;
    }
    default Monadic<W, T> orElse(final Supplier<? extends T> other) {
        if (isPresent()) {
            return this;
        } else {
            try {
                return adapter().unit(other.get());
            } catch (Exception e) {
                LogHelper.logger(Monadic.class).error("Failed to run orElseGet, supplier has been failed, ", e);
                return adapter().empty();
            }
        }
    }
    default Monadic<W, T> orElseM(final Supplier<Monadic<W, T>> other) {
        if (isPresent()) {
            return this;
        } else {
            try {
                return other.get();
            } catch (Exception e) {
                LogHelper.logger(Monadic.class).error("Failed to run orElseGet, supplier has been failed, ", e);
                return adapter().empty();
            }
        }
    }


    /*
     * Tappable
     */
    default Monadic<W, T> peek(final boolean tf, final Runnable t, final Runnable f) {
        try {
            if (tf) {
                t.run();
            } else {
                f.run();
            }
        } catch(Exception e) {
            LogHelper.logger(Monadic.class).error("Failed to run peek runnable, ", e);
        }
        return this;
    }
    default Monadic<W, T> peek(final Runnable succeeded, final Runnable failed) {
        return peek(isPresent(), succeeded, failed);
    }
    default Monadic<W, T> peekOnSuccess(final Runnable succeeded) {
        return peek(isPresent(), succeeded, () -> {});
    }
    default Monadic<W, T> peekOnFailure(final Runnable failed) {
        return peek(isPresent(), () -> {}, failed);
    }

    default Monadic<W, T> peek(final boolean tf, final Consumer<T> t, final Consumer<T> f) {
        return flatMap(v -> {
            try {
                if (tf) {
                    t.accept(v);
                } else {
                    f.accept(v);
                }
                return this;
            } catch (Exception e) {
                LogHelper.logger(Monadic.class).error("Failed to run peek consumer, ", e);
                return this;
            }
        });
    }
    default Monadic<W, T> peek(final Consumer<T> succeeded, final Consumer<T> failed) {
        return peek(isPresent(), succeeded, failed);
    }
    default Monadic<W, T> peekOnSuccess(final Consumer<T> succeeded) {
        return peek(isPresent(), succeeded, (v) -> {});
    }
    default Monadic<W, T> peekOnFailure(final Consumer<T> failed) {
        return peek(isPresent(), (v) -> {}, failed);
    }
}