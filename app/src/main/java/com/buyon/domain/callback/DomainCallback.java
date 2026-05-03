package com.buyon.domain.callback;

public interface DomainCallback<T> {
    void onSuccess(T result);

    void onError(Throwable error);
}
