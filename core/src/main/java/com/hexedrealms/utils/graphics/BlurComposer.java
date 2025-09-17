package com.hexedrealms.utils.graphics;

public class BlurComposer {
    public static float calculateBlurFactor(int refreshRate) {
        // Логика расчета blur factor
        switch (refreshRate) {
            case 60:  // Стандартный 60 Гц монитор
                return 0.2f;
            case 120: // 120 Гц монитор
                return 0.3f;
            case 144: // 144 Гц монитор
                return 0.4f;
            case 240: // 240 Гц монитор
                return 0.5f;
            default:
                // Для нестандартных частот - средний вариант
                return Math.max(0.1f, Math.min(0.5f, 1f / refreshRate));
        }
    }
}
