package org.example.util;

import java.util.Set;

public final class HospitalOptions {
    private static final Set<String> ALLOWED_HOSPITALS = Set.of(
            "都江堰医院",
            "成都市医院",
            "都江堰市人民医院",
            "都江堰市中医医院",
            "成都市第一人民医院",
            "成都市第二人民医院",
            "成都市第三人民医院"
    );

    private HospitalOptions() {
    }

    public static boolean isAllowedHospital(String hospitalName) {
        return hospitalName != null && ALLOWED_HOSPITALS.contains(hospitalName.trim());
    }
}
