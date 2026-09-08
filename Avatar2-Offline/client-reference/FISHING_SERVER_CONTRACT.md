# Fishing server contract

Authoritative reference extracted from the original `server.rar` source. This replaces guesses made from V125/V126 bytecode where the two disagree.

## Fisher NPC

- Base NPC id: `430` (`NpcIDs.THO_CAU`)
- Client-facing id: `2000000430` (`NpcIDs.ID_OFFSET + 430`)
- Name key: `tho.cau`
- Map: `13`
- Position: `(326, 64)`
- Parts: `[3387,3386,0,82,3385,3384,442]`
- Menu serializer: `AvatarService.openMenuOption(...)`
- Menu entries: `Câu cá`, `Bán cá`, `Xem hướng dẫn`, `Thoát`
- Fishing shop serializer: `AvatarService.openShopParts(10, "Câu cá", ItemManager.getInstance().getShop10())`

The NPC must be emitted through the same park/NPC serialization shape as normal server NPCs. Do not invent a shortened custom NPC packet.

## Commands

- `50` AVATAR_JOIN_PARK
- `82` CAST_BAIT
- `84` HANDLE_FISHING
- `85` FISHING_FINISHED
- `86` START_FISHING
- `87` STATUS_FISH
- `88` INFO_FISH
- `91` FISHING

## Map requirements

### Map 14 — cá rô

- Wearing rod: any of `442`, `445`, `446`
- Ticket in chest: `458`
- Bait in chest: `443`

### Map 15 — cá lóc

- Wearing rod: `445` or `446`
- Ticket in chest: `459`
- Bait in chest: `447`

### Map 16 — cá mập

- Wearing rod: `446`
- Ticket in chest: `460`
- Bait in chest: `448`

## Fish pools

Source: `avatar/common/FishHelper.java`.

- Map 14 weights: `444:1`, `449:1`, `450:1`, miss `-1:3`.
- Map 15 weights: `451:1`, `452:1`, `453:1`, miss `-1:3`.
- Map 16 weights: `454:16`, `455:17`, `456:18`, `457:1`, miss `-1:48`.

Points:

- `444`, `449`, `450`: 1 point
- `451`, `452`, `453`: 2 points
- `454`, `455`, `456`: 3 points
- `457`: 10 points

## Shop 10 core fishing items

| ID | Name | Xu | Lượng | Duration |
| ---: | --- | ---: | ---: | ---: |
| 442 | cần câu tre | 10,000 | - | 7 days |
| 443 | mồi cơm | 5 | - | consumable |
| 445 | cần câu sắt | - | 25 | 20 days |
| 446 | cần câu VIP | - | 100 | 30 days |
| 447 | mồi trùng | 20 | - | consumable |
| 448 | trứng kiến | 30 | - | consumable |
| 458 | vé câu cá rô | 1,000 | - | 3 days |
| 459 | vé câu cá lóc | 10,000 | - | 5 days |
| 460 | vé câu cá mập | - | 2 | 2 days |

## Native flow

1. Join map 13 through normal park flow and render NPC 430 with normal NPC serialization.
2. Client communicates with `2000000430`.
3. Server opens menu with `MENU_OPTION` via `openMenuOption`.
4. `Câu cá` opens shop 10 via `OPEN_SHOP` / `openShopParts`.
5. Enter map 14/15/16.
6. `START_FISHING` validates hunger, chest capacity, worn rod and valid ticket.
7. `CAST_BAIT` consumes the map-specific bait and schedules a bite after 8–13 seconds.
8. Server sends `FISHING (91)` with player id, fish id, 3000ms or -1, arrow count, then native arrow image bytes.
9. Client answers `HANDLE_FISHING (84)` with boolean, arrow count and direction bytes.
10. Server validates the sequence, adds the fish to chest on success, sends `84` result, and later `85` when fishing finishes.

## Important correction to V126

The experimental V126 implementation got several numeric IDs right, but it should not be treated as authoritative for NPC/map/shop serialization. The source server uses shared native serializers and full item/NPC state. New offline code should mirror those serializers instead of constructing ad-hoc reduced packets.
