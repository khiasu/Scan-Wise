# ScanWise

ScanWise is a premium local-first Android document scanner and digitizer. It captures physical documents via camera, imports gallery images, or rasterizes PDF files, then extracts structured content using secure, developer-configured cloud text extraction engines. The extracted data can be exported in various structural formats (PDF, DOCX, CSV, JSON) directly to the device's public Downloads directory.

---

## Architecture Overview

The codebase is structured to run entirely on-device with zero proprietary backends, maintaining strict data privacy:

- **Local Storage**: Powered by **Room DB** to persist document metadata, page structures, and extracted texts.
- **Background Pipeline**: Uses **WorkManager** to orchestrate asynchronous page extraction. Operations are tag-chained by document and survive app destruction or backgrounding.
- **Secure Credentials**: API credentials for extraction engines are encrypted on-device via Android Keystore-backed **EncryptedSharedPreferences**.
- **Hand-Rolled Exporters**: Supports clean PDF (using native canvas-based `PdfDocument`), DOCX (using lightweight hand-rolled OOXML zip structures to avoid bulky dependencies), CSV, and JSON exports.
- **Dynamic Themes**: Integrates a dynamic theme toggling system (Cream Light theme and Purple/Black Dark theme) controlled by a live Compose state linked with local settings persistence.

---

## Getting Started

### Prerequisites
- Android Studio Koala (2024.1+) or newer.
- Android Device or Emulator running API level 26 (Android 8.0) or higher.

### Installation & Run
1. Open Android Studio, select **Open**, and navigate to the project directory.
2. Allow Gradle to sync the dependencies (Kotlin, Jetpack Compose, Room, WorkManager, OkHttp, Security Crypto).
3. Connect your device (via USB or Wireless Debugging).
4. Run the app (`Shift + F10` or click **Run**).

---

## Configuration & Usage

1. **Setup Engine**: On the first launch, go to the **Settings** tab. Choose an extraction provider (Gemini, OpenAI, or Claude), insert your developer API key, and tap **Save Settings**.
2. **Scan Documents**:
   - Navigate to the **Scan** tab.
   - Use the **Camera** to capture pages in sequence, select files from the **Gallery**, or **Import PDF File**.
   - Tap **Analyze & Digitize** to queue pages for processing.
3. **Manage & Export**:
   - Access scanned files under the **Library** tab.
   - Monitor the digitization status. If a network drop or bad key occurs, tap **Retry** on failed pages.
   - Tap **Export Document** to choose layout configurations (merged single file vs. separate files) and select the format.

---

## Directory Structure

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

---

## Extensibility

### Customizing Extraction Prompt
To modify the instructions sent to the extraction engines, edit the `EXTRACTION_PROMPT` constant defined in [AiClient.kt](file:///e:/QuestBank_AI/app/src/main/java/com/khiasu/docscanai/network/AiClient.kt). This prompt defines how the engines structure raw text transcribing and key-value field mapping.

### R8/ProGuard Rules
Before shipping a production release, make sure to add target Keep rules for Room and OkHttp in `proguard-rules.pro` and toggle `isMinifyEnabled = true` inside the release build type in `app/build.gradle.kts`.
