final class hr implements ii {
    hr(ir unused) {}

    public final void a() {
        if (LocalFarmHub258.active) {
            LocalFarmHub258.select();
            return;
        }
        // ae.b is the current park/map id, set by the native scene builder.
        if (ae.b == 13) {
            LocalFishingHub258.active = true;
            LocalFishingHub258.select();
            return;
        }
        LocalFishingHub258.active = false;
        ir.c();
    }
}
