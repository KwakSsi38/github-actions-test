package back.testUtil;

import back.global.security.BearerTokenResolver;
import back.global.security.JwtTokenProvider;
import back.global.security.RestAccessDeniedHandler;
import back.global.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code @WebMvcTest} 기반 컨트롤러 슬라이스 테스트 공통 지원 클래스.
 *
 * MockMvc, ObjectMapper, Security 관련 공통 MockBean을 미리 정의합니다.
 * {@code @WebMvcTest}는 Security 필터 체인을 로드하므로
 * JwtAuthenticationFilter 동작에 필요한 빈들을 mock으로 등록합니다.
 *
 * 인증이 필요한 엔드포인트 테스트에는 {@link WithMockMember}를 함께 사용합니다.
 *
 * <pre>
 * {@literal @}WebMvcTest(SomeController.class)
 * class SomeControllerTest extends WebMvcTestSupport {
 *
 *     {@literal @}MockBean
 *     SomeService someService;
 *
 *     {@literal @}Test
 *     {@literal @}WithMockMember
 *     void someTest() throws Exception {
 *         mockMvc.perform(get("/api/v1/some"))
 *                .andExpect(status().isOk())
 *                .andExpect(RsDataMatcher.hasMessage("성공했습니다."));
 *     }
 * }
 * </pre>
 */

public abstract class WebMvcTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    // JwtAuthenticationFilter 구성에 필요한 빈 mock
    @MockitoBean
    protected JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    protected BearerTokenResolver bearerTokenResolver;

    @MockitoBean
    protected RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    protected RestAccessDeniedHandler restAccessDeniedHandler;
}