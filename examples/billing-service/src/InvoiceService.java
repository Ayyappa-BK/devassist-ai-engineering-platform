package demo;
import java.math.BigDecimal;
import java.util.List;
public class InvoiceService {
 public BigDecimal total(List<BigDecimal> lines) {
  if (lines == null || lines.stream().anyMatch(p -> p == null || p.signum() < 0)) {
   throw new IllegalArgumentException("Nonnegative invoice lines required");
  }
  return lines.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
 }
}
