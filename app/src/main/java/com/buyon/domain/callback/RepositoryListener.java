package com.buyon.domain.callback;

public interface RepositoryListener<T> {
    void onData(T data);

    void onError(Throwable error);
}
