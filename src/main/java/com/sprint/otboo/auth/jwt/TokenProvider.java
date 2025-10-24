package com.sprint.otboo.auth.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sprint.otboo.common.exception.auth.InvalidTokenException;
import com.sprint.otboo.common.exception.auth.TokenExpiredException;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Role;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class TokenProvider {

    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    private final JWSSigner accessTokenSigner;
    private final JWSSigner refreshTokenSigner;
    private final JWSVerifier accessTokenVerifier;
    private final JWSVerifier refreshTokenVerifier;

    public TokenProvider(
        @Value("${jwt.access-token.secret}") String accessTokenSecret,
        @Value("${jwt.access-token.expiration}") long accessTokenExpiration,
        @Value("${jwt.refresh-token.secret}") String refreshTokenSecret,
        @Value("${jwt.refresh-token.expiration}") long refreshTokenExpiration
    ) throws JOSEException {
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;

        byte[] accessTokenSecretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSigner = new MACSigner(accessTokenSecretBytes);
        this.accessTokenVerifier = new MACVerifier(accessTokenSecretBytes);

        byte[] refreshTokenSecretBytes = refreshTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.refreshTokenSigner = new MACSigner(refreshTokenSecretBytes);
        this.refreshTokenVerifier = new MACVerifier(refreshTokenSecretBytes);
    }

    public String createAccessToken(UserDto user) throws JOSEException {
        return createToken(user, accessTokenExpiration, accessTokenSigner, "access");
    }

    public String createRefreshToken(UserDto user) throws JOSEException {
        return createToken(user, refreshTokenExpiration, refreshTokenSigner, "refresh");
    }

    /**
     * JWT의 Claim을 설정하고 서명하여 직렬화된 토큰 문자열을 생성한다.
     *
     * @param user       사용자 정보 DTO
     * @param expiration 만료 시간
     * @param signer     서명기
     * @param tokenType  토큰 타입 ("access" 또는 "refresh")
     * @return 직렬화된 JWT 문자열
     * @throws JOSEException 토큰 서명 실패 시
     */
    private String createToken(UserDto user, long expiration, JWSSigner signer, String tokenType) throws JOSEException {
        String tokenId = UUID.randomUUID().toString();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(user.email())
            .jwtID(tokenId)
            .issueTime(now)
            .expirationTime(expiryDate)
            .claim("userId", user.id().toString())
            .claim("username", user.name())
            .claim("type", tokenType)
            .claim("roles", List.of("ROLE_" + user.role().name()))
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader(JWSAlgorithm.HS256),
            claimsSet
        );

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public void validateAccessToken(String token) throws TokenExpiredException, ParseException {
        validateToken(token, accessTokenVerifier, "access");
    }

    public void validateRefreshToken(String token) throws TokenExpiredException, ParseException {
        validateToken(token, refreshTokenVerifier, "refresh");
    }

    /**
     * 주어진 토큰의 서명, 타입, 만료 시간을 검증합니다.
     *
     * @param token        검증할 토큰
     * @param verifier     검증기
     * @param expectedType 기대하는 토큰 타입
     * @throws TokenExpiredException 토큰이 만료된 경우
     * @throws ParseException      토큰 파싱에 실패한 경우
     * @throws InvalidTokenException 서명이나 타입이 유효하지 않은 경우
     */
    private void validateToken(String token, JWSVerifier verifier, String expectedType)
        throws TokenExpiredException, ParseException, InvalidTokenException {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!signedJWT.verify(verifier)) {
                throw new InvalidTokenException();
            }

            String tokenType = (String) signedJWT.getJWTClaimsSet().getClaim("type");
            if (!expectedType.equals(tokenType)) {
                throw new InvalidTokenException();
            }

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                throw new TokenExpiredException();
            }

        } catch (JOSEException e) {
            throw new InvalidTokenException(e);
        }
    }

    /**
     * 유효한 Access Token에서 사용자 정보를 추출하여 Spring Security의 Authentication 객체를 생성한다.
     *
     * @param token Access Token
     * @return 생성된 Authentication 객체
     * @throws ParseException 토큰 파싱에 실패한 경우
     */
    public Authentication getAuthentication(String token) throws ParseException {
        // 1. 토큰을 파싱하여 Claims 추출
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

        // 2. Claims에서 사용자 정보(username)와 권한(roles) 추출
        String userId = (String) claimsSet.getClaim("userId");
        String email = claimsSet.getSubject();
        String username = (String) claimsSet.getClaim("username");
        List<String> roles = (List<String>) claimsSet.getClaim("roles");

        Role role = roles.isEmpty() ? null : Role.valueOf(roles.get(0).replace("ROLE_", ""));

        // 3. 추출한 정보로 UserDto 생성
        UserDto userDto = new UserDto(
            UUID.fromString(userId),
            claimsSet.getIssueTime().toInstant(),
            email,
            username,
            role,
            null,
            false
        );

        // 4. UserDto를 사용하여 CustomUserDetails 생성
        UserDetails principal = new CustomUserDetails(userDto, "");

        // 5. CustomUserDetails를 principal로 사용하는 Authentication 객체 생성
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public String getEmailFromRefreshToken(String token) throws ParseException {

        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getSubject();
    }
}
