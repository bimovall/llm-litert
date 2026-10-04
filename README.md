# LiteRT Android

Example Android app showing two ways to run an LLM on-device: the low-level
**LiteRT** interpreter API, and the high-level **LiteRT-LM** API.

## Modules

| Module | API | What it shows |
| --- | --- | --- |
| `app` | — | Host app. Entry point is `MainActivity`. |
| `feature:starter` | LiteRT `2.1.6` | Running SmolLM-135M by hand — your own GPT-2 tokenizer, model signatures and KV cache. |
| `feature:litertlm` | LiteRT-LM `0.16.0` | Chat UI with token streaming and reasoning output, with tokenization and KV cache handled by the library. Tested with Qwen3-0.6B and Gemma-4-E2B-it. |

The two feature modules solve the same problem at different levels. `starter` is
useful for understanding what actually happens during inference; `litertlm` is
what you'd build on.

See [`feature/litertlm/README.md`](feature/litertlm/README.md) for the LiteRT-LM
details and known issues.

## Requirements

- Android Studio, JDK 11
- `minSdk` 24, `compileSdk` 37
- AGP 9.2.1, Kotlin 2.4.10

## Running

`MainActivity` shows `PromptScreen` from `feature:litertlm`. Swap it for
`ChatScreen` to run the `feature:starter` sample instead.

**Model files are not in this repo** — you need to supply your own:

- **`feature:litertlm`** — pick a model file from device storage at runtime via
  the menu in the top app bar. It's copied into app storage on first use, so you
  only do this once per model.
- **`feature:starter`** — expects `SmolLM-135M-Instruct_seq128_q8_ekv1280.tflite`
  in `feature/starter/src/main/assets/`. Download it and drop it there before
  building, or the module will fail at startup.

Models used here come from [litert-community](https://huggingface.co/litert-community)
on Hugging Face — [Qwen3-0.6B](https://huggingface.co/litert-community/Qwen3-0.6B)
and [Gemma-4-E2B-it](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm).
See the [LiteRT-LM Android guide](https://developers.google.com/edge/litert-lm/android)
for supported formats.

> Models are 100 MB–several GB. Keep them out of git.
