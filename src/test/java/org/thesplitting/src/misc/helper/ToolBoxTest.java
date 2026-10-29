package org.thesplitting.src.misc.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToolBoxTest {
    @Test
    void formatEnumWord_upperCaseWord_returnsCapitalizedWord() {
        String input = "NOVICE";
        String result = ToolBox.formatEnumToWord(input);

        assertEquals("Novice", result);
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "NOVICE,    Novice",
            "prodigy,   Prodigy",
            "iNtElLeCtUaL,  Intellectual",
            "A, A",
            "WATER_FLOW, Water_flow"
    })
    void formatEnumToWord_VariousInputs_returnsFirstLetterCapitalized(String input, String expected) {
        assertEquals(expected, ToolBox.formatEnumToWord(input));
    }

    @Test
    void formatEnumToWord_emptyString_throwsException() {
        assertThrows(StringIndexOutOfBoundsException.class, () -> ToolBox.formatEnumToWord(""));
    }

    @Test
    void formatEnumToWord_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ToolBox.formatEnumToWord(null));
    }
}
