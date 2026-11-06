package com.marinainsaurralde.plango.data;

import com.marinainsaurralde.plango.model.Lugar;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PlanIdentity;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.model.PriceStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlanesMerger {
    private PlanesMerger(){ }
    public static List<Plan> mergeTourism(List<? extends Plan> existing,List<? extends Plan> tourism){Map<String,Plan> result=new LinkedHashMap<>();if(existing!=null)for(Plan plan:existing)put(result,plan);if(tourism!=null)for(Plan tourist:tourism){if(tourist==null)continue;String parkId=TurismoNaturalezaEquivalencias.parkIdForTourism(tourist.getSourceId());String parkKey=PlanIdentity.compose(PlanIdentity.PARQUE_MADRID,parkId);Plan park=parkKey==null?null:result.get(parkKey);if(park!=null)result.put(parkKey,mergePlace(park,tourist));else put(result,tourist);}return new ArrayList<>(result.values());}
    private static void put(Map<String,Plan> result,Plan plan){if(plan!=null&&plan.getCompositeId()!=null)result.putIfAbsent(plan.getCompositeId(),plan);}
    static Plan mergePlace(Plan canonical,Plan secondary){Set<PlanType> types=new LinkedHashSet<>(canonical.getPlanTypes());types.addAll(secondary.getPlanTypes());Set<String> categories=new LinkedHashSet<>(canonical.getCategoryKeys());categories.addAll(secondary.getCategoryKeys());Map<String,String> refs=new LinkedHashMap<>(canonical.getSourceRefs());refs.putAll(secondary.getSourceRefs());Double latitude=canonical.hasValidCoordinates()?canonical.getLatitud():secondary.getLatitud();Double longitude=canonical.hasValidCoordinates()?canonical.getLongitud():secondary.getLongitud();PriceStatus price=canonical.getPriceStatus()!=PriceStatus.DESCONOCIDO?canonical.getPriceStatus():secondary.getPriceStatus();return new Lugar(Plan.builder(canonical.getSource(),canonical.getSourceId(),first(canonical.getTitulo(),secondary.getTitulo())).planTypes(types).temporalKind(canonical.getTemporalKind()).categoryKeys(categories).description(first(canonical.getDescription(),secondary.getDescription())).address(first(canonical.getAddress(),secondary.getAddress())).schedule(first(canonical.getSchedule(),secondary.getSchedule())).priceInfo(first(canonical.getPriceInfo(),secondary.getPriceInfo())).priceStatus(price).lastSourceUpdate(first(canonical.getLastSourceUpdate(),secondary.getLastSourceUpdate())).distrito(first(canonical.getDistrito(),secondary.getDistrito())).coordinates(latitude,longitude).nombreInstalacion(first(canonical.getNombreInstalacion(),secondary.getNombreInstalacion())).urlOficial(first(canonical.getUrlOficial(),secondary.getUrlOficial())).sourceRefs(refs));}
    private static String first(String preferred,String fallback){return preferred==null||preferred.trim().isEmpty()?fallback:preferred;}
}
