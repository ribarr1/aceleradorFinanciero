package com.greensqa.model;

public enum Op {
    EQ,           // =
    NEQ,          // <>
    IN,           // en lista (o)
    NIN,          // no en lista
    DATE_DIFF_LT, // diferencia de fechas <
    DATE_DIFF_GT, // diferencia de fechas >
    DATE_DIFF_EQ,  // diferencia de fechas =
    PICK_MIN_DATE,
    PICK_MAX_DATE,
    MONTHS_BETWEEN,
    DATE_DIFF_LTE,
    DATE_DIFF_GTE
}