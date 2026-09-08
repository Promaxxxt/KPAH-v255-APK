# Project status

## Stable baseline

Use V122 as the known-good baseline for future work:

`Avatar2_Offline_V122_FARM_CARE_NATIVE_CALLBACK_FIX.jar`

Known stable area: farm flow from the V122 line.

## Do not use as baseline

- V124 anchor-map experiment: abandoned.
- V125 fishing full experiment: NPC/shop/fishing flow did not function correctly in the real client.
- V126 native-protocol experiment: protocol/resources were partially corrected but has not been validated end-to-end in the user's emulator.

## Current objective

Rebuild fishing from source with the server implementation as the authoritative behavior reference and the actual client only as a protocol/render compatibility reference.

Fishing maps:

- 13: ecological/fishing hub
- 14: ca ro area
- 15: ca loc area
- 16: ca map area

## Source-first progress

- Confirmed `avatar2-fishing` is based on the V122 policy rather than V125/V126.
- Isolated the V122 -> later map-entry compatibility delta: `sendJoinPark` must preserve the requested map byte instead of discarding it.
- Added `client-reference/FISHING_PROTOCOL_REFERENCE.md` so experimental command/resource observations are documented without turning V126 into the baseline.
- Added `tools/import_server_source.py` to import only Java/Maven source from `server.rar` or an extracted server directory while excluding raw SQL, local configuration, binaries and build output.
- Added automated fishing contract/import-safety tests and wired them into CI.
- Fishing gameplay is **not yet complete**. NPC/shop/cast/catch behavior must still be reconstructed from the imported server source and pass the acceptance sequence below.

## Server classes to inspect first

After `server-src` is imported, trace the source in this order:

1. `avatar/common/FishHelper.java`
2. `avatar/handler/NpcHandler.java`
3. `avatar/manager/NpcManager.java`
4. `avatar/model/NpcShop.java` and `NpcShopItem.java`
5. `avatar/map/MapService.java`, `MapManager.java`, `MapID.java`, `Zone.java`
6. `avatar/service/ParkService.java` and `avatar/handler/ParkMsgHandler.java`
7. persistence classes reached by the fishing flow

## Source-first workflow

1. Import sanitized extracted server source.
2. Identify the exact classes responsible for map entry, NPC creation, shop, purchase, fishing start, cast, catch result, sale and persistence.
3. Mirror required logic in offline source.
4. Add automated packet/asset checks.
5. Build one test JAR.
6. Validate the complete acceptance test before merging.

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
