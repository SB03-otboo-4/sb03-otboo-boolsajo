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
import com.sprint.otboo.user.entity.User;
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

    private final JWSSigner accessTokenSigner;
    private final JWSVerifier accessTokenVerifier;

    public TokenProvider(
        @Value("${jwt.access-token.secret}") String accessTokenSecret,
        @Value("${jwt.access-token.expiration}") long accessTokenExpiration
    ) throws JOSEException {
        this.accessTokenExpiration = accessTokenExpiration;
        byte[] accessTokenSecretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSigner = new MACSigner(accessTokenSecretBytes);
        this.accessTokenVerifier = new MACVerifier(accessTokenSecretBytes);
    }

    public String createAccessToken(User user) throws JOSEException {
        return createToken(user, accessTokenExpiration, accessTokenSigner, "access");
    }

    private String createToken(User user, long expiration, JWSSigner signer, String tokenType) throws JOSEException {
        String tokenId = UUID.randomUUID().toString();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(user.getEmail())
            .jwtID(tokenId)
            .issueTime(now)
            .expirationTime(expiryDate)
            .claim("userId", user.getId().toString())
            .claim("username", user.getUsername())
            .claim("type", tokenType)
            .claim("roles", List.of("ROLE_" + user.getRole().name()))
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
}
