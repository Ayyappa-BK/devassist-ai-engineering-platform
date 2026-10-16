package demo;
import java.util.HashMap;
import java.util.Map;
public class InventoryService {
 private final Map<String,Integer> stock = new HashMap<>();
 public synchronized void replenish(String sku, int count) {
  if (sku == null || sku.isBlank() || count <= 0) throw new IllegalArgumentException("Invalid stock entry");
  stock.merge(sku,count,Math::addExact);
 }
 public synchronized boolean reserve(String sku, int count) {
  if (count <= 0) throw new IllegalArgumentException("Positive count required");
  int available = stock.getOrDefault(sku,0);
  if (available < count) return false;
  stock.put(sku,available-count);
  return true;
 }
}
