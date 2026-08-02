# ReceiptBrain AI

ReceiptBrain AI is a full-stack Spring Boot application that provides secure receipt upload, OCR-ready storage, expense tracking, and warranty management features.

## Features
- JWT authentication and private, per-user receipt access
- Receipt upload and secure local storage
- Suggested merchant/category from the uploaded filename, plus a verification form for accurate details
- Spending dashboard with totals, search and receipt library
- Warranty expiry calculation and alerts for expired or next-60-day coverage deadlines

## Run locally
1. Install Java 21 and Maven.
2. From the project root, run:
   ```bash
   mvn spring-boot:run
   ```
3. Open http://localhost:8081

## Demo flow
1. Create an account and upload a receipt image or PDF.
2. Click the uploaded receipt and verify the merchant, amount, date and warranty duration.
3. The dashboard updates spending totals immediately; warranties expiring within 60 days appear in the alert panel.

## Container run
```bash
docker build -t receiptbrain-ai .
docker run --rm -p 8081:8081 receiptbrain-ai
```

## Free Render + Supabase deployment
1. Push this folder to a GitHub repository.
2. Create a free Supabase project. Copy its PostgreSQL connection values and create a private Storage bucket named `receipts`.
3. Create a new Render Blueprint from the repository; Render reads `render.yaml` and creates the free web service.
4. Set `DB_URL` (JDBC PostgreSQL URL), `DB_USERNAME`, `DB_PASSWORD`, `SUPABASE_URL`, and `SUPABASE_SERVICE_KEY` in Render. Never commit these secrets.
5. Free Render services sleep after inactivity, while Supabase keeps the database and files separate from the app container.

## Notes
- The default database is H2 in-memory for quick setup.
- The included SVG demo documents are automatically parsed for merchant, amount, date, payment method, category and warranty months. Photo/PDF OCR is intentionally not claimed as complete: connect a production OCR provider in `ReceiptService.extractReceiptDetails` for real photographed receipts.
- H2 is in-memory, so data resets each time the app stops. Use a persistent database before deployment.
# ReceiptBrainAI
