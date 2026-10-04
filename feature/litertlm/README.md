# feature:litertlm

Chat screen built on **LiteRT-LM** (`com.google.ai.edge.litertlm:litertlm-android:0.16.0`).

Unlike [`feature:starter`](../starter), the library handles tokenization, the
chat template and the KV cache. You give it a model path and a prompt.

## What it demonstrates

- Loading a model picked from device storage at runtime
- Token-by-token streaming into a Compose UI
- Separating the model's reasoning output from its answer
- Hilt-scoped engine ownership (one `Engine` per process, not per screen)

## Structure

```
data/service/LlmEngineService.kt   Owns the Engine + Conversation, exposes Flow<ChatProvider>
data/service/ModelService.kt       Imports a model Uri into app storage
data/model/ChatProvider.kt         Stream element: Thinking or Message
presentation/prompt/               ViewModel, UI state, screen
presentation/component/            Bubble, input field, model popup
```

The service layer keeps every `com.google.ai.edge.litertlm` type inside `data/`.
The ViewModel only ever sees `ChatProvider`, which keeps it testable — the SDK's
`Conversation` is a final class wrapping a native handle and cannot be faked.

## Models

Download a `.litert-lm` model and put it on the device (Downloads is fine):

- **Qwen3-0.6B** — https://huggingface.co/litert-community/Qwen3-0.6B
- **Gemma-4-E2B-it** — https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm

## Loading a model

Tap the overflow menu → **Load model from storage** and pick the file.

`ModelService.import` copies it to `filesDir/models/<name>`
(write-to-`.part`-then-rename, so an interrupted copy can't be mistaken for a
complete one), then `LlmEngineService.getOrLoadModel` initialises the `Engine`
from that path. The copy only happens once per model.

The copy is necessary because `EngineConfig` takes a filesystem path, and a SAF
`content://` URI isn't one.
