# ScanWise Developer Documentation

ScanWise is a local-first Android document scanner and digitizer built using Jetpack Compose, Room DB, WorkManager, and EncryptedSharedPreferences.

---

## Architectural Systems

```
app/src/main/java/com/khiasu/docscanai/
│
├── data/              # Database entities, DAO contracts, and Room Database initialization
├── export/            # Exporters for DOCX, PDF, CSV, JSON, and MediaStore writer
├── network/           # API Clients (Gemini, Claude, OpenAI) and base extraction schemas
├── prefs/             # Preferences handlers (Encrypted Credentials & Theme settings)
├── ui/                # Compose screens (Scan, Library, Detail, Settings) and Navigation
└── worker/            # WorkManager task for robust background page processing
```

### 1. Database & Persistence
We use a **Room Database** defined in [AppDatabase.kt](file:///e:/QuestBank_AI/app/src/main/java/com/khiasu/docscanai/data/AppDatabase.kt) with two core tables:
- **`documents`**: Holds the parent document entity (title, creation time, source type, client details).
- **`pages`**: Holds individual page records linked to a parent document via a foreign key constraint. To prevent full table scans when parent documents are updated or deleted, an index is explicitly defined on the `documentId` foreign key column.

### 2. Secure Storage (API Keys)
To ensure absolute data privacy, extraction credentials are never processed by external servers. Keys are saved on-device using Android's **`EncryptedSharedPreferences`**:
- Value elements are encrypted using **AES256-GCM**.
- Key headers are encrypted using **AES256-SIV**.
- Symmetric keys are securely sealed inside the hardware-backed **Android Keystore System**.

### 3. Background Text Extraction
Digitizing and parsing text is handled asynchronously using **WorkManager** in [ProcessPageWorker.kt](file:///e:/QuestBank_AI/app/src/main/java/com/khiasu/docscanai/worker/ProcessPageWorker.kt):
- Document operations survive app backgrounding or system garbage collection.
- Failed tasks (e.g., connection losses or API rate limits) display an `ERROR` flag, letting users trigger manual retries on individual pages.

### 4. Customizing Extraction Prompts
The instructions sent to the remote engines live in [AiClient.kt](file:///e:/QuestBank_AI/app/src/main/java/com/khiasu/docscanai/network/AiClient.kt) under `EXTRACTION_PROMPT`. You can modify this string to extract custom key-value shapes or add target validation rules.

### 5. Structured Exporters
Exporters in `export/` are hand-rolled to maintain a dependency-free codebase:
- **PDF**: Uses native canvas `PdfDocument` to write plain text pages.
- **DOCX**: Compiles raw XML document blocks zipped into an OOXML zip layout (no dependencies).
- **CSV / JSON**: Standard text structures written directly to the device's public Downloads directory using the Android scoped storage `MediaStoreWriter` helper.
