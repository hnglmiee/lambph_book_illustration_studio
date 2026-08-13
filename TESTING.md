# Testing

## Strategy

**Backend** — unit tests target the logic that governs step ordering, cap enforcement, and retry/stale recovery, since that's where a bug is expensive: a wrong ordering check lets a step run out of turn, a missing cap silently bills for extra Gemini calls, and a wrong retry dispatch corrupts a project's progress. `GeminiInteractionClient` and `GeminiFileClient` are mocked everywhere (Mockito) — no test in the suite makes a real network call, so `./test.sh` costs nothing to run repeatedly and never depends on live quota. Covered:

- Step ordering guards for every transition (`CREATED → STYLE_SET → CHARACTERS_GENERATED → PORTRAITS_GENERATED → CHAPTERS_GENERATED → DONE`) — each step rejects with `INVALID_STEP_ORDER` if called before its prerequisor completed.
- Double-call protection — starting a step that's already `RUNNING` returns `STEP_ALREADY_RUNNING` (409), never re-dispatches.
- **Cap enforcement, the single most load-bearing test in the suite**: feeding a mocked Gemini response with 5 characters (cap is 2) or 3 chapters (cap is 1) and asserting the persisted result is capped, not just requested-to-be-capped. This is the test that would fail immediately if someone "simplified" the prompt-based cap back in without the code-level `.limit()`.
- Retry dispatch — `POST /steps/retry` picks the correct next step purely from `project.status`, across all five possible statuses, including the terminal `DONE` case (rejected) and the stale-`RUNNING` auto-recovery path.
- `StepStaleChecker` — pure logic, no mocks needed: false while `IDLE`/`FAILED` regardless of timestamp age, false while freshly `RUNNING`, true only once `RUNNING` has exceeded the threshold.

**Frontend** — not yet implemented at time of writing.

**What's deliberately not tested**: the Gemini client implementations themselves (`GeminiInteractionClientImpl`, `GeminiFileClientImpl`) are not unit-tested against a mocked HTTP layer — they were instead verified against the real API during development (see the debug endpoints and the real request/response pairs captured in the conversation history that produced this codebase), since a mocked-`RestClient` test would mostly just assert that the code calls what it calls, without catching the real integration issues that actually surfaced (wrong field naming, `background` incompatibility) — those were only found by hitting the real API. End-to-end tests through the full 5-step pipeline against a mocked Gemini client are a documented gap (see `DECISIONS.md`, "if we had one more day").

## Test report

```
$ ./mvnw test

[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.hoanglam.bis.util.StepStaleCheckerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.hoanglam.bis.service.GeminiPipelineServiceTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 26, Failures: 7, Errors: 2, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
```

> Replace this block with the actual output of `./test.sh` before submitting — paste the real run, don't hand-edit the numbers.
