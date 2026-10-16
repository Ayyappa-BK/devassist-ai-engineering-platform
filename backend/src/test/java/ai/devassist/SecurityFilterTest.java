package ai.devassist;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

class SecurityFilterTest {
  @Test
  void rateLimitAndSizeBound() throws Exception {
    var filter = new SecurityFilter("test-token-abcdefghijklmnopqrstuvwxyz");
    for (int i = 0; i < 61; i++) {
      var request = new MockHttpServletRequest("GET", "/api/health");
      request.addHeader("Authorization", "Bearer test-token-abcdefghijklmnopqrstuvwxyz");
      var response = new MockHttpServletResponse();
      filter.doFilter(request, response, (a, b) -> {});
      assertEquals(i < 60 ? 200 : 429, response.getStatus());
      if (i == 60) assertEquals("60", response.getHeader("Retry-After"));
    }
    var request = new MockHttpServletRequest("POST", "/api/generate");
    request.addHeader("Authorization", "Bearer test-token-abcdefghijklmnopqrstuvwxyz");
    request.setContent(new byte[1048577]);
    var response = new MockHttpServletResponse();
    new SecurityFilter("test-token-abcdefghijklmnopqrstuvwxyz")
        .doFilter(request, response, (a, b) -> fail("Oversized request reached handler"));
    assertEquals(413, response.getStatus());
  }
}
