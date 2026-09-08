import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "client-src/FishingClientParts258.java").read_text(encoding="utf-8")


class FisherClientAssetsTest(unittest.TestCase):
    def test_dynamic_part_ids_are_exact(self):
        normalized = SOURCE.replace(" ", "")
        self.assertIn("PART_IDS={3384,3385,3386,3387}", normalized)

    def test_fishing_image_range_is_preloaded_locally(self):
        for image_id in range(5608, 5625):
            self.assertIn(str(image_id), SOURCE)
        self.assertIn('"/offline/item_" + size + "/" + imageId + ".png"', SOURCE)

    def test_native_dynamic_part_cache_is_filled(self):
        self.assertIn("fx.j.put(String.valueOf(id), part)", SOURCE)

    def test_native_dynamic_image_cache_is_filled(self):
        self.assertIn("Image.createImage(png, 0, png.length)", SOURCE)
        self.assertIn("fx.i.put(key, new f(image))", SOURCE)

    def test_part_payload_parser_preserves_native_z_order_fields(self):
        self.assertIn("part.j = in.readByte()", SOURCE)
        self.assertIn("part.c = new short[15]", SOURCE)
        self.assertIn("part.d = new byte[15]", SOURCE)
        self.assertIn("part.e = new byte[15]", SOURCE)


if __name__ == "__main__":
    unittest.main()
