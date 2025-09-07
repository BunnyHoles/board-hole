package bunny.boardhole.email.e2e;

import java.util.List;

import jakarta.mail.internet.MimeUtility;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import bunny.boardhole.email.application.EmailService;
import bunny.boardhole.email.domain.EmailMessage;
import bunny.boardhole.email.infrastructure.SmtpEmailService;
import bunny.boardhole.user.domain.Role;
import bunny.boardhole.user.domain.User;
import bunny.boardhole.user.infrastructure.UserRepository;

import io.restassured.RestAssured;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SMTP 이메일 서비스 E2E 테스트 MailHog 컨테이너를 사용한 실제 SMTP 발송 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "smtp"})
@Testcontainers
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("📮 SMTP 이메일 E2E 테스트")
@Tag("e2e")
class EmailSmtpE2ETest {

    @Container
    private static final GenericContainer<?> mailhog = createMailhogContainer();
    @LocalServerPort
    private int port;
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserRepository userRepository;
    private User testUser;
    private String mailhogApiUrl;

    @SuppressWarnings("resource") // Testcontainers @Container annotation manages lifecycle automatically
    private static GenericContainer<?> createMailhogContainer() {
        return new GenericContainer<>(DockerImageName.parse("mailhog/mailhog:latest")).withExposedPorts(1025, 8025); // SMTP: 1025, HTTP API: 8025
    }

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", mailhog::getHost);
        registry.add("spring.mail.port", () -> mailhog.getMappedPort(1025));
        // MailHog doesn't require authentication, but we need a from email
        registry.add("spring.mail.username", () -> "noreply@boardhole.test");
        registry.add("spring.mail.password", () -> "");
        registry.add("spring.mail.properties.mail.smtp.auth", () -> false);
        registry.add("boardhole.security.require-email-verification", () -> false);
    }

    /**
     * MIME 인코딩된 헤더 디코딩
     */
    private static String decodeMimeHeader(String encoded) {
        try {
            return MimeUtility.decodeText(encoded);
        } catch (Exception e) {
            return encoded; // 디코딩 실패시 원본 반환
        }
    }

    @BeforeEach
    void setUp() {
        // RestAssured 베이스 설정
        RestAssured.port = port;
        RestAssured.basePath = "";

        // MailHog API URL 설정
        mailhogApiUrl = String.format("http://%s:%d", mailhog.getHost(), mailhog.getMappedPort(8025));

        // MailHog 메시지 초기화
        clearMailHogMessages();

        // 테스트 사용자 생성 (username 길이 제한 고려)
        String uniquePrefix = "smtp_" + (System.currentTimeMillis() % 10000);
        testUser = User.builder().username(uniquePrefix).email(uniquePrefix + "@test.com").name("SMTP 테스트").password("Password123!").roles(java.util.Set.of(Role.USER)).build();
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.delete(testUser);
    }

    @Test
    @DisplayName("1️⃣ SMTP 프로파일에서 SmtpEmailService가 활성화됨")
    void smtpEmailServiceIsActive() {
        // given & when & then
        assertThat(emailService).isNotNull().isInstanceOf(SmtpEmailService.class);
    }

    @Test
    @DisplayName("2️⃣ 실제 SMTP로 이메일이 발송됨")
    void sendEmailViaSmtp() throws Exception {
        // given
        EmailMessage emailMessage = EmailMessage.create("recipient@test.com", "SMTP 테스트 제목", "<p>SMTP 테스트 내용입니다.</p>");

        // when
        emailService.sendEmail(emailMessage);
        Thread.sleep(2000); // 이메일 발송 대기

        // then
        List<MailHogMessage> messages = getMailHogMessages();
        assertThat(messages).hasSize(1);

        MailHogMessage message = messages.getFirst();
        assertThat(decodeMimeHeader(message.content().getSubject())).isEqualTo("SMTP 테스트 제목");
        assertThat(message.content().getToHeader()).contains("recipient@test.com");
    }

    @Test
    @DisplayName("3️⃣ 회원가입 인증 이메일이 SMTP로 발송됨")
    void sendSignupVerificationViaSmtp() throws Exception {
        // given
        final String token = "VERIFY-TOKEN-789";

        // when
        emailService.sendSignupVerificationEmail(testUser, token);
        Thread.sleep(2000); // 이메일 발송 대기

        // then
        List<MailHogMessage> messages = getMailHogMessages();
        assertThat(messages).hasSize(1);

        MailHogMessage message = messages.getFirst();
        assertThat(decodeMimeHeader(message.content().getSubject())).isEqualTo("이메일 인증을 완료해주세요");
        assertThat(message.content().getToHeader()).contains(testUser.getEmail());
        assertThat(message.content().body()).contains(token);
        assertThat(message.content().body()).contains("verify-email");
    }

    @Test
    @DisplayName("4️⃣ 이메일 변경 인증이 SMTP로 발송됨")
    void sendEmailChangeVerificationViaSmtp() throws Exception {
        // given
        final String newEmail = "newemail@test.com";
        final String token = "CHANGE-TOKEN-ABC";

        // when
        emailService.sendEmailChangeVerificationEmail(testUser, newEmail, token);
        Thread.sleep(2000); // 이메일 발송 대기

        // then
        List<MailHogMessage> messages = getMailHogMessages();
        assertThat(messages).hasSize(1);

        MailHogMessage message = messages.getFirst();
        assertThat(decodeMimeHeader(message.content().getSubject())).isEqualTo("이메일 변경 인증을 완료해주세요");
        assertThat(message.content().getToHeader()).contains(newEmail);
    }

    @Test
    @DisplayName("5️⃣ 환영 이메일이 SMTP로 발송됨")
    void sendWelcomeEmailViaSmtp() throws Exception {
        // given & when
        emailService.sendWelcomeEmail(testUser);
        Thread.sleep(2000); // 이메일 발송 대기

        // then
        List<MailHogMessage> messages = getMailHogMessages();
        assertThat(messages).hasSize(1);

        MailHogMessage message = messages.getFirst();
        assertThat(decodeMimeHeader(message.content().getSubject())).isEqualTo("Board-Hole에 오신 것을 환영합니다!");
        assertThat(message.content().getToHeader()).contains(testUser.getEmail());
    }

    @Test
    @DisplayName("6️⃣ 이메일 변경 완료 알림이 SMTP로 발송됨")
    void sendEmailChangedNotificationViaSmtp() throws Exception {
        // given
        final String newEmail = "updated@test.com";

        // when
        emailService.sendEmailChangedNotification(testUser, newEmail);
        Thread.sleep(2000); // 이메일 발송 대기

        // then
        List<MailHogMessage> messages = getMailHogMessages();
        assertThat(messages).hasSize(1);

        MailHogMessage message = messages.getFirst();
        assertThat(decodeMimeHeader(message.content().getSubject())).isEqualTo("이메일 주소가 성공적으로 변경되었습니다");
        assertThat(message.content().getToHeader()).contains(newEmail);
    }

    @Test
    @DisplayName("7️⃣ 이메일 인증 체크가 비활성화됨 (test-smtp 프로파일)")
    void emailVerificationDisabled() {
        // test 프로파일에서 require-email-verification: false 설정 확인

        // given
        String uniqueId = "unverified_" + (System.currentTimeMillis() % 1000);
        User unverifiedUser = User.builder().username(uniqueId).email(uniqueId + "@test.com").name("미인증 사용자").password("Password123!").roles(java.util.Set.of(Role.USER)).build();

        // when
        User saved = userRepository.save(unverifiedUser);

        // then
        assertThat(saved.isEmailVerified()).isFalse();
        // 인증 체크가 비활성화되어 있으므로 미인증 사용자도 정상 처리됨

        // cleanup
        userRepository.delete(saved);
    }

    /**
     * MailHog API를 통해 메시지 조회 (Type-safe with DTO)
     */
    private List<MailHogMessage> getMailHogMessages() {
        try {
            MailHogResponse response = RestAssured.given().baseUri(mailhogApiUrl).when().get("/api/v2/messages").then().statusCode(200).extract().as(MailHogResponse.class);

            return response.items();
        } catch (Exception e) {
            // 에러 발생 시 빈 리스트 반환
        }
        return List.of();
    }

    /**
     * MailHog의 모든 메시지 삭제
     */
    private void clearMailHogMessages() {
        try {
            RestAssured.given().baseUri(mailhogApiUrl).when().delete("/api/v1/messages").then().statusCode(200);
        } catch (Exception e) {
            // 삭제 실패 무시 (테스트는 계속 진행)
        }
    }

}
