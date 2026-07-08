# ScanWise

[![Download APK](https://img.shields.io/badge/Download-Latest%20APK-vividpurple?style=for-the-badge&logo=android)](https://github.com/khiasu/Scan-Wise/releases)

ScanWise is a premium, privacy-first Android document scanner and digitizer. It captures physical documents via camera, imports gallery images, or rasterizes PDF files, and extracts structured text.

By default, ScanWise runs **completely offline on-device** (no API keys, no internet required). For advanced parsing, you can optionally configure free or premium cloud AI engines.

---

## Key Features

- **On-Device OCR (Offline & Free)**: Powered by Google ML Kit. Digitizes documents locally in milliseconds without sending any data off your device. No API keys needed.
- **Multiple Engines (BYOK)**: Connect your own API key to run state-of-the-art vision models:
  - **Groq Cloud**: Free, ultra-fast Llama 3.2 Vision engine.
  - **Google Gemini**: Generous free-tier via Gemini Flash.
  - **OpenAI GPT / Anthropic Claude**: Premium cloud intelligence.
- **True AMOLED Black Theme**: Fully optimized dark mode built with deep violet accents and pure black background (`#000000`) to maximize battery savings on OLED screens.
- **Seamless Document Exports**: Redesigned, non-clipping export menu to output documents as PDF, DOCX (Word), CSV (Spreadsheet), or JSON (Metadata).
- **Direct Actions**: Instant "Open File" and "Share" options on export completion dialogs.
- **Hardware-Backed Encryption**: Stored credentials are encrypted on-device using AES-256 and backed by the Android Keystore system.

---

## Quick Start

1. Download and install the [latest APK](https://github.com/khiasu/Scan-Wise/releases) on your Android device (API 26+).
2. Open the app — it runs on **On-Device OCR** by default out-of-the-box.
3. Tap **Scan**, capture pages or select a PDF, and click **Process & Save Document** to process.
4. (Optional) Go to **Settings**, choose a cloud provider (e.g. Groq Cloud), enter your API key, and click Save.

---

## Documentation

For developers, structural details, database schemas, and building layouts are documented in [DOCUMENTATION.md](file:///e:/QuestBank_AI/DOCUMENTATION.md).
