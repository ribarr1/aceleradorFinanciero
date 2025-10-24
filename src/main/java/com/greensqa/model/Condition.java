package com.greensqa.model;

import java.util.List;

public class Condition {
    public enum Op { EQ, NEQ, IN, NIN, DATE_DIFF_LT, DATE_DIFF_GT, DATE_DIFF_EQ }
    public String leftVar;    // nombre de la variable JSON (o par fecha)
    public Op op;
    public List<String> values; // para =, <>, IN/NOT IN
    // Para DATE_DIFF_* interpretamos values: [fechaVar1, fechaVar2, "12 meses"]
}