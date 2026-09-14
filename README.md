# BIS Intelligent Agent — Backend (SIH 26107)

Spring Boot backend for the BIS Intelligent Agent project. Implements all 8 agents +
orchestrator + admin panel + RAG reliability layer, wired end-to-end with mock/seeded
data so the frontend team can integrate against real endpoints immediately.

## What's implemented

- JWT auth (register/login), 3 self-signup roles (CONSUMER/MSME/LABORATORY), seeded
  first ADMIN account, admin-only invite/promote flow for further admins
- All REST endpoints from the build prompt, matching the exact contracts
- RAG pipeline skeleton: `RetrievalService` (embedding + Atlas Vector Search — currently
  mocked, see below), `ConfidenceScorer` (retrieval-score-derived confidence, never
  LLM-invented), `GenerationService` (refuses to call the LLM if retrieval is weak —
  hallucination prevention), `IngestionService` (chunk + embed on publish)
- Draft → pending-review → published lifecycle enforced at the service layer for
  Standards (fully) and Schemes/Services (same pattern)
- Automatic admin audit logging via an AOP aspect (no manual logging per controller)
- Process Guide Agent orchestrator chaining Standard Finder → Certification Guide →
  Find Lab, with resumable `user_journey_progress`
- Consumer Affairs with both general-query mode and complaint-filing mode

## What you need to set up manually before running

### 1. Java + Maven
```bash
java -version   # need 17+ (this project targets 21)
mvn -version
```

### 2. MongoDB Atlas (cloud, no local install needed)
1. Create a free account at https://www.mongodb.com/cloud/atlas and a free M0 cluster.
2. Create a database user (username/password).
3. Under Network Access, allow your IP (or `0.0.0.0/0` for local dev only).
4. Copy the connection string (`mongodb+srv://...`) into `MONGODB_URI`.
5. **Vector Search indexes** — once you have real data, create an Atlas Vector Search
   index on each of these paths (Atlas UI → your cluster → Search → Create Search Index
   → JSON Editor):
   - `standards.chunks.embedding`
   - `certification_schemes.chunks.embedding`
   - `bis_services.chunks.embedding`

   Example index definition (adjust `numDimensions` to match your embedding model):
   ```json
   {
     "fields": [
       { "type": "vector", "path": "chunks.embedding", "numDimensions": 1536, "similarity": "cosine" }
     ]
   }
   ```
   Until this is done, `RetrievalService` returns seeded mock matches so every
   endpoint still works for frontend integration — see the TODO comment in
   `service/rag/RetrievalService.java` for where to wire in the real
   `$vectorSearch` query once the index exists.

### 3. LLM + Embedding API keys
- LLM: an Anthropic or OpenAI API key (`LLM_API_KEY`, `LLM_PROVIDER`)
- Embeddings: an OpenAI (or other LangChain4j-supported) embedding API key
  (`EMBEDDING_API_KEY`)

### 4. Environment variables
Copy `.env.example` to `.env` (or export as real env vars / set in your run
configuration) and fill in real values. **Never commit real secrets.**

### 5. Run it
```bash
mvn spring-boot:run
```
On first boot, one ADMIN account is auto-created using `ADMIN_SEED_EMAIL` /
`ADMIN_SEED_PASSWORD` — change that password immediately after your first login.
Demo HUID + Lab records are also seeded automatically (see `config/DataSeeder.java`).

Server runs on `http://localhost:8080` by default.

## Project structure
```
config/       - Security, LangChain4j, Mongo, demo data seeding
controller/   - REST controllers (agents) + controller/admin (admin-only, @PreAuthorize)
service/
  auth/       - register/login
  rag/        - retrieval, confidence scoring, grounded generation, ingestion
  agents/     - one service per worker agent
  orchestrator/ - Process Guide Agent (flagship orchestrator)
  admin/      - content moderation (draft/review/publish), bulk import
repository/   - Spring Data MongoDB repositories, one per collection
model/        - MongoDB document classes
dto/          - request/response DTOs matching the API contracts
security/     - JWT filter + service
aspect/       - automatic admin audit logging
exception/    - global exception handler
```

## Known scaffold limitations (flagged intentionally, not hidden)
- `RetrievalService` uses seeded mock chunks instead of a live Atlas `$vectorSearch`
  call — wire in the real aggregation once your Atlas Vector Search indexes exist
  and you've ingested real BIS documents via the admin publish flow.
- Version history / restore for Standards (`/history`, `/restore/{versionId}`) needs
  a dedicated version-history collection — not yet persisted separately.
- `ProcessGuideController.advance()` doesn't yet carry the original product
  description / manufacturer type / state / district forward from `start()` into
  later steps — store these on `UserJourneyProgress` itself in a full build.
- Schemes/Services admin controllers duplicate the Standards lifecycle logic inline
  rather than sharing a generic `ContentModerationService<T>` — fine for a hackathon
  timeline, worth refactoring later if the team has time.

## API contracts
See the full contract list in the original build prompt — every endpoint listed
there is implemented in `controller/` and `controller/admin/` exactly as specified.
