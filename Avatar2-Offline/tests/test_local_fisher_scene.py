import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PARK = (ROOT / "client-src/LocalPark258.java").read_text(encoding="utf-8")
HOOK = (ROOT / "client-src/hr.java").read_text(encoding="utf-8")
HUB = (ROOT / "client-src/LocalFishingHub258.java").read_text(encoding="utf-8")
ACTION = (ROOT / "client-src/LocalFishingAction258.java").read_text(encoding="utf-8")


class FarmStyleFishingShopTest(unittest.TestCase):
    def test_select_hook_preserves_working_farm_path_first(self):
        self.assertIn("if (LocalFarmHub258.active)", HOOK)
        self.assertIn("LocalFarmHub258.select()", HOOK)
        self.assertLess(HOOK.index("LocalFarmHub258.select()"), HOOK.index("LocalFishingHub258.select()"))

    def test_map_13_uses_same_local_select_hook_pattern(self):
        compact = HOOK.replace(" ", "")
        self.assertIn("if(ae.b==13)", compact)
        self.assertIn("LocalFishingHub258.select()", HOOK)
        self.assertIn("le.a().a(menu, 0)", HUB)

    def test_shop_opens_directly_through_native_client_shop_screen(self):
        compact = HUB.replace(" ", "")
        self.assertIn('ae.b().a((byte)0,10,"Câu cá",SHOP_IDS,-1,null)', compact)
        self.assertNotIn("cx.a().h(FISHER_NPC)", compact)

    def test_authoritative_fishing_shop_ids_are_preserved(self):
        compact = HUB.replace(" ", "").replace("\n", "")
        self.assertIn("442,443,445,446,447,448,458,459,460", compact)

    def test_bad_custom_npc_scene_injection_is_removed(self):
        self.assertNotIn("createFisher", PARK)
        self.assertNotIn("users.addElement(createFisher())", PARK)
        self.assertNotIn("3387", PARK)
        self.assertNotIn("FishingClientParts258", PARK)

    def test_purchase_action_still_uses_existing_native_shop_buy_path(self):
        self.assertIn("LocalFishingHub258.openShop()", ACTION)


if __name__ == "__main__":
    unittest.main()
