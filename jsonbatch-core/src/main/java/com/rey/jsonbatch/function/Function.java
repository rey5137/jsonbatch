package com.rey.jsonbatch.function;

import com.rey.jsonbatch.JsonBuilder.Type;

import java.util.List;

public abstract class Function {

    public abstract String getName();

    public abstract boolean isReduceFunction();

    public Object invoke(Type type, List<Object> arguments) {
        return null;
    }

    public Result handle(Type type, Object argument, Result prevResult) {
        return null;
    }

    public static class Result<T> {

        T value;
        boolean isDone;

        public Result(T value, boolean isDone) {
            this.value = value;
            this.isDone = isDone;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public boolean isDone() {
            return isDone;
        }

        public void setDone(boolean done) {
            isDone = done;
        }

        public static <T> Result<T> of(T value, boolean isDone) {
            return new Result<>(value, isDone);
        }

    }

}
