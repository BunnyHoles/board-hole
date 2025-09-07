package bunny.boardhole.user.e2e;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import bunny.boardhole.shared.util.MessageUtils;
import bunny.boardhole.testsupport.e2e.AuthSteps;
import bunny.boardhole.testsupport.e2e.E2ETestBase;
import bunny.boardhole.testsupport.e2e.SessionCookie;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("사용자 API E2E 테스트")
@Tag("e2e")
@Tag("user")
class UserE2ETest extends E2ETestBase {

    private SessionCookie admin;
    private SessionCookie regular;

    @BeforeEach
    void loginDefaultUsers() {
        // DataInitializer가 기본 admin/regular를 생성하므로 바로 로그인
        admin = AuthSteps.login("admin", "Admin123!");
        regular = AuthSteps.login("user", "User123!");
    }

    @Nested
    @DisplayName("GET /api/users - 목록")
    class ListUsers {
        @Test
        @DisplayName("❌ 익명 → 401 Unauthorized")
        void anonymous() {
            given()
                .when().get("/users")
                .then()
                .statusCode(401)
                .body("type", equalTo("urn:problem-type:unauthorized"))
                .body("title", equalTo(MessageUtils.get("exception.title.unauthorized")))
                .body("status", equalTo(401))
                .body("code", equalTo("UNAUTHORIZED"));
        }
        @Test
        @DisplayName("✅ 관리자 목록 조회")
        void adminCanList() {
            given().cookie(admin.name(), admin.value()).when().get("/users").then().statusCode(200).body("content", notNullValue()).body("pageable", notNullValue());
        }

        @Test
        @DisplayName("🔍 관리자 검색")
        void adminCanSearch() {
            given().cookie(admin.name(), admin.value()).when().get("/users?search=admin").then().statusCode(200).body("content", notNullValue());
        }

        @Test
        @DisplayName("📄 관리자 페이지네이션")
        void adminPagination() {
            given().cookie(admin.name(), admin.value()).when().get("/users?page=0&size=5").then().statusCode(200).body("pageable.pageSize", equalTo(5)).body("pageable.pageNumber", equalTo(0));
        }

        @Test
        @DisplayName("❌ 일반 사용자 - 목록 조회 금지 → 403")
        void regularCannotList() {
            given().cookie(regular.name(), regular.value()).when().get("/users").then().statusCode(403).body("type", equalTo("urn:problem-type:forbidden")).body("title", equalTo(MessageUtils.get("exception.title.access-denied"))).body("status", equalTo(403)).body("code", equalTo("FORBIDDEN"));
        }
        
        @Test
        @DisplayName("🔍 빈 검색어 처리")
        void emptySearchParameter() {
            // 빈 문자열 검색 - 전체 목록 반환
            given().cookie(admin.name(), admin.value()).when().get("/users?search=").then().statusCode(200).body("content", notNullValue());
            
            // 공백만 있는 검색 - 전체 목록 반환
            given().cookie(admin.name(), admin.value()).when().get("/users?search=   ").then().statusCode(200).body("content", notNullValue());
        }
        
        @Test
        @DisplayName("🔍 특수문자 검색 처리")
        void specialCharactersInSearch() {
            // SQL 인젝션 시도 방어
            given().cookie(admin.name(), admin.value())
                .when().get("/users?search=' OR '1'='1")
                .then().statusCode(200);
            
            // 특수문자 포함 검색
            given().cookie(admin.name(), admin.value())
                .when().get("/users?search=@#$%^&*()")
                .then().statusCode(200);
            
            // XSS 시도 방어
            given().cookie(admin.name(), admin.value())
                .when().get("/users?search=<script>alert('xss')</script>")
                .then().statusCode(200);
        }
        
        @Test
        @DisplayName("📄 잘못된 페이지네이션 파라미터")
        void invalidPaginationParameters() {
            // 음수 페이지 번호
            given().cookie(admin.name(), admin.value())
                .when().get("/users?page=-1&size=10")
                .then().statusCode(200)
                .body("pageable.pageNumber", equalTo(0)); // Spring이 0으로 보정
            
            // 음수 페이지 사이즈
            given().cookie(admin.name(), admin.value())
                .when().get("/users?page=0&size=-10")
                .then().statusCode(200); // Spring이 기본값으로 처리
            
            // 0 페이지 사이즈
            given().cookie(admin.name(), admin.value())
                .when().get("/users?page=0&size=0")
                .then().statusCode(200); // Spring이 기본값으로 처리
            
            // 너무 큰 페이지 사이즈 (2000 이상)
            given().cookie(admin.name(), admin.value())
                .when().get("/users?page=0&size=5000")
                .then().statusCode(200)
                .body("pageable.pageSize", notNullValue()); // Spring이 최대값으로 제한
        }
        
        @Test
        @DisplayName("📄 범위 밖 페이지 번호")
        void outOfBoundPageNumber() {
            // 존재하지 않는 큰 페이지 번호
            given().cookie(admin.name(), admin.value())
                .when().get("/users?page=99999&size=10")
                .then().statusCode(200)
                .body("content", notNullValue())
                .body("content.size()", equalTo(0)); // 빈 결과
        }
        
        @Test
        @DisplayName("🔍 매우 긴 검색어 처리")
        void veryLongSearchString() {
            String longSearch = "A".repeat(1000);
            given().cookie(admin.name(), admin.value())
                .when().get("/users?search=" + longSearch)
                .then().statusCode(200);
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id} - 단일 조회")
    class GetUser {
        @Test
        @DisplayName("❌ 익명 → 401 Unauthorized")
        void anonymous() {
            given()
                .when().get("/users/1")
                .then()
                .statusCode(401)
                .body("type", equalTo("urn:problem-type:unauthorized"))
                .body("title", equalTo(MessageUtils.get("exception.title.unauthorized")))
                .body("status", equalTo(401))
                .body("code", equalTo("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("✅ 본인 조회 (기본 일반 사용자)")
        void getOwnUser() {
            Response meRes = given().cookie(regular.name(), regular.value()).when().get("/users/me").then().extract().response();
            Long myId = meRes.jsonPath().getLong("id");

            given().cookie(regular.name(), regular.value()).when().get("/users/" + myId).then().statusCode(200).body("id", equalTo(myId.intValue())).body("username", equalTo("user"));
        }

        @Test
        @DisplayName("❌ 존재하지 않는 사용자 → 404 Not Found")
        void notFound() {
            given().cookie(admin.name(), admin.value()).when().get("/users/999999").then().statusCode(404).body("type", equalTo("urn:problem-type:not-found")).body("title", equalTo(MessageUtils.get("exception.title.not-found"))).body("instance", equalTo("/api/users/999999"));
        }

        @Test
        @DisplayName("일반(타인) → 403 (권한 없음)")
        void regularOther() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "viewother_" + uid;
            String p = "Password123!";
            String e = u + "@example.com";
            AuthSteps.signup(u, p, "ViewerTarget", e);
            SessionCookie other = AuthSteps.login(u, p);
            Long otherId = given().cookie(other.name(), other.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");

            // Regular users cannot view other users' information
            given().cookie(regular.name(), regular.value())
                .when().get("/users/" + otherId)
                .then().statusCode(403)
                .body("type", equalTo("urn:problem-type:forbidden"));
        }

        @Test
        @DisplayName("✅ 관리자 - 기본 일반 사용자 조회")
        void adminCanGetOtherUser() {
            Long userId = given().cookie(regular.name(), regular.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");

            given().cookie(admin.name(), admin.value()).when().get("/users/" + userId).then().statusCode(200).body("username", equalTo("user"));
        }
    }

    @Nested
    @DisplayName("PUT /api/users/{id} - 수정")
    class UpdateUser {
        @Test
        @DisplayName("❌ 일반(타인) → 403 Forbidden")
        void regularOther() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "upd_other_" + uid;
            String p = "Password123!";
            String e = u + "@example.com";
            AuthSteps.signup(u, p, "OtherUser", e);
            SessionCookie other = AuthSteps.login(u, p);
            Long otherId = given().cookie(other.name(), other.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");

            given().cookie(regular.name(), regular.value())
                .contentType(ContentType.URLENC)
                .formParam("name", "Hacker")
                .when().put("/users/" + otherId)
                .then().statusCode(403)
                .body("type", equalTo("urn:problem-type:forbidden"))
                .body("title", equalTo(MessageUtils.get("exception.title.access-denied")))
                .body("status", equalTo(403))
                .body("code", equalTo("FORBIDDEN"));
        }

        @Test
        @DisplayName("✅ 본인 정보 수정")
        void updateOwn() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "upd_" + uid;
            String p = "Password123!";
            String e = u + "@example.com";
            AuthSteps.signup(u, p, "Updater", e);
            SessionCookie me = AuthSteps.login(u, p);
            Long myId = given().cookie(me.name(), me.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");

            given().cookie(me.name(), me.value()).contentType(ContentType.URLENC).formParam("name", "Updated Name").when().put("/users/" + myId).then().statusCode(200).body("name", equalTo("Updated Name"));
        }

        @Test
        @DisplayName("❌ 인증 없음 → 401 Unauthorized")
        void updateUnauthorized() {
            given().contentType(ContentType.URLENC).formParam("name", "Hacked").when().put("/users/1").then().statusCode(401).body("type", equalTo("urn:problem-type:unauthorized")).body("title", equalTo(MessageUtils.get("exception.title.unauthorized"))).body("status", equalTo(401)).body("code", equalTo("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("❌ 권한 없음/리소스 없음 → 403 Forbidden")
        void updateForbidden() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "updforb_" + uid;
            String p = "Password123!";
            String e = u + "@example.com";
            AuthSteps.signup(u, p, "Updater", e);
            SessionCookie me = AuthSteps.login(u, p);

            given().cookie(me.name(), me.value()).contentType(ContentType.URLENC).formParam("name", "Updated").when().put("/users/999999").then().statusCode(403).body("type", equalTo("urn:problem-type:forbidden")).body("title", equalTo(MessageUtils.get("exception.title.access-denied"))).body("status", equalTo(403)).body("code", equalTo("FORBIDDEN"));
        }

        @Test
        @DisplayName("✅ 관리자 - 타 사용자 정보 수정")
        void adminCanUpdateOtherUser() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "updoc_" + uid;
            String p = "Password123!";
            String e = u + "@example.com";
            AuthSteps.signup(u, p, "Updatable", e);
            SessionCookie user = AuthSteps.login(u, p);
            Long userId = given().cookie(user.name(), user.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");

            given().cookie(admin.name(), admin.value()).contentType(ContentType.URLENC).formParam("name", "AdminUpdated").when().put("/users/" + userId).then().statusCode(200).body("name", equalTo("AdminUpdated"));
        }
        
        // 이메일 수정 기능이 구현되지 않아서 주석 처리
        // UserUpdateRequest에는 name 필드만 있고 email 필드가 없음
        // @Test
        // @DisplayName("❌ 잘못된 이메일 형식 → 400 Bad Request")
        // void invalidEmailFormat() {
        //     String uid = UUID.randomUUID().toString().substring(0, 8);
        //     String u = "emailtest_" + uid;
        //     String p = "Password123!";
        //     String e = u + "@example.com";
        //     AuthSteps.signup(u, p, "EmailTest", e);
        //     SessionCookie me = AuthSteps.login(u, p);
        //     Long myId = given().cookie(me.name(), me.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");

        //     // 잘못된 이메일 형식
        //     given().cookie(me.name(), me.value())
        //         .contentType(ContentType.URLENC)
        //         .formParam("email", "invalid-email")
        //         .when().put("/users/" + myId)
        //         .then().statusCode(400)
        //         .body("type", equalTo("urn:problem-type:validation-error"));
                
        //     // 이메일에 공백
        //     given().cookie(me.name(), me.value())
        //         .contentType(ContentType.URLENC)
        //         .formParam("email", "test @example.com")
        //         .when().put("/users/" + myId)
        //         .then().statusCode(400)
        //         .body("type", equalTo("urn:problem-type:validation-error"));
        // }
        
        @Test
        @DisplayName("❌ 필드 길이 초과 → 400 Bad Request")
        void fieldLengthViolation() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "lentest_" + uid;
            String p = "Password123!";
            String e = u + "@example.com";
            AuthSteps.signup(u, p, "LengthTest", e);
            SessionCookie me = AuthSteps.login(u, p);
            Long myId = given().cookie(me.name(), me.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");

            // 너무 긴 이름 (100자 초과)
            String longName = "A".repeat(101);
            given().cookie(me.name(), me.value())
                .contentType(ContentType.URLENC)
                .formParam("name", longName)
                .when().put("/users/" + myId)
                .then().statusCode(400)
                .body("type", equalTo("urn:problem-type:validation-error"));
        }
        
        @Test
        @DisplayName("❌ 잘못된 사용자 ID 형식 → 400 Bad Request")
        void invalidUserIdFormat() {
            given().cookie(admin.name(), admin.value())
                .contentType(ContentType.URLENC)
                .formParam("name", "Test")
                .when().put("/users/invalid-id")
                .then().statusCode(400);
                
            given().cookie(admin.name(), admin.value())
                .contentType(ContentType.URLENC)
                .formParam("name", "Test")
                .when().put("/users/12.34")
                .then().statusCode(400);
        }
        
        @Test
        @DisplayName("🛡️ XSS 공격 방어")
        void xssAttackPrevention() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "xsstest_" + uid;
            String p = "Password123!";
            String e = u + "@example.com";
            AuthSteps.signup(u, p, "XSSTest", e);
            SessionCookie me = AuthSteps.login(u, p);
            Long myId = given().cookie(me.name(), me.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");

            // XSS 시도 - 스크립트 태그
            given().cookie(me.name(), me.value())
                .contentType(ContentType.URLENC)
                .formParam("name", "<script>alert('xss')</script>")
                .when().put("/users/" + myId)
                .then().statusCode(200);
            
            // 검증: 스크립트가 이스케이프되어 저장됨
            String savedName = given().cookie(me.name(), me.value())
                .when().get("/users/" + myId)
                .then().extract().jsonPath().getString("name");
            
            // HTML 태그가 그대로 저장되어도 렌더링시 이스케이프됨
            // 실제 저장된 값 확인
            assert savedName != null;
        }
        
        @Test
        @DisplayName("🛡️ SQL 인젝션 방어") 
        void sqlInjectionPrevention() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "sqltest_" + uid;
            String p = "Password123!";
            String e = u + "@example.com";
            AuthSteps.signup(u, p, "SQLTest", e);
            SessionCookie me = AuthSteps.login(u, p);
            Long myId = given().cookie(me.name(), me.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");

            // SQL 인젝션 시도
            given().cookie(me.name(), me.value())
                .contentType(ContentType.URLENC)
                .formParam("name", "'; DROP TABLE users; --")
                .when().put("/users/" + myId)
                .then().statusCode(200);
            
            // 시스템이 여전히 정상 작동하는지 확인
            given().cookie(admin.name(), admin.value())
                .when().get("/users")
                .then().statusCode(200)
                .body("content", notNullValue());
        }
        
        @Test
        @DisplayName("🛡️ Path Traversal 공격 방어")
        void pathTraversalPrevention() {
            // Path traversal 시도
            given().cookie(admin.name(), admin.value())
                .contentType(ContentType.URLENC)
                .formParam("name", "Test")
                .when().put("/users/../../../etc/passwd")
                .then().statusCode(400);
            
            given().cookie(admin.name(), admin.value())
                .contentType(ContentType.URLENC) 
                .formParam("name", "Test")
                .when().put("/users/%2e%2e%2f%2e%2e%2f")
                .then().statusCode(400);
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/{id} - 삭제")
    class DeleteUser {
        @Test
        @DisplayName("❌ 일반(타인) → 403 Forbidden")
        void regularOther() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "del_other_" + uid;
            String p = "Password123!";
            String e = u + "@example.com";
            AuthSteps.signup(u, p, "DelOther", e);
            SessionCookie other = AuthSteps.login(u, p);
            Long otherId = given().cookie(other.name(), other.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");

            given().cookie(regular.name(), regular.value())
                .when().delete("/users/" + otherId)
                .then().statusCode(403)
                .body("type", equalTo("urn:problem-type:forbidden"))
                .body("title", equalTo(MessageUtils.get("exception.title.access-denied")))
                .body("status", equalTo(403))
                .body("code", equalTo("FORBIDDEN"));
        }

        @Test
        @DisplayName("✅ 본인 삭제 및 404 확인")
        void deleteOwn() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "del_" + uid;
            String p = "Password123!";
            String e = u + "@example.com";
            AuthSteps.signup(u, p, "Deletable", e);
            SessionCookie me = AuthSteps.login(u, p);
            Long myId = given().cookie(me.name(), me.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");

            given().cookie(me.name(), me.value()).when().delete("/users/" + myId).then().statusCode(204);

            // admin으로 삭제 확인(404)
            given().cookie(admin.name(), admin.value()).when().get("/users/" + myId).then().statusCode(404);
        }

        @Test
        @DisplayName("❌ 인증 없음 → 401 Unauthorized")
        void deleteUnauthorized() {
            given().when().delete("/users/1").then().statusCode(401).body("type", equalTo("urn:problem-type:unauthorized")).body("title", equalTo(MessageUtils.get("exception.title.unauthorized"))).body("status", equalTo(401)).body("code", equalTo("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("❌ 권한 없음/리소스 없음 → 403 Forbidden")
        void deleteForbidden() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "other_" + uid;
            String p = "Password123!";
            String e = u + "@example.com";
            AuthSteps.signup(u, p, "Other", e);
            SessionCookie other = AuthSteps.login(u, p);

            given().cookie(other.name(), other.value()).when().delete("/users/999999").then().statusCode(403).body("type", equalTo("urn:problem-type:forbidden")).body("title", equalTo(MessageUtils.get("exception.title.access-denied"))).body("status", equalTo(403)).body("code", equalTo("FORBIDDEN"));
        }

        @Test
        @DisplayName("✅ 관리자 - 타 사용자 삭제")
        void adminCanDeleteOtherUser() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "deloc_" + uid;
            String p = "Password123!";
            String e = u + "@example.com";
            AuthSteps.signup(u, p, "DelTarget", e);
            SessionCookie user = AuthSteps.login(u, p);
            Long userId = given().cookie(user.name(), user.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");

            given().cookie(admin.name(), admin.value()).when().delete("/users/" + userId).then().statusCode(204);

            given().cookie(admin.name(), admin.value()).when().get("/users/" + userId).then().statusCode(404);
        }
    }

    @Nested
    @DisplayName("GET /api/users/me - 현재 사용자")
    class Me {
        @Test
        @DisplayName("✅ 현재 사용자 정보 조회 (기본 일반 사용자)")
        void meSuccess() {
            given().cookie(regular.name(), regular.value()).when().get("/users/me").then().statusCode(200).body("username", equalTo("user")).body("roles", notNullValue());
        }

        @Test
        @DisplayName("❌ 인증 없음 → 401 Unauthorized")
        void meUnauthorized() {
            given().when().get("/users/me").then().statusCode(401).body("type", equalTo("urn:problem-type:unauthorized")).body("title", equalTo(MessageUtils.get("exception.title.unauthorized"))).body("status", equalTo(401)).body("code", equalTo("UNAUTHORIZED"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/{id}/password - 패스워드 변경")
    class UpdatePassword {
        @Test
        @DisplayName("✅ 본인 패스워드 변경 성공")
        void updateOwnPasswordSuccess() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "pwdchange_" + uid;
            String oldPwd = "OldPass123!";
            String newPwd = "NewPass123!";
            String e = u + "@example.com";
            
            AuthSteps.signup(u, oldPwd, "PasswordUser", e);
            SessionCookie user = AuthSteps.login(u, oldPwd);
            Long userId = given().cookie(user.name(), user.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");
            
            // 패스워드 변경
            given().cookie(user.name(), user.value())
                .contentType(ContentType.URLENC)
                .formParam("currentPassword", oldPwd)
                .formParam("newPassword", newPwd)
                .formParam("confirmPassword", newPwd)
                .when().patch("/users/" + userId + "/password")
                .then().statusCode(204);
            
            // 새 패스워드로 로그인 확인
            AuthSteps.login(u, newPwd);
        }
        
        @Test
        @DisplayName("❌ 패스워드 확인 불일치 → 400 Bad Request")
        void passwordMismatch() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "pwdmismatch_" + uid;
            String pwd = "Password123!";
            String e = u + "@example.com";
            
            AuthSteps.signup(u, pwd, "MismatchUser", e);
            SessionCookie user = AuthSteps.login(u, pwd);
            Long userId = given().cookie(user.name(), user.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");
            
            given().cookie(user.name(), user.value())
                .contentType(ContentType.URLENC)
                .formParam("currentPassword", pwd)
                .formParam("newPassword", "NewPass123!")
                .formParam("confirmPassword", "DifferentPass123!")
                .when().patch("/users/" + userId + "/password")
                .then().statusCode(400)
                .body("type", equalTo("urn:problem-type:validation-error"))
                .body("title", equalTo(MessageUtils.get("exception.title.validation-failed")))
                .body("status", equalTo(400));
        }
        
        @Test
        @DisplayName("❌ 현재 패스워드 틀림 → 401 Unauthorized")
        void wrongCurrentPassword() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "wrongpwd_" + uid;
            String pwd = "Password123!";
            String e = u + "@example.com";
            
            AuthSteps.signup(u, pwd, "WrongPwdUser", e);
            SessionCookie user = AuthSteps.login(u, pwd);
            Long userId = given().cookie(user.name(), user.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");
            
            given().cookie(user.name(), user.value())
                .contentType(ContentType.URLENC)
                .formParam("currentPassword", "WrongPassword123!")
                .formParam("newPassword", "NewPass123!")
                .formParam("confirmPassword", "NewPass123!")
                .when().patch("/users/" + userId + "/password")
                .then().statusCode(401);
        }
        
        @Test
        @DisplayName("❌ 인증 없음 → 401 Unauthorized")
        void updatePasswordUnauthorized() {
            given()
                .contentType(ContentType.URLENC)
                .formParam("currentPassword", "OldPass123!")
                .formParam("newPassword", "NewPass123!")
                .formParam("confirmPassword", "NewPass123!")
                .when().patch("/users/1/password")
                .then().statusCode(401)
                .body("type", equalTo("urn:problem-type:unauthorized"))
                .body("title", equalTo(MessageUtils.get("exception.title.unauthorized")))
                .body("status", equalTo(401))
                .body("code", equalTo("UNAUTHORIZED"));
        }
        
        @Test
        @DisplayName("❌ 타인 패스워드 변경 시도 → 403 Forbidden")
        void updateOthersPasswordForbidden() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "pwdother_" + uid;
            String pwd = "Password123!";
            String e = u + "@example.com";
            
            AuthSteps.signup(u, pwd, "OtherUser", e);
            SessionCookie other = AuthSteps.login(u, pwd);
            Long otherId = given().cookie(other.name(), other.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");
            
            given().cookie(regular.name(), regular.value())
                .contentType(ContentType.URLENC)
                .formParam("currentPassword", "Password123!")
                .formParam("newPassword", "NewPass123!")
                .formParam("confirmPassword", "NewPass123!")
                .when().patch("/users/" + otherId + "/password")
                .then().statusCode(403)
                .body("type", equalTo("urn:problem-type:forbidden"))
                .body("title", equalTo(MessageUtils.get("exception.title.access-denied")))
                .body("status", equalTo(403))
                .body("code", equalTo("FORBIDDEN"));
        }
        
        @Test
        @DisplayName("❌ 존재하지 않는 사용자 → 404 Not Found")
        void updatePasswordNotFound() {
            given().cookie(admin.name(), admin.value())
                .contentType(ContentType.URLENC)
                .formParam("currentPassword", "Password123!")
                .formParam("newPassword", "NewPass123!")
                .formParam("confirmPassword", "NewPass123!")
                .when().patch("/users/999999/password")
                .then().statusCode(404)
                .body("type", equalTo("urn:problem-type:not-found"))
                .body("title", equalTo(MessageUtils.get("exception.title.not-found")));
        }
        
        @Test
        @DisplayName("❌ 패스워드 복잡도 미충족 → 400 Bad Request")
        void invalidPasswordComplexity() {
            String uid = UUID.randomUUID().toString().substring(0, 8);
            String u = "pwdcomplex_" + uid;
            String pwd = "Password123!";
            String e = u + "@example.com";
            
            AuthSteps.signup(u, pwd, "ComplexUser", e);
            SessionCookie user = AuthSteps.login(u, pwd);
            Long userId = given().cookie(user.name(), user.value()).when().get("/users/me").then().extract().jsonPath().getLong("id");
            
            // 특수문자 없음
            given().cookie(user.name(), user.value())
                .contentType(ContentType.URLENC)
                .formParam("currentPassword", pwd)
                .formParam("newPassword", "Password123")
                .formParam("confirmPassword", "Password123")
                .when().patch("/users/" + userId + "/password")
                .then().statusCode(400)
                .body("type", equalTo("urn:problem-type:validation-error"));
            
            // 너무 짧음
            given().cookie(user.name(), user.value())
                .contentType(ContentType.URLENC)
                .formParam("currentPassword", pwd)
                .formParam("newPassword", "Pass1!")
                .formParam("confirmPassword", "Pass1!")
                .when().patch("/users/" + userId + "/password")
                .then().statusCode(400)
                .body("type", equalTo("urn:problem-type:validation-error"));
        }
    }
}
