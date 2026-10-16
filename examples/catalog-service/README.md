# Catalog inventory
InventoryService replenishes and reserves stock. Reservation must not exceed available units.
Reservations are serialized within one JVM. Persistence and distributed concurrency require a separate design.
