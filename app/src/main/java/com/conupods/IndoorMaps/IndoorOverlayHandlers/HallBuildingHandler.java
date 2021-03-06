/**
 * @author: Patricia Nunes
 */
package com.conupods.IndoorMaps.IndoorOverlayHandlers;

import com.conupods.IndoorMaps.ConcreteBuildings.HBuilding;
import com.conupods.IndoorMaps.IndoorBuildingOverlays;
import com.conupods.OutdoorMaps.Models.Building.Building;
import com.google.android.gms.maps.model.LatLngBounds;

public class HallBuildingHandler extends IndoorOverlayHandler {

    Building hInstance = HBuilding.getInstance();

    /**
     * H building handler
     * if request can't be handled, request is sent to the next in chain
     *
     * @param bounds
     * @param indoorBuildingOverlays
     */
    @Override
    public void checkBounds(LatLngBounds bounds, IndoorBuildingOverlays indoorBuildingOverlays) {
        if (bounds.contains(hInstance.getLatLng())) {
            indoorBuildingOverlays.displayOverlay(IndoorBuildingOverlays.BuildingCodes.H);
            indoorBuildingOverlays.showFloorButtons(IndoorBuildingOverlays.BuildingCodes.H);
        } else {
            if (nextInChain != null) {
                nextInChain.checkBounds(bounds, indoorBuildingOverlays);
            }
        }
    }
}
