package com.example.board.validation;

import com.example.board.exception.BadRequestException;
import com.example.board.exception.errorMessage.AuthErrorMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordValidatorTest {

    @Test
    @DisplayName("비밀번호가 8자 미만이면 검증에 실패한다.")
    void TestshortPasswordException() {
        //given
        String password = "aaaa";
        //when
        //then
        Exception exception = assertThrows(BadRequestException.class, () -> PasswordValidator.validate(password));
        assertThat(exception.getMessage()).isEqualTo(AuthErrorMessage.PASSWORD_LENGTH_LIMIT);
    }

    @Test
    @DisplayName("비밀번호가 20자를 초과하면 검증에 실패한다.")
    void TestLongPasswordException() {
        //given
        String password = "aaaaaaaaaaaaaaaaaaaaaaa";    //20자 이상
        //when
        //then
        Exception exception = assertThrows(BadRequestException.class, () -> PasswordValidator.validate(password));
        assertThat(exception.getMessage()).isEqualTo(AuthErrorMessage.PASSWORD_LENGTH_LIMIT);
    }

    @Test
    @DisplayName("비밀번호가 null이면 검증에 실패한다.")
    void testNullPassword(){
        //given
        String password = null;
        //when
        //then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> PasswordValidator.validate(password));
        assertThat(exception.getMessage()).isEqualTo(AuthErrorMessage.PASSWORD_REQUIRED);
    }

    @Test
    @DisplayName("비밀번호에 영문 대문자가 없으면 검증에 실패한다.")
    void testNoUpperCasePassword() {
        //given
        String password = "asdasda12!";
        //when
        //then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> PasswordValidator.validate(password));
        assertThat(exception.getMessage()).isEqualTo(AuthErrorMessage.PASSWORD_MUST_CONTAIN_UPPERCASE);
    }

    @Test
    @DisplayName("비밀번호에 영문 소문자가 없으면 검증에 실패한다.")
    void testNoLowerCasePassword(){
        //given
        String password = "AAAAAAAA12!";
        //when
        //then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> PasswordValidator.validate(password));
        assertThat(exception.getMessage()).isEqualTo(AuthErrorMessage.PASSWORD_MUST_CONTAIN_LOWERCASE);
    }

    @Test
    @DisplayName("비밀번호에 공백이 포함되면 검증에 실패한다.")
    void testPasswordContainsBlank(){
        //given
        String password = "AAAA AAAA12!";
        //when
        //then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> PasswordValidator.validate(password));
        assertThat(exception.getMessage()).isEqualTo(AuthErrorMessage.PASSWORD_CANNOT_CONTAINS_BLANK);
    }

    @Test
    @DisplayName("비밀번호에 특수문자가 없으면 검증에 실패한다.")
    void testNoSpecialLetterPassword(){
        //given
        String password = "Apple122";
        //when
        //then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> PasswordValidator.validate(password));
        assertThat(exception.getMessage()).isEqualTo(AuthErrorMessage.PASSWORD_MUST_CONTAIN_SPECIAL_LETTER);
    }

    @Test
    @DisplayName("비밀번호에 숫자가 없으면 검증에 실패한다.")
    void testNoNumberPassword(){
        //given
        String password = "Ilikeapple!";
        //when
        //then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> PasswordValidator.validate(password));
        assertThat(exception.getMessage()).isEqualTo(AuthErrorMessage.PASSWORD_MUST_CONTAIN_NUMBER);
    }

    @Test
    @DisplayName("비밀번호가 모든 조건을 만족하면 검증에 성공한다.")
    void testValidPassword(){
        //given
        String password = "Ilikeapple12!";
        //when
        Boolean isValidPassword = PasswordValidator.isValid(password);
        //then
        assertThat(isValidPassword).isEqualTo(true);
    }
}
