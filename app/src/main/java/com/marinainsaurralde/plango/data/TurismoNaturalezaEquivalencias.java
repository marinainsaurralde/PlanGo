package com.marinainsaurralde.plango.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TurismoNaturalezaEquivalencias {
    private static final Map<String,String> TOURISM_TO_PARK;
    static {
        Map<String,String> values=new LinkedHashMap<>();
        values.put("69383","6010175"); values.put("85866","6010463");
        values.put("69396","6011769"); values.put("76933","5978133");
        values.put("76594","5969105"); values.put("69955","5992502");
        values.put("100608","7767308"); values.put("78421","5989658");
        values.put("69857","5982087"); values.put("69423","5982119");
        values.put("69422","5965783"); values.put("69869","5987836");
        values.put("102136","5996223"); values.put("102282","50215260");
        values.put("74894","7642234");
        TOURISM_TO_PARK=Collections.unmodifiableMap(values);
    }
    private TurismoNaturalezaEquivalencias(){ }
    public static String parkIdForTourism(String tourismId){return TOURISM_TO_PARK.get(tourismId);}
    public static Map<String,String> all(){return TOURISM_TO_PARK;}
}
