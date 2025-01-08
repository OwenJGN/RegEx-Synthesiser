package com.owenjg.regexsynthesiser.dfa;

import java.util.Objects;

public class DFAState {
    private final int id;
    private boolean accepting;

    public DFAState(int id) {
        this.id = id;
        this.accepting = false;
    }

    public void setAccepting(boolean accepting) {
        this.accepting = accepting;
    }

    public boolean isAccepting() {
        return accepting;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DFAState state = (DFAState) o;
        return id == state.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
