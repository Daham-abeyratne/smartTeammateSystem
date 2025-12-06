package test.java.smartTeamMate.service;

import org.junit.jupiter.api.Test;
import smartTeamMate.service.ClassificationResult;
import smartTeamMate.service.PersonalityClassifier;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PersonalityClassifierTest {

    @Test
    void testLeaderClassification() {
        PersonalityClassifier classifier = new PersonalityClassifier();
        ClassificationResult result = classifier.typeClassifier(List.of(10,10,10));

        assertEquals("Leader", result.getClassificationType());
    }

    @Test
    void testThinkerClassification() {
        PersonalityClassifier classifier = new PersonalityClassifier();
        ClassificationResult result = classifier.typeClassifier(List.of(5,5,5));

        assertEquals("Thinker", result.getClassificationType());
    }
}
