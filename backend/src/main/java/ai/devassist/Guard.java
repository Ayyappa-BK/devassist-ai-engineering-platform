package ai.devassist;

import java.util.regex.Pattern;

public final class Guard {
  private static final Pattern SECRET =
      Pattern.compile(
          "(?i)(-----BEGIN[ A-Z]*PRIVATE"
              + " KEY|AKIA[A-Z0-9]{16}|gh[pousr]_[A-Za-z0-9]{20,}|sk-[A-Za-z0-9_-]{16,}|(?:password|passwd|api[_-]?key|secret|token)\\s*[:=]\\s*[\"']?[^\\s\"';]{8,})");
  private static final Pattern INJECTION =
      Pattern.compile(
          "(?i)(ignore.{0,40}(instructions|prompt)|system\\s*prompt|<\\|(?:system|im_start)\\|>|exfiltrat|reveal.{0,30}secret|override.{0,30}instructions)");

  public static void check(String s) {
    if (SECRET.matcher(s).find()) throw new IllegalArgumentException("Sensitive material blocked");
    if (INJECTION.matcher(s).find())
      throw new IllegalArgumentException("Instruction-like content blocked");
  }
}
