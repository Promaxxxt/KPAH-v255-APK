import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "client-src/LocalPark258.java").read_text(encoding="utf-8")


class LocalFisherSceneTest(unittest.TestCase):
    def test_map_13_injects_fisher_into_render_entity_vector(self):
        self.assertIn("if (map == 13)", SOURCE)
        self.assertIn("users.addElement(createFisher())", SOURCE)

    def test_fisher_identity_and_position_match_authoritative_server(self):
        self.assertIn("npc.w = 2000000430", SOURCE)
        self.assertIn("npc.aw = 326", SOURCE)
        self.assertIn("npc.ax = 64", SOURCE)
        self.assertIn("npc.C = 326", SOURCE)
        self.assertIn("npc.D = 64", SOURCE)

    def test_fisher_parts_match_database(self):
        normalized = SOURCE.replace(" ", "")
        self.assertIn("newshort[]{3387,3386,0,82,3385,3384,442}", normalized)

    def test_map_13_spawns_player_beside_fisher_for_visibility(self):
        self.assertIn("case 13: return 300", SOURCE)
        self.assertIn("case 13: return 64", SOURCE)

    def test_client_still_uses_native_scene_builder(self):
        self.assertIn("ae.b().a((byte) map, (byte) zone", SOURCE)


if __name__ == "__main__":
    unittest.main()
