package nextstep.auth.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import nextstep.auth.context.Authentication;
import nextstep.auth.token.JwtTokenProvider;
import nextstep.auth.token.TokenRequest;
import nextstep.auth.token.TokenResponse;
import nextstep.member.application.UserDetailsService;
import nextstep.member.domain.LoginMember;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TokenAuthenticationInterceptor implements HandlerInterceptor {

  private final UserDetailsService userDetailsService;
  private final JwtTokenProvider jwtTokenProvider;
  private final ObjectMapper objectMapper;

  public TokenAuthenticationInterceptor(UserDetailsService userDetailsService, JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
    this.userDetailsService = userDetailsService;
    this.jwtTokenProvider = jwtTokenProvider;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
    AuthenticationToken authenticationToken = convert(request);
    Authentication authentication = authenticate(authenticationToken);

    String payload = objectMapper.writeValueAsString(authentication.getPrincipal());
    String token = jwtTokenProvider.createToken(payload);
    TokenResponse tokenResponse = new TokenResponse(token);

    String responseToClient = objectMapper.writeValueAsString(tokenResponse);
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getOutputStream().print(responseToClient);

    return false;
  }

  public AuthenticationToken convert(HttpServletRequest request) throws IOException {
    TokenRequest tokenRequest = objectMapper.readValue(request.getInputStream(), TokenRequest.class);
    String principal = tokenRequest.getEmail();
    String credentials = tokenRequest.getPassword();

    return new AuthenticationToken(principal, credentials);
  }

  public Authentication authenticate(AuthenticationToken authenticationToken) {
    LoginMember loginMember = userDetailsService.loadUserByUsername(authenticationToken.getPrincipal());

    validateAuthentication(loginMember, authenticationToken.getCredentials());
    return new Authentication(loginMember);
  }

  private void validateAuthentication(LoginMember loginMember, String credentials) {
    if (!loginMember.checkPassword(credentials)) {
      throw new AuthenticationException();
    }
  }
}
