# Avatar2 Offline

Workspace for rebuilding the Avatar2 offline client from source instead of repeatedly patching JAR bytecode.

## Baseline

- Stable gameplay baseline: `Avatar2_Offline_V122_FARM_CARE_NATIVE_CALLBACK_FIX.jar`
- Fishing-map entry work must branch from the stable baseline, not from the broken V125/V126 experiments.
- Reference server source: `server.rar` from the ChatGPT project files. The archive must be extracted and sanitized before committing because this repository is public.

## Development rules

1. Do not patch the release JAR directly unless diagnosing client protocol behavior.
2. Implement features in source and build a fresh JAR.
3. Keep feature work isolated (`fishing`, `farm`, `shops`, etc.).
4. Do not claim a feature works until its end-to-end acceptance test passes.
5. Never commit passwords, database credentials, private IPs, tokens, or local machine paths.

## Fishing acceptance test

The fishing feature is complete only when all of these pass in order:

1. Login succeeds.
2. Enter map 13 (Khu Sinh Thai).
3. Fisher NPC visibly renders and can be interacted with.
4. Fisher menu opens without loading/spinning forever.
5. Fishing shop opens and lists the expected rods, bait and tickets.
6. Buy operations return normally and inventory updates.
7. Enter maps 14, 15 and 16.
8. Sit at a valid fishing spot without hanging.
9. Start fishing, cast, receive the native arrow mini-game, submit input and get a result.
10. Caught fish is persisted.
11. Selling fish updates currency.
12. Exit/reopen the game and verify fishing data persists.

## Target layout

```text
Avatar2-Offline/
├── server-src/        # sanitized extracted server.rar source
├── offline-src/       # offline implementation
├── client-reference/  # text/decompiled references only; avoid copyrighted binary duplication when unnecessary
├── resources/
├── database/
├── tests/
├── tools/
└── builds/            # generated locally; do not commit binaries by default
```
