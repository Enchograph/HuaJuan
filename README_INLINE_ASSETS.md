Inline assets helper

Folders created under `app/src/main/assets/inline/`:
- js/
- fonts/
- images/

Usage:
- Put the exact files you want to embed into `app/src/main/assets/inline/js`, `.../fonts`, `.../images`.
- Run the helper to generate `inlined_assets.json` in `app/src/main/assets/inline_out`:

```powershell
python tools/inline_assets.py --input-dir app/src/main/assets/inline --output-dir app/src/main/assets/inline_out
```

- `inlined_assets.json` will contain base64 data for each resource; you can then modify `ShareHelper.kt` to read that JSON at runtime (from assets) and inline the data into the HTML template instead of referencing external CDNs.

Notes:
- For fonts you may want to embed woff2 files and generate corresponding @font-face CSS with data URIs.
- For JS libraries (e.g., marked.min.js), you can embed the raw script text inside a <script> tag in the HTML template (no base64 needed), but the helper outputs base64 for binary safety.
- I placed a small placeholder `marked.min.js` in `app/src/main/assets/inline/js/` — replace it with the real file if desired.

