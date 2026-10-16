package ai.devassist;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RepositoryService {
  private final Path root;

  public RepositoryService(@Value("${app.repos}") String root) {
    this.root = Path.of(root).toAbsolutePath().normalize();
  }

  public Path repo(String name) throws IOException {
    if (!name.matches("[a-zA-Z0-9_-]{1,80}"))
      throw new IllegalArgumentException("Invalid repository");
    Path p = root.resolve(name);
    if (!Files.isDirectory(p)
        || Files.isSymbolicLink(p)
        || !p.toRealPath().getParent().equals(root.toRealPath()))
      throw new IllegalArgumentException("Repository unavailable");
    return p;
  }

  public String read(String repo, String file) throws IOException {
    if (file.length() > 240
        || file.contains("\\")
        || file.startsWith("/")
        || Arrays.stream(file.split("/")).anyMatch(part -> part.startsWith(".") || part.isBlank())
        || !file.matches("[a-zA-Z0-9_./-]+")
        || !(file.endsWith(".java") || file.endsWith(".md")))
      throw new IllegalArgumentException("Only safe Java and Markdown paths allowed");
    Path base = repo(repo), p = base.resolve(file).normalize();
    Path cursor = base;
    for (Path part : base.relativize(p)) {
      cursor = cursor.resolve(part);
      if (Files.isSymbolicLink(cursor)) throw new IllegalArgumentException("Symlinks forbidden");
    }
    if (!p.startsWith(base)
        || !p.toRealPath().startsWith(base.toRealPath())
        || !Files.isRegularFile(p)
        || Files.size(p) > 65536)
      throw new IllegalArgumentException("File unavailable or too large");
    try (var in = Files.newInputStream(p)) {
      byte[] bytes = in.readNBytes(65537);
      if (bytes.length > 65536) throw new IllegalArgumentException("File too large");
      String s = new String(bytes, StandardCharsets.UTF_8);
      Guard.check(s);
      return s;
    }
  }

  public List<String> files(String name) throws IOException {
    try (var paths = Files.walk(repo(name), 12)) {
      return paths
          .limit(5000)
          .filter(Files::isRegularFile)
          .filter(p -> !Files.isSymbolicLink(p))
          .map(p -> repoPath(name, p))
          .filter(s -> Arrays.stream(s.split("/")).noneMatch(part -> part.startsWith(".")))
          .filter(s -> s.endsWith(".java") || s.endsWith(".md"))
          .sorted()
          .limit(200)
          .toList();
    }
  }

  private String repoPath(String name, Path p) {
    return root.resolve(name).relativize(p).toString();
  }

  public Map<String, Object> analyze(String source) {
    StaticJavaParser.getParserConfiguration()
        .setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_21);
    var unit = StaticJavaParser.parse(source);
    var methods = unit.findAll(MethodDeclaration.class);
    List<String> issues = new ArrayList<>();
    methods.forEach(
        m -> {
          if (m.getParameters().size() > 4)
            issues.add("Too many parameters: " + m.getNameAsString());
          if (m.toString().lines().count() > 30) issues.add("Long method: " + m.getNameAsString());
        });
    if (source.contains("System.out")) issues.add("Use structured logging instead of System.out");
    if (source.contains("catch (Exception")) issues.add("Broad exception handler requires review");
    return Map.of(
        "methodCount",
        methods.size(),
        "methods",
        methods.stream().map(m -> m.getNameAsString()).toList(),
        "issues",
        issues);
  }

  public static String digest(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
