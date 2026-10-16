# Billing service contract
InvoiceService.total returns the sum of nonnegative invoice lines using BigDecimal.
An empty invoice returns zero. Null collections, null entries and negative values are rejected with IllegalArgumentException.
Amounts retain decimal precision. Currency conversion and tax calculation are outside this service.
