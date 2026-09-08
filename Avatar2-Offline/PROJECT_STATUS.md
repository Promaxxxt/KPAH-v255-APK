# Project status

## Stable baseline

Use V122 as the known-good baseline for future work:

`Avatar2_Offline_V122_FARM_CARE_NATIVE_CALLBACK_FIX.jar`

Known stable area: farm flow from the V122 line.

## Do not use as baseline

- V124 anchor-map experiment: abandoned.
- V125 fishing full experiment: NPC/shop/fishing flow did not function correctly in the real client.
- V126 native-protocol experiment: reference only; do not inherit its ad-hoc fishing serializers.

## Current objective

Rebuild fishing from source with the extracted server implementation as authoritative behavior reference and V122 as the stable client/offline baseline.

Fishing maps:

- 13: ecological/fishing hub
- 14: cá rô area
- 15: cá lóc area
- 16: cá mập area

## Source-first progress

- `server.rar` was successfully extracted as RAR5 using system libarchive; the archive contains the real Maven server source.
- Added `tools/extract_rar5.py` so the extraction path is reproducible even when `unrar`/`7z`/`bsdtar` executables are absent.
- Inspected authoritative classes: `FishHelper`, `NpcHandler`, `ParkMsgHandler`, `ParkService`, `AvatarService`, `ItemManager`, `NpcIDs` and `Cmd`.
- Added `client-reference/FISHING_SERVER_CONTRACT.md` with the exact NPC, command, map requirement, fish-pool and shop contract from source/database.
- Confirmed fisherman: base id `430`, client id `2000000430`, map `13`, position `(326,64)`, parts `[3387,3386,0,82,3385,3384,442]`.
- Confirmed native menu path is `openMenuOption(...)` and native fishing shop path is `openShopParts(10, "Câu cá", shop10)`.
- Confirmed commands `82/84/85/86/87/88/91` and the server-side fishing flow.
- Updated automated contract tests with exact map requirements, fish weights, points and shop prices.
- Identified a likely V126 failure mode: it intercepted fishing packets before `AvatarProtocol` while reproducing some world/NPC/shop state with custom reduced serializers. New implementation must reuse the native packet shapes instead.

## Authoritative fishing requirements

- Map 14: rods `442/445/446`, ticket `458`, bait `443`.
- Map 15: rods `445/446`, ticket `459`, bait `447`.
- Map 16: rod `446`, ticket `460`, bait `448`.
- Fish pools and scores are defined in `tools/fishing_contract.py` and locked by tests.

## Next implementation slice

1. Build a clean offline fishing handler against V122-compatible server classes.
2. Reuse the existing V122 park/NPC serializer; inject NPC 430 through that serializer rather than a custom join packet.
3. Reuse the exact `MENU_OPTION` and `OPEN_SHOP` payload shapes from `AvatarService`.
4. Implement start/cast/fish/handle-fishing in source with deterministic tests for packet payloads.
5. Add persistence for fishing inventory/state.
6. Produce one test JAR only after compile/tests pass.

## Fishing acceptance test

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
