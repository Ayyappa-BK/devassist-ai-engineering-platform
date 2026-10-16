package ai.devassist;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurityFilter implements Filter {
  private final byte[] token;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  private record Window(long minute, AtomicInteger count) {}

  public SecurityFilter(@Value("${app.token}") String token) {
    if (token.length() < 32)
      throw new IllegalArgumentException("API_TOKEN must contain at least 32 characters");
    this.token = token.getBytes(StandardCharsets.UTF_8);
  }

  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    var request = (HttpServletRequest) req;
    var response = (HttpServletResponse) res;
    String trace = UUID.randomUUID().toString();
    MDC.put("traceId", trace);
    response.setHeader("X-Trace-Id", trace);
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Cache-Control", "no-store");
    long start = System.nanoTime();
    try {
      String auth = Objects.toString(request.getHeader("Authorization"), "");
      if (!MessageDigest.isEqual(
          ("Bearer " + new String(token, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8),
          auth.getBytes(StandardCharsets.UTF_8))) {
        response.sendError(401);
        return;
      }
      long minute = System.currentTimeMillis() / 60000;
      Window win =
          windows.compute(
              "operator",
              (k, v) ->
                  v == null || v.minute() != minute ? new Window(minute, new AtomicInteger()) : v);
      if (win.count().incrementAndGet() > 60) {
        response.setHeader("Retry-After", "60");
        response.sendError(429);
        return;
      }
      if (request.getContentLengthLong() > 1048576) {
        response.sendError(413);
        return;
      }
      byte[] body = request.getInputStream().readNBytes(1048577);
      if (body.length > 1048576) {
        response.sendError(413);
        return;
      }
      var wrapped =
          new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() {
              var input = new java.io.ByteArrayInputStream(body);
              return new ServletInputStream() {
                public int read() {
                  return input.read();
                }

                public boolean isFinished() {
                  return input.available() == 0;
                }

                public boolean isReady() {
                  return true;
                }

                public void setReadListener(ReadListener listener) {
                  throw new UnsupportedOperationException();
                }
              };
            }
          };
      wrapped.setAttribute("owner", "operator");
      chain.doFilter(wrapped, res);
    } finally {
      LoggerFactory.getLogger(getClass())
          .info(
              "request method={} status={} latencyMs={}",
              request.getMethod(),
              response.getStatus(),
              (System.nanoTime() - start) / 1000000);
      MDC.clear();
    }
  }
}
