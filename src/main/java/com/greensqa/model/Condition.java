package com.greensqa.model;

import java.util.List;

public class Condition {
    public String leftVar;
    public Op op;
    public List<String> values;

    // Para condiciones compuestas
    public List<Condition> orConditions;
    public List<Condition> andConditions;

    public boolean isCompound() {
        return (orConditions != null && !orConditions.isEmpty()) ||
                (andConditions != null && !andConditions.isEmpty());
    }
}