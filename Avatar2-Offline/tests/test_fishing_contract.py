import importlib.util
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


contract = load("fishing_contract", ROOT / "tools" / "fishing_contract.py")
importer = load("import_server_source", ROOT / "tools" / "import_server_source.py")


class FishingContractTest(unittest.TestCase):
    def test_expected_maps_and_fisher(self):
        self.assertEqual(contract.FISHING_HUB_MAP, 13)
        self.assertEqual(contract.FISHING_MAPS, (14, 15, 16))
        self.assertEqual(contract.FISHER_BASE_ID, 430)
        self.assertEqual(contract.FISHER_CLIENT_ID, 2000000430)
        self.assertEqual(contract.FISHER_POSITION, (326, 64))
        self.assertEqual(contract.FISHER_PARTS, (3387, 3386, 0, 82, 3385, 3384, 442))

    def test_map_requirements_match_server(self):
        self.assertEqual(contract.MAP_REQUIREMENTS[14], {"rods": (442, 445, 446), "ticket": 458, "bait": 443})
        self.assertEqual(contract.MAP_REQUIREMENTS[15], {"rods": (445, 446), "ticket": 459, "bait": 447})
        self.assertEqual(contract.MAP_REQUIREMENTS[16], {"rods": (446,), "ticket": 460, "bait": 448})

    def test_fish_pool_weights_match_fish_helper(self):
        self.assertEqual(sum(w for _, w in contract.FISH_POOLS[14]), 6)
        self.assertEqual(sum(w for _, w in contract.FISH_POOLS[15]), 6)
        self.assertEqual(sum(w for _, w in contract.FISH_POOLS[16]), 100)
        self.assertEqual(dict(contract.FISH_POOLS[16])[457], 1)
        self.assertEqual(contract.FISH_POINTS[457], 10)

    def test_core_command_values_do_not_drift(self):
        self.assertEqual(contract.COMMANDS["join_park"], 50)
        self.assertEqual(contract.COMMANDS["cast_bait"], 82)
        self.assertEqual(contract.COMMANDS["handle_fishing"], 84)
        self.assertEqual(contract.COMMANDS["fishing_finished"], 85)
        self.assertEqual(contract.COMMANDS["start_fishing"], 86)
        self.assertEqual(contract.COMMANDS["fishing"], 91)

    def test_shop_core_matches_server_database(self):
        self.assertEqual(contract.SHOP_CORE[442], {"xu": 10000, "luong": 0, "days": 7})
        self.assertEqual(contract.SHOP_CORE[445]["luong"], 25)
        self.assertEqual(contract.SHOP_CORE[446]["luong"], 100)
        self.assertEqual(contract.SHOP_CORE[458]["days"], 3)
        self.assertEqual(contract.SHOP_CORE[459]["xu"], 10000)
        self.assertEqual(contract.SHOP_CORE[460]["luong"], 2)

    def test_map_entry_preserves_requested_map(self):
        self.assertEqual(contract.parse_requested_map_and_zone(bytes([13, 2]), 9, 0), (13, 2))
        self.assertEqual(contract.parse_requested_map_and_zone(bytes([16, 255]), 9, 3), (16, 3))
        self.assertEqual(contract.parse_requested_map_and_zone(bytes([13]), 9, 3), (9, 3))

    def test_import_filter_excludes_sensitive_and_binary_files(self):
        self.assertFalse(importer.is_allowed(Path("avatar.sql")))
        self.assertFalse(importer.is_allowed(Path("config.properties")))
        self.assertFalse(importer.is_allowed(Path("database.properties")))
        self.assertFalse(importer.is_allowed(Path("target/Foo.class")))
        self.assertTrue(importer.is_allowed(Path("pom.xml")))
        self.assertTrue(importer.is_allowed(Path("src/main/java/avatar/common/FishHelper.java")))

    def test_importer_copies_only_public_source_subset(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "server"
            (root / "src/main/java/avatar/common").mkdir(parents=True)
            (root / "target").mkdir()
            (root / "pom.xml").write_text("<project/>", encoding="utf-8")
            (root / "src/main/java/avatar/common/FishHelper.java").write_text("class FishHelper {}", encoding="utf-8")
            (root / "config.properties").write_text("password=secret", encoding="utf-8")
            (root / "avatar.sql").write_text("-- private db", encoding="utf-8")
            (root / "target/Foo.class").write_bytes(b"binary")
            stage = Path(tmp) / "stage"
            stage.mkdir()
            copied, skipped = importer.copy_allowed(root, stage)
            self.assertEqual(copied, 2)
            self.assertGreaterEqual(skipped, 3)
            self.assertTrue((stage / "pom.xml").exists())
            self.assertTrue((stage / "src/main/java/avatar/common/FishHelper.java").exists())
            self.assertFalse((stage / "config.properties").exists())
            self.assertFalse((stage / "avatar.sql").exists())


if __name__ == "__main__":
    unittest.main()
