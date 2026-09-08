#!/usr/bin/env python3
"""Small source-first fishing compatibility contract shared by tests/tools."""

FISHING_MAPS = (13, 14, 15, 16)

# Compatibility observations from the experimental client. Confirm payloads
# against server source before implementing or changing behavior.
COMMANDS = {
    "join_park": 50,
    "npc_open": -61,
    "npc_menu_action": -59,
    "open_shop": -49,
    "buy_item": -24,
    "cast_bait": 82,
    "fishing_action_84": 84,
    "fishing_action_85": 85,
    "start_fishing": 86,
    "arrow_result": 91,
}

FISHER_PARTS = (3384, 3385, 3386, 3387)
ARROW_IDS = tuple(range(36))


def is_fishing_map(map_id: int) -> bool:
    return map_id in FISHING_MAPS


def parse_requested_map_and_zone(payload: bytes, default_map: int, default_zone: int) -> tuple[int, int]:
    """Mirror the clean V123+ map-entry delta isolated from bytecode.

    The first byte is requested map id. The second byte is a signed zone; a
    negative value leaves the default zone unchanged.
    """
    map_id = default_map
    zone = default_zone
    if len(payload) >= 2:
        map_id = payload[0] if payload[0] < 128 else payload[0] - 256
        requested_zone = payload[1] if payload[1] < 128 else payload[1] - 256
        if requested_zone >= 0:
            zone = requested_zone
    return map_id, zone
