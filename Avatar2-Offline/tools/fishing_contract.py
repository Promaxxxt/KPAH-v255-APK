#!/usr/bin/env python3
"""Authoritative source-first fishing compatibility contract."""

FISHING_HUB_MAP = 13
FISHING_MAPS = (14, 15, 16)
FISHER_BASE_ID = 430
NPC_ID_OFFSET = 2_000_000_000
FISHER_CLIENT_ID = NPC_ID_OFFSET + FISHER_BASE_ID
FISHER_POSITION = (326, 64)
FISHER_PARTS = (3387, 3386, 0, 82, 3385, 3384, 442)

COMMANDS = {
    "join_park": 50,
    "cast_bait": 82,
    "handle_fishing": 84,
    "fishing_finished": 85,
    "start_fishing": 86,
    "status_fish": 87,
    "info_fish": 88,
    "fishing": 91,
}

MAP_REQUIREMENTS = {
    14: {"rods": (442, 445, 446), "ticket": 458, "bait": 443},
    15: {"rods": (445, 446), "ticket": 459, "bait": 447},
    16: {"rods": (446,), "ticket": 460, "bait": 448},
}

FISH_POOLS = {
    14: ((444, 1), (449, 1), (450, 1), (-1, 3)),
    15: ((451, 1), (452, 1), (453, 1), (-1, 3)),
    16: ((454, 16), (455, 17), (456, 18), (457, 1), (-1, 48)),
}

FISH_POINTS = {
    444: 1, 449: 1, 450: 1,
    451: 2, 452: 2, 453: 2,
    454: 3, 455: 3, 456: 3,
    457: 10,
}

SHOP_CORE = {
    442: {"xu": 10_000, "luong": 0, "days": 7},
    443: {"xu": 5, "luong": 0, "days": 0},
    445: {"xu": 0, "luong": 25, "days": 20},
    446: {"xu": 0, "luong": 100, "days": 30},
    447: {"xu": 20, "luong": 0, "days": 0},
    448: {"xu": 30, "luong": 0, "days": 0},
    458: {"xu": 1_000, "luong": 0, "days": 3},
    459: {"xu": 10_000, "luong": 0, "days": 5},
    460: {"xu": 0, "luong": 2, "days": 2},
}

ARROW_IDS = tuple(range(36))


def is_fishing_map(map_id: int) -> bool:
    return map_id in FISHING_MAPS


def parse_requested_map_and_zone(payload: bytes, default_map: int, default_zone: int) -> tuple[int, int]:
    map_id = default_map
    zone = default_zone
    if len(payload) >= 2:
        map_id = payload[0] if payload[0] < 128 else payload[0] - 256
        requested_zone = payload[1] if payload[1] < 128 else payload[1] - 256
        if requested_zone >= 0:
            zone = requested_zone
    return map_id, zone
