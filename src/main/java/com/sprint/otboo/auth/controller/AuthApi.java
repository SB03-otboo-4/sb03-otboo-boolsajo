package com.sprint.otboo.auth.controller;

import com.sprint.otboo.auth.dto.JwtDto;
import com.sprint.otboo.auth.dto.ResetPasswordRequest;
import com.sprint.otboo.auth.dto.SignInRequest;
import com.sprint.otboo.common.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.text.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;

@Tag(name = "인증 관리", description = "인증 관련 API")
public interface AuthApi {

    @Operation(summary = "CSRF 토큰 조회", description = "CSRF 토큰을 조회합니다. 토큰은 쿠키(XSRF-TOKEN)에 저장됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "CSRF 토큰 조회 성공"),
        @ApiResponse(responseCode = "400", description = "CSRF 토큰 조회 실패")
    })
    ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken);

    @Operation(
        summary = "로그인",
        description = "이메일과 비밀번호를 입력하여 로그인합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = JwtDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "로그인 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @RequestBody(
        required = true,
        content = @Content(mediaType = "multipart/form-data")
    )
    ResponseEntity<JwtDto> signIn(@Valid @ModelAttribute SignInRequest request, HttpServletResponse response);

    @Operation(
        summary = "로그아웃",
        description = "로그아웃합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "로그아웃 성공"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "로그아웃 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    ResponseEntity<Void> signOut(@CookieValue("REFRESH_TOKEN") String refreshToken, HttpServletResponse response) throws ParseException;

    @Operation(
        summary = "토큰 재발급",
        description = "쿠키(REFRESH_TOKEN)에 저장된 리프레시 토큰으로 리프레시 토큰과 엑세스 토큰을 재발급합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "토큰 재발급 성공",
            content = @Content(schema = @Schema(implementation = JwtDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "토큰 재발급 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @Parameter(
        name = "REFRESH_TOKEN",
        in = ParameterIn.COOKIE,
        description = "리프레시 토큰",
        required = true,
        schema = @Schema(type = "string")
    )
    ResponseEntity<JwtDto> refreshToken(@CookieValue("REFRESH_TOKEN") String refreshToken, HttpServletResponse response) throws ParseException;

    @Operation(
        summary = "비밀번호 초기화",
        description = "임시 비밀번호로 초기화 후 이메일로 전송합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "비밀번호 초기화 성공"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "비밀번호 초기화 실패(사용자 없음)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @RequestBody(
        required = true,
        content = @Content(schema = @Schema(implementation = ResetPasswordRequest.class))
    )
    ResponseEntity<Void> resetPassword(ResetPasswordRequest request);
}
