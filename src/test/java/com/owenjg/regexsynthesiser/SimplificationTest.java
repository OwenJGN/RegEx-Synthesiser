package com.owenjg.regexsynthesiser;

import com.owenjg.regexsynthesiser.dfa.DFA;
import com.owenjg.regexsynthesiser.simplification.RegexGeneraliser;
import com.owenjg.regexsynthesiser.simplification.RegexSimplifier;
import com.owenjg.regexsynthesiser.simplification.StateEliminationAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimplificationTest {

    private RegexSimplifier simplifier;
    private StateEliminationAlgorithm eliminationAlgorithm;
    private RegexGeneraliser generaliser;

    @BeforeEach
    void setUp() {
        simplifier = new RegexSimplifier();
        eliminationAlgorithm = new StateEliminationAlgorithm();
        generaliser = new RegexGeneraliser();
    }

    @Test
    void testRegexSimplifierWithDuplicates() {
        // Test removing duplicates in alternation
        String regex = "(abc|def|abc|ghi)";
        String simplified = RegexSimplifier.simplify(regex);
        assertEquals("(abc|def|ghi)", simplified);

        // Test with escaped characters and bracket matching
        regex = "(\\(a\\)|\\(b\\)|\\(a\\))";
        simplified = RegexSimplifier.simplify(regex);
        assertEquals("(\\(a\\)|\\(b\\))", simplified);
    }

    @Test
    void testRegexSimplifierWithSinglePattern() {
        // Test simplification of single pattern
        String regex = "(abc)";
        String simplified = RegexSimplifier.simplify(regex);
        assertEquals("abc", simplified);

        // Test with already simplified pattern
        regex = "abc";
        simplified = RegexSimplifier.simplify(regex);
        assertEquals("abc", simplified);
    }

    @Test
    void testRegexSimplifierWithMultipleIdenticalPatterns() {
        // Test simplification of multiple identical patterns
        String regex = "(abc|abc|abc)";
        String simplified = RegexSimplifier.simplify(regex);
        assertEquals("abc", simplified);

        // Test with whitespace
        regex = " (abc|abc|abc) ";
        simplified = RegexSimplifier.simplify(regex);
        assertEquals("abc", simplified);
    }

    @Test
    void testRegexSimplifierWithNestedParentheses() {
        // Test with nested parentheses
        String regex = "((a|b)|(a|c))";
        String simplified = RegexSimplifier.simplify(regex);
        assertEquals("((a|b)|(a|c))", simplified);

        // More complex nested example
        regex = "(a(b|c)|a(b|d))";
        simplified = RegexSimplifier.simplify(regex);
        assertEquals("(a(b|c)|a(b|d))", simplified);
    }

    @Test
    void testRegexSimplifierWithCharacterClasses() {
        // Test with character classes
        String regex = "([a-z]|[0-9]|[a-z])";
        String simplified = RegexSimplifier.simplify(regex);
        assertEquals("([a-z]|[0-9])", simplified);

        // Test with escaped character in character class
        regex = "([\\]\\-]|[a-z]|[\\]\\-])";
        simplified = RegexSimplifier.simplify(regex);
        assertEquals("([\\]\\-]|[a-z])", simplified);
    }

    @Test
    void testStateElimination() {
        // Create a simple DFA for testing
        DFA dfa = new DFA(0);
        dfa.addTransition(0, 'a', 1);
        dfa.addTransition(1, 'b', 2);
        dfa.addAcceptingState(2);

        String regex = eliminationAlgorithm.eliminateStates(dfa);
        // The expected result is "ab"
        assertEquals("ab", regex);

        // Test DFA with alternation (a|b)
        DFA altDfa = new DFA(0);
        altDfa.addTransition(0, 'a', 1);
        altDfa.addTransition(0, 'b', 1);
        altDfa.addAcceptingState(1);

        regex = eliminationAlgorithm.eliminateStates(altDfa);
        // The expected result is either "(a|b)" or "[ab]"
        assertTrue(regex.equals("(a|b)") || regex.equals("[ab]"));
    }


    @Test
    void testRegexGeneraliser() {
        // Create a DFA to generalise
        DFA dfa = new DFA(0);
        dfa.addTransition(0, 'a', 1);
        dfa.addTransition(0, 'b', 1);
        dfa.addTransition(0, 'c', 1);
        dfa.addTransition(1, '1', 2);
        dfa.addTransition(1, '2', 2);
        dfa.addAcceptingState(2);

        DFA generalised = generaliser.generalizeDFA(dfa);

        // Check if the generalised DFA preserves accepting states
        assertTrue(generalised.isAcceptingState(2));
        assertFalse(generalised.isAcceptingState(0));
        assertFalse(generalised.isAcceptingState(1));

        assertEquals(0, generalised.getStartState());
    }
}