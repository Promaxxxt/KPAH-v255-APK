public final class LocalFishingAction258 implements ii {
    private byte action;

    public LocalFishingAction258(int action) {
        this.action = (byte) action;
    }

    public void a() {
        if (action == 1) {
            LocalFishingHub258.openShop();
        } else if (action == 2) {
            LocalFishingHub258.sellFish();
        } else if (action == 3) {
            LocalFishingHub258.guide();
        }
    }
}
