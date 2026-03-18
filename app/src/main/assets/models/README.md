Place bundled local MNN models in this directory.

Supported layouts:

1. Manifest-driven
   - models/model_config.json
   - models/<model-dir>/llm.mnn
   - models/<model-dir>/...other required files

2. Auto-discovery fallback
   - models/<model-dir>/llm.mnn
   - models/<model-dir>/metadata.json (optional)
   - models/<model-dir>/...other required files

Notes:
- The main model file must be named llm.mnn.
- metadata.json can reuse the same fields as a model_config.json item.
