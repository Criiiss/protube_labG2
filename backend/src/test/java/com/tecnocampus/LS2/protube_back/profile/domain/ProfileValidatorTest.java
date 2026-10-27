package com.tecnocampus.LS2.protube_back.profile.domain;

import com.tecnocampus.LS2.protube_back.profile.domain.exception.InvalidUsernameException;
import com.tecnocampus.LS2.protube_back.profile.domain.service.ProfileValidator;
import org.junit.jupiter.api.Test;
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

    @Test
    public void rejectsEmptyUsername() {
        assertThrows(InvalidUsernameException.class,() -> validator.normalizeUsername(""));
    }

    @Test
    public void acceptsUsernameWithMinimumLength() {
        assertEquals("abc", validator.normalizeUsername("abc"));
    }

    @Test
    public void acceptsUsernameWithMaximumLength() {
        String username = "a".repeat(20);
        assertEquals(username, validator.normalizeUsername(username));
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
    @Test
    public void rejectsNullDisplayName() {
        assertThrows(IllegalArgumentException.class,() -> validator.validateDisplayName(null));
    }

    @Test
    public void rejectsDisplayNameOverMaximumLength() {
        String displayName = "a".repeat(51);
        assertThrows(IllegalArgumentException.class,() -> validator.validateDisplayName(displayName));
    }

    @Test
    public void acceptsDisplayNameWithMaximumLength() {
        String displayName = "a".repeat(50);
        assertEquals(displayName, validator.validateDisplayName(displayName));
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

    @Test
    public void trimsBiography() {
        assertEquals("My biography",validator.validateBiography("  My biography  "));
    }

    @Test
    public void acceptsBiographyWithMaximumLength() {
        String biography = "a".repeat(250);
        assertEquals(biography, validator.validateBiography(biography));
    }



}
