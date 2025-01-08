package com.owenjg.regexsynthesiser.dfa;

import java.util.Objects;

public class DFATransition {
    private final DFAState source;
    private final DFAState destination;
    private final char symbol;

    public DFATransition(DFAState source, DFAState destination, char symbol) {
        this.source = source;
        this.destination = destination;
        this.symbol = symbol;
    }

    public DFAState getSource() {
        return source;
    }

    public DFAState getDestination() {
        return destination;
    }

    public char getSymbol() {
        return symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DFATransition that = (DFATransition) o;
        return symbol == that.symbol &&
                Objects.equals(source, that.source) &&
                Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination, symbol);
    }
}