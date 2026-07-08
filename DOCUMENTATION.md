# ScanWise Developer Documentation

ScanWise is a local-first Android document scanner and digitizer built using Jetpack Compose, Room DB, WorkManager, and EncryptedSharedPreferences.

---

## Architectural Systems

```
app/src/main/java/com/khiasu/docscanai/
│
├── data/              # Database entities, DAO contracts, and Room Database initialization
├── export/            # Exporters for DOCX, PDF, CSV, JSON, and MediaStore writer
├── network/           # API Clients (Offline ML Kit, Groq, Gemini, Claude, OpenAI)
├── prefs/             # Preferences handlers (Encrypted Credentials & Theme settings)
├── ui/                # Compose screens (Scan, Library, Detail, Settings) and Navigation
└── worker/            # WorkManager task for robust background page processing
```

### 1. Database & Persistence
We use a **Room Database** defined in [AppDatabase.kt](file:///e:/QuestBank_AI/app/src/main/java/com/khiasu/docscanai/data/AppDatabase.kt) with two core tables:
- **`documents`**: Holds the parent document entity (title, creation time, source type, client details).
- **`pages`**: Holds individual page records linked to a parent document via a foreign key constraint. To prevent full table scans when parent documents are updated or deleted, an index is explicitly defined on the `documentId` foreign key column.

### 2. On-Device OCR (Offline Mode)
The default extraction engine uses Google's ML Kit Text Recognition API locally on-device.
- **Client**: [MlKitOcrClient.kt](file:///e:/QuestBank_AI/app/src/main/java/com/khiasu/docscanai/network/MlKitOcrClient.kt)
- **Adapter**: Uses a lightweight custom coroutine `Task.await()` extension to bridge Google's Task API with Kotlin Coroutines without declaring heavy play-services coroutines helper library dependencies.
- **Worker Configuration**: When `OFFLINE` is selected, `ProcessPageWorker` skips network validation constraints and processes items immediately without keys or connection.

### 3. Secure Storage (API Keys)
To ensure absolute data privacy, extraction credentials are never processed by external servers. Keys are saved on-device using Android's **`EncryptedSharedPreferences`**:
- Value elements are encrypted using **AES256-GCM**.
- Key headers are encrypted using **AES256-SIV**.
- Symmetric keys are securely sealed inside the hardware-backed **Android Keystore System**.

### 4. Background Cloud Extraction
If a cloud engine is selected (e.g. Groq Llama 3.2 Vision), pages are enqueued asynchronously using **WorkManager** in [ProcessPageWorker.kt](file:///e:/QuestBank_AI/app/src/main/java/com/khiasu/docscanai/worker/ProcessPageWorker.kt):
- Document operations survive app backgrounding or system garbage collection.
- Enqueued as a sequential chain (`doc_$docId`) per document.
- Failed tasks display an `ERROR` flag, allowing manual retry.

### 5. Customizing Extraction Prompts
The instructions sent to the cloud AI models live in [AiClient.kt](file:///e:/QuestBank_AI/app/src/main/java/com/khiasu/docscanai/network/AiClient.kt) under `EXTRACTION_PROMPT`. You can modify this string to extract custom key-value shapes or add target validation rules.

### 6. Structured Exporters
Exporters in `export/` are hand-rolled to maintain a dependency-free codebase:
- **PDF**: Uses native canvas `PdfDocument` to write plain text pages.
- **DOCX**: Compiles raw XML document blocks zipped into an OOXML zip layout (no dependencies).
- **CSV / JSON**: Standard text structures written directly to the device's public Downloads directory using the Android scoped storage `MediaStoreWriter` helper.
- On successful write, the app prompts the user to open or share the file directly using target Android Intent filters.
