# Question Bank Digitizer

Question Bank Digitizer is a minimal, professional Android application designed for educators, teachers, and students to digitize physical test papers, compile structured question sheets, generate answer keys, and run interactive practice quizzes.

For developer architecture, database schemas, and system layout details, see [DOCUMENTATION.md](file:///e:/QuestBank_AI/DOCUMENTATION.md).
For data safety, hardware key storage encryption, and usage rules, see [PRIVACY.md](file:///e:/QuestBank_AI/PRIVACY.md).

---

## Key Features

- **Two-Step Question Digitizer Pipeline**:
  - **Fast OCR Transcription**: Performs rapid text transcription of scanned pages to avoid API timeouts.
  - **Interactive Question Solver**: A single click instructs the AI model to extract questions, solve multiple-choice and subjective problems, and write concise explanations and hints.
- **Interactive Practice Quizzes**:
  - Test yourself or students directly inside the app.
  - **Multiple-Choice Questions (MCQs)**: Renders clickable options with instant visual grading (green/red feedback), score counts, and solution walkthroughs.
  - **Subjective Questions**: Write responses, review model answer rubrics, and self-grade.
- **Engaging Page History**:
  - Tracks scanned pages with decoded thumbnail image previews that remain visible during background processing.
  - Delete individual pages from document history or edit the transcribed text manually to correct formatting.
- **Structured Spreadsheet & Document Exports**:
  - **CSV Export**: Splits questions cleanly into spreadsheet columns: `Page, Question Number, Type, Marks, Question, Paraphrased Question, Option A/B/C/D, Correct Answer, Explanation, Hint`.
  - **JSON Export**: Serializes solved data into fully parsed JSON models.
  - **PDF & Word (DOCX)**: Outputs beautifully formatted question sheets and solution booklets.
- **Hardware-Backed Encryption**: Cloud API credentials (Gemini, Groq Cloud, OpenAI, Anthropic Claude) are encrypted on-device using AES-256 GCM sealed inside the Android Keystore System.

---

## Quick Start

1. Download and install the latest release APK.
2. Go to **Settings**, choose a cloud provider (e.g. Google Gemini or Groq Cloud), enter your API key, and click Save.
3. Tap **Scan**, capture pages or import a PDF, and tap **Process & Save** to add it to history.
4. Open the document, head to the **Question Bank** tab, and click **Solve & Generate Answers** to solve the questions.
5. Practice the solved questions under the **Practice Quiz** tab, or export the paper using the **Export** menu.
