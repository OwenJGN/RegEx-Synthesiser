package com.owenjg.regexsynthesiser;

import com.owenjg.regexsynthesiser.dfa.DFA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DFATest {

    private DFA dfa;

    @BeforeEach
    void setUp() {
        // Initialize a fresh DFA for each test
        dfa = new DFA(0);
    }

    @Test
    void testConstructorAndBasicGetters() {
        // Test initial state is set correctly
        assertEquals(0, dfa.getStartState());

        // Test initial state is included in states
        assertTrue(dfa.getStates().contains(0));

        // Initial DFA should have 1 state (start state)
        assertEquals(1, dfa.getNumStates());

        // Initial alphabet should be empty
        assertTrue(dfa.getAlphabet().isEmpty());

        // Initial accepting states should be empty
        assertFalse(dfa.isAcceptingState(0));
    }

    @Test
    void testAcceptingStates() {
        // Add accepting states
        dfa.addAcceptingState(1);
        dfa.addAcceptingState(2);

        // Test accepting states are recognized
        assertTrue(dfa.isAcceptingState(1));
        assertTrue(dfa.isAcceptingState(2));
        assertFalse(dfa.isAcceptingState(0));

        // Remove accepting state
        dfa.removeAcceptingState(1);
        assertFalse(dfa.isAcceptingState(1));
        assertTrue(dfa.isAcceptingState(2));
    }

    @Test
    void testTransitions() {
        // Add transitions
        dfa.addTransition(0, 'a', 1);
        dfa.addTransition(0, 'b', 2);
        dfa.addTransition(1, 'a', 2);
        dfa.addTransition(2, 'b', 2);

        // Test transitions
        assertEquals(1, dfa.getTransition(0, 'a'));
        assertEquals(2, dfa.getTransition(0, 'b'));
        assertEquals(2, dfa.getTransition(1, 'a'));
        assertEquals(2, dfa.getTransition(2, 'b'));

        // Test invalid transitions return INVALID_STATE
        assertEquals(DFA.INVALID_STATE, dfa.getTransition(1, 'b'));
        assertEquals(DFA.INVALID_STATE, dfa.getTransition(2, 'a'));
        assertEquals(DFA.INVALID_STATE, dfa.getTransition(3, 'a')); // Non-existent state
    }

    @Test
    void testAlphabet() {
        dfa.addTransition(0, 'a', 1);
        dfa.addTransition(0, 'b', 2);
        dfa.addTransition(1, 'c', 2);

        Set<Character> alphabet = dfa.getAlphabet();
        assertEquals(3, alphabet.size());
        assertTrue(alphabet.contains('a'));
        assertTrue(alphabet.contains('b'));
        assertTrue(alphabet.contains('c'));
    }

    @Test
    void testGetStates() {
        dfa.addTransition(0, 'a', 1);
        dfa.addTransition(0, 'b', 2);
        dfa.addTransition(1, 'a', 3);
        dfa.addAcceptingState(3);

        Set<Integer> states = dfa.getStates();
        assertEquals(4, states.size());
        assertTrue(states.contains(0)); // Start state
        assertTrue(states.contains(1));
        assertTrue(states.contains(2));
        assertTrue(states.contains(3)); // Accepting state
    }

    @Test
    void testGetTransitions() {
        dfa.addTransition(0, 'a', 1);
        dfa.addTransition(0, 'b', 2);
        dfa.addTransition(1, 'a', 1);

        var transitions = dfa.getTransitions();
        assertEquals(2, transitions.size());
        assertTrue(transitions.containsKey(0));
        assertTrue(transitions.containsKey(1));

        assertEquals(1, transitions.get(0).get('a'));
        assertEquals(2, transitions.get(0).get('b'));
        assertEquals(1, transitions.get(1).get('a'));
    }

    @Test
    void testSetStartState() {
        assertEquals(0, dfa.getStartState());

        dfa.setStartState(5);
        assertEquals(5, dfa.getStartState());

        // Start state should be in states set
        assertTrue(dfa.getStates().contains(5));
    }
}