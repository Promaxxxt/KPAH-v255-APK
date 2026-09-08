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

## Source-first workflow

1. Import sanitized extracted server source.
2. Identify the exact classes responsible for map entry, NPC creation, shop, purchase, fishing start, cast, catch result, sale and persistence.
3. Mirror required logic in offline source.
4. Add automated packet/asset checks.
5. Build one test JAR.
6. Validate the complete acceptance test before merging.
