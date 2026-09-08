# Project status

## Stable baseline

Use V122 as the known-good baseline for future work:

`Avatar2_Offline_V122_FARM_CARE_NATIVE_CALLBACK_FIX.jar`

Known stable area: farm flow from the V122 line.

## Do not use as baseline

- V124 anchor-map experiment: abandoned.
- V125 fishing full experiment: NPC/shop/fishing flow did not function correctly in the real client.
- V126 native-protocol experiment: reference only; do not inherit its ad-hoc fishing state model.

## Current objective

Rebuild fishing from source with the extracted server implementation as authoritative behavior reference and V122 as the stable client/offline baseline.

Fishing maps:

- 13: ecological/fishing hub
- 14: cá rô area
- 15: cá lóc area
- 16: cá mập area

## Source-first progress

- `server.rar` was successfully extracted as RAR5 using system libarchive.
- Added `tools/extract_rar5.py` for reproducible extraction without command-line `unrar`/`7z`.
- Inspected authoritative `FishHelper`, `NpcHandler`, `ParkMsgHandler`, `ParkService`, `AvatarService`, `ItemManager`, `NpcIDs` and `Cmd` behavior.
- Added `client-reference/FISHING_SERVER_CONTRACT.md` with exact NPC, commands, map requirements, fish pools and shop contract.
- Fisherman is base id `430`, client id `2000000430`, map `13`, position `(326,64)`, parts `[3387,3386,0,82,3385,3384,442]`.
- Added clean `offline-src/offline/server/FishingProtocol.java` and a V122-compatible `AvatarOfflineServer.java` routing layer.
- Menu/shop responses use the native `MENU_OPTION` / `OPEN_SHOP` shapes.
- Buying rods `442/445/446` now updates a persisted `wornRod` and broadcasts native `USING_PART (-48)` instead of merely incrementing an isolated item counter.
- Fishing inventory, worn rod, score/revenue and caught fish are persisted together in RMS.
- Start/cast/result commands follow authoritative server commands `82/84/85/86/87/88/91`.
- Fishing resources added to the test build were checksum-verified against `server.rar`.
- Local packet smoke passed: map13 join -> fisherman -> menu -> shop -> buy/wear rod -> buy ticket/bait -> map14 join -> start fishing -> cast bait.
- Generated local test build: `Avatar2_Offline_V127_FISHING_SOURCE_NATIVE_STATE.jar` from V122. ZIP integrity check passes; replacement classes retain J2ME class version 45.3.
- GitHub CI includes authoritative contract checks and source regression checks.

## Authoritative fishing requirements

- Map 14: rods `442/445/446`, ticket `458`, bait `443`.
- Map 15: rods `445/446`, ticket `459`, bait `447`.
- Map 16: rod `446`, ticket `460`, bait `448`.
- Fish pools and scores are defined in `tools/fishing_contract.py` and locked by tests.

## Remaining real-client acceptance

The packet-level/source tests pass, but the following must still be validated in the user's J2ME emulator before calling fishing complete:

1. Login succeeds.
2. Enter map 13 (Khu Sinh Thai).
3. Fisher NPC visibly renders and can be interacted with.
4. Fisher menu opens without loading/spinning forever.
5. Fishing shop opens and lists rods, bait and tickets.
6. Buy operations return normally and the bought rod visibly equips.
7. Enter maps 14, 15 and 16.
8. Sit at a valid fishing spot without hanging.
9. Start fishing, cast, receive the native arrow mini-game, submit input and get a result.
10. Caught fish persists.
11. Selling fish updates currency.
12. Exit/reopen the game and verify fishing data persists.
