# Fishing protocol reference

This file records compatibility observations from the known Avatar2 Offline binaries. It is **not** the source of truth for gameplay rules. The extracted server implementation (`server-src`) remains authoritative.

## Baseline policy

- Stable baseline: `Avatar2_Offline_V122_FARM_CARE_NATIVE_CALLBACK_FIX.jar`.
- V125/V126 are reference-only and must never become the build baseline.
- Do not copy an entire experimental fishing implementation into the clean source tree.

## Map-entry delta isolated from V122 -> V126

The `AvatarProtocol.sendJoinPark(byte[])` bytecode differs at one relevant instruction before the fishing code is dispatched:

- V122 reads the first request byte and discards it (`pop`).
- V126 stores that first request byte into the local `mapId` (`istore_2`).
- The following byte remains the requested zone.

Clean source behavior to preserve:

```text
requested map byte -> player.mapId
requested zone byte (when >= 0) -> player.zone
```

This is sufficient to explain why the V122 line cannot route a requested transition into fishing maps while the later experiment can.

## Fishing maps

| Map | Purpose |
| ---: | --- |
| 13 | Khu Sinh Thai / fishing hub |
| 14 | Ca ro area |
| 15 | Ca loc area |
| 16 | Ca map area |

## Client command contract observed in V126

These values are compatibility references to verify against server source before implementation:

| Command | Observed role |
| ---: | --- |
| `50` | park/map join request intercepted for maps 13-16 |
| `-61` | fisherman NPC interaction/open menu |
| `-59` | fisherman menu action |
| `-49` | open native shop response |
| `-24` | native shop purchase |
| `82` | cast-bait flow |
| `84` | fishing inventory/action flow |
| `85` | fishing action flow |
| `86` | start-fishing response; V126 writes a boolean and UTF text |
| `91` | arrow mini-game/result step |

The exact payload layout for every command must be confirmed against `FishHelper`, `NpcHandler`, `NpcShop`, and the relevant server services before being treated as final.

## Fisher NPC compatibility observations

- Experimental NPC id: `2000000430`.
- Experimental NPC key/name: `tho.cau`.
- Referenced dynamic parts: `3384`, `3385`, `3386`, `3387`.

These identifiers should be checked against server source rather than hard-coded from this file alone.

## Resource observations

V126 adds client-side fishing references absent from V122:

- arrow images `offline/arrows_hd/0.png` ... `35.png`
- arrow images `offline/arrows_medium/0.png` ... `35.png`
- fisherman part data `offline/part/3384.dat` ... `3387.dat`
- fishing item/part images in the `761-785` and `5608-5624` ranges

Asset presence is necessary but not sufficient: NPC part metadata and packet layout must agree with the original client renderer or the client can spin/hang even when PNG files exist.

## Acceptance rule

A change is not considered complete until the repository acceptance sequence passes end-to-end: login, map 13, visible/interactable fisherman, menu, shop, purchase, maps 14-16, sitting, cast + native arrow mini-game, catch persistence, fish sale, and persistence after restart.
