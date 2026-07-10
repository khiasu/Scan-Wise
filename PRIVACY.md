# Privacy & Terms of Use

This document outlines the privacy policy and terms of use for **Question Bank Digitizer**.

---

## 1. Privacy Policy (Privacy-First Principle)

**Question Bank Digitizer** is designed to operate as a local pass-through utility. We believe in absolute user data sovereignty and privacy.

### A. Local Credentials Encryption
- All API keys (Google Gemini, Groq, OpenAI, Claude) configured by users are encrypted on-device.
- Encryption uses standard Android Jetpack Security libraries powered by **AES-256-GCM** (authenticated symmetric encryption) and **AES-256-SIV** (deterministic key wrapping).
- Symmetrical key materials are sealed inside the phone's **hardware-backed Android Keystore system** (utilizing StrongBox or TrustZone where available).

### B. No Analytics or Backend
- There is **no registration system, no account creation, no tracking metrics, and no analytics SDKs** (such as Firebase, Mixpanel, or custom endpoints) in this application.
- Your scanned images, OCR transcriptions, and generated question banks are stored entirely inside a local SQLite Room database on your phone.

### C. Direct Client-to-API Communications
- The application connects directly from your device to the API endpoints of the providers you configure (e.g. Gemini API, Groq Console, OpenAI API, Anthropic Console).
- No middleman server is used. Transactions are anonymous pass-through payloads signed by your own key.

---

## 2. Terms of Use

By using this application, you agree to the following conditions:

- **API Token Responsibility**: You are solely responsible for managing and configuring your own API keys. Any monetary charges incurred on your cloud providers (Gemini, Groq, OpenAI, Claude) during test solving or transcriptions are your responsibility.
- **Local Database Cleanliness**: All documents are stored inside the app sandbox on your local storage. If you clear the app's cache, clear app data, or uninstall the app, your local scans and database history will be permanently deleted.
- **Third-Party Terms**: Your use of cloud API engines is subject to the terms and privacy conditions of the respective providers (Google Cloud, Groq Inc., OpenAI, and Anthropic).
