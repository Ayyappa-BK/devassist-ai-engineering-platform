package demo;
import java.math.BigDecimal;
import java.util.List;
public class InvoiceService {
 public BigDecimal total(List<BigDecimal> lines) {
  System.out.println("Calculating invoice");
  return lines.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
 }
}
