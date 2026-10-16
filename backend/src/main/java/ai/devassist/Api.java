package ai.devassist;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api")
public class Api {
  private final RepositoryService repos;
  private final JdbcTemplate db;
  private final RestClient worker;

  public Api(
      RepositoryService repos,
      JdbcTemplate db,
      @Value("${app.worker}") String url,
      @Value("${app.worker-token}") String token) {
    this.repos = repos;
    this.db = db;
    this.worker =
        RestClient.builder()
            .baseUrl(url)
            .defaultHeader("Authorization", "Bearer " + token)
            .requestFactory(factory())
            .build();
  }

  private static org.springframework.http.client.SimpleClientHttpRequestFactory factory() {
    var f = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    f.setConnectTimeout(3000);
    f.setReadTimeout(35000);
    return f;
  }

  public record FileRequest(
      @NotBlank @Size(max = 80) String repo, @NotBlank @Size(max = 240) String path) {}

  public record GenerateRequest(
      @NotBlank @Size(max = 80) String repo,
      @NotBlank @Size(max = 240) String path,
      @NotNull @Pattern(regexp = "explain|tests|quality|docs|review") String task,
      @Size(max = 240) String basePath) {}

  public record ReviewRequest(@NotNull @Pattern(regexp = "APPROVED|REJECTED") String decision) {}

  @GetMapping("/health")
  public Map<String, String> health() {
    db.queryForObject("SELECT 1", Integer.class);
    return Map.of("status", "ok");
  }

  @GetMapping("/repositories/{repo}/files")
  public List<String> files(@PathVariable String repo) throws IOException {
    return repos.files(repo);
  }

  @PostMapping("/analyze")
  public Map<String, Object> analyze(@Valid @RequestBody FileRequest r) throws IOException {
    if (!r.path.endsWith(".java")) throw new IllegalArgumentException("Java required");
    return repos.analyze(repos.read(r.repo, r.path));
  }

  @org.springframework.transaction.annotation.Transactional
  @PostMapping("/documents/approve")
  public Map<String, String> approve(@Valid @RequestBody FileRequest r) throws IOException {
    if (!r.path.endsWith(".md")) throw new IllegalArgumentException("Markdown required");
    String text = repos.read(r.repo, r.path);
    String digest = RepositoryService.digest(text);
    db.update(
        "DELETE FROM approved_docs WHERE owner_id=? AND repo=? AND path=?",
        "operator",
        r.repo,
        r.path);
    db.update(
        "INSERT INTO approved_docs(id,owner_id,repo,path,digest) VALUES(?,?,?,?,?)",
        UUID.randomUUID().toString(),
        "operator",
        r.repo,
        r.path,
        digest);
    return Map.of("digest", digest);
  }

  @PostMapping("/generate")
  public Map<String, Object> generate(@Valid @RequestBody GenerateRequest r) throws IOException {
    if (r.task == null) throw new IllegalArgumentException("Task required");
    long start = System.nanoTime();
    String source = repos.read(r.repo, r.path);
    Object analysis = r.path.endsWith(".java") ? repos.analyze(source) : Map.of();
    List<Map<String, String>> documents = new ArrayList<>();
    for (var row :
        db.queryForList(
            "SELECT path,digest FROM approved_docs WHERE owner_id=? AND repo=?",
            "operator",
            r.repo)) {
      String path = (String) row.get("path");
      String content = repos.read(r.repo, path);
      if (RepositoryService.digest(content).equals(row.get("digest")))
        documents.add(Map.of("path", path, "content", content));
      if (documents.size() >= 20) break;
    }
    String before = "";
    if (r.task.equals("review")) {
      if (r.basePath == null)
        throw new IllegalArgumentException("Review requires basePath snapshot");
      before = repos.read(r.repo, r.basePath);
    }
    Map<String, Object> payload = new HashMap<>();
    payload.put("source", source);
    payload.put("before", before);
    payload.put("task", r.task);
    payload.put("documents", documents);
    payload.put("analysis", analysis);
    Map<?, ?> answer =
        worker
            .post()
            .uri("/generate")
            .header("X-Trace-Id", org.slf4j.MDC.get("traceId"))
            .body(payload)
            .retrieve()
            .body(Map.class);
    if (answer == null || !(answer.get("content") instanceof String))
      throw new IllegalStateException("Invalid worker response");
    String content = (String) answer.get("content");
    if (content.length() > 40000) throw new IllegalStateException("Output too large");
    Guard.check(content);
    String id = UUID.randomUUID().toString();
    long latency = (System.nanoTime() - start) / 1000000;
    db.update(
        "INSERT INTO drafts(id,owner_id,repo,task,content,status,latency_ms) VALUES(?,?,?,?,?,?,?)",
        id,
        "operator",
        r.repo,
        r.task,
        content,
        "PENDING",
        latency);
    return Map.of("id", id, "content", content, "status", "PENDING", "latencyMs", latency);
  }

  @GetMapping("/drafts")
  public List<Map<String, Object>> drafts() {
    return db.queryForList(
        "SELECT * FROM drafts WHERE owner_id=? ORDER BY created_at DESC LIMIT 100", "operator");
  }

  @PostMapping("/drafts/{id}/review")
  public Map<String, String> review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest r) {
    if (r.decision == null) throw new IllegalArgumentException("Decision required");
    if (db.update(
            "UPDATE drafts SET status=? WHERE id=? AND owner_id=? AND status='PENDING'",
            r.decision,
            id.toString(),
            "operator")
        != 1) throw new IllegalArgumentException("Pending draft unavailable");
    return Map.of("status", r.decision);
  }

  @GetMapping("/drafts/{id}/export")
  public Map<String, Object> export(@PathVariable UUID id) {
    var rows =
        db.queryForList(
            "SELECT content FROM drafts WHERE id=? AND owner_id=? AND status='APPROVED'",
            id.toString(),
            "operator");
    if (rows.isEmpty()) throw new IllegalArgumentException("Approval required");
    return rows.get(0);
  }

  @RestControllerAdvice
  static class Errors {
    @ExceptionHandler({
      org.springframework.web.bind.MethodArgumentNotValidException.class,
      org.springframework.http.converter.HttpMessageNotReadableException.class,
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> validation(Exception e) {
      return Map.of("error", "Invalid request");
    }

    @ExceptionHandler({
      IllegalArgumentException.class,
      IOException.class,
      com.github.javaparser.ParseProblemException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> invalid(Exception e) {
      return Map.of("error", "Invalid, unsafe, or unavailable input");
    }

    @ExceptionHandler(org.springframework.web.client.RestClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    Map<String, String> worker(Exception e) {
      return Map.of("error", "AI service unavailable");
    }
  }
}
