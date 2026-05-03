package com.buyon.core.resource;

public final class Resource<T> {
    public enum Status {
        LOADING,
        SUCCESS,
        ERROR
    }

    private final Status status;
    private final T data;
    private final Throwable error;

    private Resource(Status status, T data, Throwable error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }

    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null);
    }

    public static <T> Resource<T> success(T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    public static <T> Resource<T> error(Throwable error) {
        return new Resource<>(Status.ERROR, null, error);
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public Throwable getError() {
        return error;
    }
}
