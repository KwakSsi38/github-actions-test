package back.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import back.domain.auth.port.GoogleIdTokenVerifier;
import back.domain.auth.entity.RefreshToken;
import back.domain.auth.repository.RefreshTokenRepository;
import back.domain.member.entity.Member;
import back.domain.member.entity.MemberRole;
import back.domain.member.repository.MemberRepository;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import back.global.security.JwtTokenProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "ADMIN_ALLOWLIST_EMAILS=admin-login@example.com")
@AutoConfigureMockMvc
@Transactional
@Import(AuthIntegrationTest.TestControllerConfig.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("구글 로그인 신규 회원이면 회원 생성 후 내부 토큰을 발급한다")
    void googleLogin_newMember_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "google-id-token-user-301"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.memberId").isNumber())
                .andExpect(jsonPath("$.data.email").value("user301@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        Member savedMember = memberRepository.findByGoogleSub("google-sub-301").orElseThrow();
        assertThat(savedMember.getEmail()).isEqualTo("user301@example.com");
        assertThat(savedMember.getName()).isEqualTo("User 301");
        assertThat(savedMember.getRole()).isEqualTo(MemberRole.USER);
        assertThat(refreshTokenRepository.findByMemberId(savedMember.getId())).isPresent();
    }

    @Test
    @DisplayName("구글 로그인 이메일이 allowlist에 포함되면 ADMIN으로 승격된다")
    void googleLogin_adminAllowlistPromote() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "google-id-token-admin-302"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        Member savedMember = memberRepository.findByGoogleSub("google-sub-302").orElseThrow();
        assertThat(savedMember.getRole()).isEqualTo(MemberRole.ADMIN);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("구글 로그인 재시도 시 동일 sub 회원은 이름/이메일을 최신값으로 갱신한다")
    void googleLogin_existingMemberUpdateProfile() throws Exception {
        memberRepository.findByGoogleSub("google-sub-303").ifPresent(memberRepository::delete);
        memberRepository.findByEmail("old303@example.com").ifPresent(memberRepository::delete);
        memberRepository.findByEmail("new303@example.com").ifPresent(memberRepository::delete);
        memberRepository.flush();

        memberRepository.saveAndFlush(Member.createUser("google-sub-303", "old303@example.com", "Old 303"));

        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "google-id-token-update-303"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.email").value("new303@example.com"))
                .andExpect(jsonPath("$.data.name").value("New 303"));

        Member updatedMember = memberRepository.findByGoogleSub("google-sub-303").orElseThrow();
        assertThat(updatedMember.getEmail()).isEqualTo("new303@example.com");
        assertThat(updatedMember.getName()).isEqualTo("New 303");

        memberRepository.delete(updatedMember);
        memberRepository.flush();
    }

    @Test
    @DisplayName("유효한 refresh 토큰이면 재발급 성공 후 기존 refresh 재사용은 실패한다")
    void refresh_successAndOldTokenReuseFail() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-101", "u101@example.com", "User 101"));
        TokenFixture tokenFixture = issueAndStore(member);

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(tokenFixture.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("토큰 재발급 성공"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String rotatedRefreshToken = jsonNode.get("data").get("refreshToken").asText();
        assertThat(rotatedRefreshToken).isNotEqualTo(tokenFixture.refreshToken());

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(tokenFixture.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."));
    }

    @Test
    @DisplayName("재발급 API는 만료/위조 access 헤더가 포함되어도 refresh 토큰으로 처리한다")
    void refresh_withInvalidAuthorizationHeader() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-103", "u103@example.com", "User 103"));
        TokenFixture tokenFixture = issueAndStore(member);

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(tokenFixture.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("토큰 재발급 성공"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("로그아웃 API는 인증 정보가 없으면 401을 반환한다")
    void logout_withoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "dummy"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("인증된 USER가 ADMIN API에 접근하면 403을 반환한다")
    void adminApi_withUserRole() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-102", "u102@example.com", "User 102"));
        TokenFixture tokenFixture = issueAndStore(member);

        mockMvc.perform(get("/api/v1/admin/probe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(tokenFixture.accessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("명시 규칙이 없는 /api/v1 경로는 기본적으로 인증이 필요하다")
    void defaultApiPolicy_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/security/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("명시 규칙이 없는 /api/v1 경로도 인증되면 접근 가능하다")
    void defaultApiPolicy_authenticatedUserCanAccess() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-104", "u104@example.com", "User 104"));
        TokenFixture tokenFixture = issueAndStore(member);

        mockMvc.perform(get("/api/v1/security/probe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(tokenFixture.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("보호 리소스 접근 성공"));
    }

    @Test
    @DisplayName("로그아웃 요청의 refresh 토큰이 본인 소유가 아니면 403을 반환한다")
    void logout_withOtherMembersRefreshToken() throws Exception {
        Member owner = memberRepository.save(Member.createUser("google-sub-201", "u201@example.com", "User 201"));
        Member attacker = memberRepository.save(Member.createUser("google-sub-202", "u202@example.com", "User 202"));
        TokenFixture ownerTokenFixture = issueAndStore(owner);
        TokenFixture attackerTokenFixture = issueAndStore(attacker);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(attackerTokenFixture.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(ownerTokenFixture.refreshToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("본인 토큰이 아닙니다."));
    }

    private TokenFixture issueAndStore(Member member) {
        String role = member.getRole().name();
        String accessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getEmail(), role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId(), member.getEmail(), role);
        refreshTokenRepository.save(RefreshToken.issue(member.getId(), refreshToken));
        return new TokenFixture(accessToken, refreshToken);
    }

    record TokenFixture(String accessToken, String refreshToken) {}

    @TestConfiguration
    static class TestControllerConfig {

        @Bean
        AdminProbeController adminProbeController() {
            return new AdminProbeController();
        }

        @Bean
        @Primary
        GoogleIdTokenVerifier googleIdTokenVerifier() {
            return idToken -> switch (idToken) {
                case "google-id-token-user-301" ->
                    new GoogleIdTokenVerifier.GoogleUserInfo("google-sub-301", "user301@example.com", "User 301");
                case "google-id-token-admin-302" ->
                    new GoogleIdTokenVerifier.GoogleUserInfo(
                            "google-sub-302", "admin-login@example.com", "Admin 302");
                case "google-id-token-update-303" ->
                    new GoogleIdTokenVerifier.GoogleUserInfo("google-sub-303", "new303@example.com", "New 303");
                default -> throw new ServiceException(
                        CommonErrorCode.UNAUTHORIZED,
                        "[AuthIntegrationTest#googleIdTokenVerifier] unsupported idToken",
                        "유효하지 않은 구글 토큰입니다.");
            };
        }
    }

    @RestController
    static class AdminProbeController {

        @GetMapping("/api/v1/admin/probe")
        String probe() {
            return MemberRole.ADMIN.name();
        }

        @GetMapping("/api/v1/security/probe")
        tools.jackson.databind.node.ObjectNode securityProbe() {
            tools.jackson.databind.node.JsonNodeFactory jsonNodeFactory =
                    tools.jackson.databind.node.JsonNodeFactory.instance;
            tools.jackson.databind.node.ObjectNode objectNode = jsonNodeFactory.objectNode();
            objectNode.put("message", "보호 리소스 접근 성공");
            return objectNode;
        }
    }
}
