package com.tecnocampus.LS2.protube_back.profile.domain;

import com.tecnocampus.LS2.protube_back.profile.domain.exception.InvalidUsernameException;
import com.tecnocampus.LS2.protube_back.profile.domain.service.ProfileValidator;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProfileValidatorTest {

    private final ProfileValidator validator = new ProfileValidator();

    // Test method normalizeUsername
    @Test
    public void normalizesUsernameToLowerCase(){
        assertEquals("maria_23", validator.normalizeUsername(" Maria_23 "));

    }
    @Test
    public void rejectsNullUsername() {
        assertThrows(InvalidUsernameException.class,() -> validator.normalizeUsername(null));
    }
    @Test
    public void rejectsUsernameWithInvalidCharacters() {
        assertThrows(InvalidUsernameException.class,() -> validator.normalizeUsername("maria-23"));
    }

    @Test
    public void rejectsUsernameTooShort() {
        assertThrows(InvalidUsernameException.class, () -> validator.normalizeUsername("ab"));
    }

    @Test
    public void rejectsUsernameTooLong() {
        assertThrows(InvalidUsernameException.class,() -> validator.normalizeUsername("this_username_is_too_long"));
    }

    // Test method validateDisplayName
    @Test
    public void validatesDisplayName() {
        assertEquals("Maria",validator.validateDisplayName(" Maria "));
    }

    @Test
    public void rejectsBlankDisplayName() {
        assertThrows(IllegalArgumentException.class,() -> validator.validateDisplayName(" "));
    }

    //Test method validateBiography
    @Test
    public void validatesNullBiographyAsEmptyOptionalValue() {
        assertNull(validator.validateBiography(null));
    }

    @Test
    public void rejectsBiographyOverMaximumLength() {
        String biography = "a".repeat(251);
        assertThrows(IllegalArgumentException.class,() -> validator.validateBiography(biography));
    }


}
