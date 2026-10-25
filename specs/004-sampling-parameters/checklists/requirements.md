# Requirements Checklist: Per-request Sampling Parameters

- [x] Request options are optional and use nullable wrapper types.
- [x] Null uses configured values; absent configuration leaves provider defaults in effect.
- [x] Ranges and field-specific 400 behavior are explicit.
- [x] Invalid requests are rejected before model invocation.
- [x] `temperature: 17` behavior is explicitly rejection, with no clamping.
- [x] `numPredict` abuse scenario and 2048 application cap are justified.
- [x] `/api/v1` and existing response contract remain additive.
- [x] No changes to specs 001 or 003.
- [ ] Automated coverage for validation and option resolution is added.
