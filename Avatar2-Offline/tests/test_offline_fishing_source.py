import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "offline-src/offline/server/FishingProtocol.java").read_text(encoding="utf-8")
LOOP = (ROOT / "offline-src/offline/server/AvatarOfflineServer.java").read_text(encoding="utf-8")


class OfflineFishingSourceTest(unittest.TestCase):
    def test_fishing_handler_runs_before_existing_handlers(self):
        self.assertIn("FishingProtocol.handle(codec, out, player, world, resources, hooks, packet)", LOOP)
        self.assertLess(LOOP.index("FishingProtocol.handle"), LOOP.index("FarmProtocol.handle"))

    def test_native_fisher_and_shop_serializers(self):
        self.assertIn("FISHER_NPC = 2000000430", SOURCE)
        self.assertIn("ds.writeByte(10); ds.writeUTF(\"Câu cá\")", SOURCE)
        self.assertIn("send(codec, out, -59", SOURCE)
        self.assertIn("send(codec, out, -49", SOURCE)

    def test_buying_rod_sets_worn_rod_and_broadcasts_using_part(self):
        self.assertIn("s.wornRod = id", SOURCE)
        self.assertIn("sendUsingPart(codec, out, player.id, id)", SOURCE)
        self.assertIn("send(codec, out, -48", SOURCE)

    def test_start_requires_worn_rod_not_owned_rod(self):
        self.assertIn("validRod(player.mapId, s.wornRod)", SOURCE)
        self.assertNotRegex(SOURCE, r"validRod\(player\.mapId,\s*s\.get")

    def test_rms_persists_worn_rod(self):
        self.assertIn("ds.writeShort(s.wornRod)", SOURCE)
        self.assertIn("s.wornRod=in.readShort()", SOURCE.replace(" ", ""))

    def test_no_v126_prefilled_rods_or_bait(self):
        state_match = re.search(r"static final class State \{(.+?)\n    \}\n\}", SOURCE, re.S)
        self.assertIsNotNone(state_match)
        state = state_match.group(1)
        self.assertNotIn("set(442,1)", state.replace(" ", ""))
        self.assertNotIn("set(445,1)", state.replace(" ", ""))
        self.assertNotIn("set(446,1)", state.replace(" ", ""))
        self.assertNotIn("set(443,30)", state.replace(" ", ""))


if __name__ == "__main__":
    unittest.main()
