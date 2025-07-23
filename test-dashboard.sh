#!/bin/bash

echo "🧪 Creating test payments for dashboard..."

# Create a few test payments
curl -s -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: dashboard-test-1" \
  -d '{"amount": 99.99, "currency": "USD", "merchantId": "merchant_1", "customerId": "customer_1", "description": "Dashboard test payment 1"}' > /dev/null

curl -s -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: dashboard-test-2" \
  -d '{"amount": 250.50, "currency": "EUR", "merchantId": "merchant_2", "customerId": "customer_2", "description": "Dashboard test payment 2"}' > /dev/null

curl -s -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: dashboard-test-3" \
  -d '{"amount": 75.25, "currency": "GBP", "merchantId": "merchant_3", "customerId": "customer_3", "description": "Dashboard test payment 3"}' > /dev/null

echo "✅ Test payments created! Visit http://localhost:8080/dashboard"
