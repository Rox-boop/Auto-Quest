package eg.core;

import java.util.List;

public abstract class Setting {
    public final String name;
    protected Setting(String name) { this.name = name; }

    public static final class Bool extends Setting {
        private volatile boolean value;
        public Bool(String name, boolean def) { super(name); this.value = def; }
        public boolean get() { return value; }
        public void set(boolean v) { value = v; }
    }

    public static final class Num extends Setting {
        public final double min, max, step;
        private volatile double value;
        public Num(String name, double def, double min, double max, double step) {
            super(name);
            this.min = min; this.max = max; this.step = step;
            set(def);
        }
        public double get() { return value; }
        public int getInt() { return (int) Math.round(value); }
        public void set(double v) {
            double s = step > 0 ? Math.round(v / step) * step : v;
            value = Mth.clamp(s, min, max);
        }
    }

    public static final class Mode extends Setting {
        public final List<String> modes;
        private volatile int index;
        public Mode(String name, int def, String... modes) {
            super(name);
            this.modes = List.of(modes);
            this.index = Math.floorMod(def, this.modes.size());
        }
        public String get() { return modes.get(index); }
        public int index() { return index; }
        public void next() { index = (index + 1) % modes.size(); }
    }
}
