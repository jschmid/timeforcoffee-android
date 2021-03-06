package ch.liip.timeforcoffee.api;

import android.location.Location;
import android.os.Build;
import android.os.Handler;
import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * Created by fsantschi on 08/03/15.
 */
public class Station implements RoutingListener {
    private String name;
    private Location location;
    private String id;
    private float distance;
    private boolean isFavorite;

    private WalkingDistance walkingDistance;
    private Location walkingDistanceLastCoord;
    private LatLng tempStart;
    private LatLng tempEnd;

    private OnDistanceComputedListener onDistanceComputedListener;

    public interface OnDistanceComputedListener {
        void onDistanceComputed(WalkingDistance walkingDistance);
    }

    public void setOnDistanceComputedListener(OnDistanceComputedListener listener) {
        onDistanceComputedListener = listener;
    }

    public Station(String id, String name, float distance, Location location, boolean isFavorite) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.distance = distance;
        this.isFavorite = isFavorite;
    }

    public WalkingDistance getDistanceForDisplay(Location userLocation) {

        if (userLocation == null || location == null || userLocation == null) {
            onDistanceComputedListener.onDistanceComputed(null);
        }

        if (walkingDistance != null) {
            return walkingDistance;
        }

        int directDistance = getDistanceInMeter(userLocation);
        String distanceString = "";
        if (directDistance > 5000) {
            int km = (int) (Math.round((double) (directDistance) / 1000));
            distanceString = km + " km";
            walkingDistance = new WalkingDistance(distanceString, null);
            onDistanceComputedListener.onDistanceComputed(walkingDistance);
            return walkingDistance;
        } else {
            //calculate exact distance
            this.getWalkingDistance(userLocation);
        }
        return null;
    }

    public int getDistanceInMeter(Location userLocation) {
        if (userLocation != null) {
            return (int) userLocation.distanceTo(location);
        }
        return 0;
    }

    private void getWalkingDistance(Location userLocation) {

        if (Build.FINGERPRINT.contains("generic") || location == null || userLocation == null) {
            return;
        }

        LatLng start = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
        LatLng end = new LatLng(this.getLocation().getLatitude(), this.getLocation().getLongitude());

        String key = getWalkingDistanceKey(start, end);
        WalkingDistance distance = WalkingDistanceCache.getInstance().get(key);

        this.walkingDistance = distance;

        if (walkingDistance != null) { //we have a cached walking
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    onDistanceComputedListener.onDistanceComputed(walkingDistance);
                }
            });
        } else {

            tempStart = start;
            tempEnd = end;
            final Station station = this;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Routing routing = new Routing.Builder()
                            .travelMode(Routing.TravelMode.WALKING)
                            .withListener(station)
                            .waypoints(tempStart, tempEnd)
                            .build();
                    routing.execute();
                }
            }, 0);
        }
    }

    private String getWalkingDistanceKey(LatLng start, LatLng end) {

        double startLat = start.latitude;
        double startLng = start.longitude;
        double endLat = end.latitude;
        double endLng = end.longitude;

        String startLatString = String.format("%.4f", startLat);
        String startLngString = String.format("%.4f", startLng);
        String endLatString = String.format("%.4f", endLat);
        String endLngString = String.format("%.4f", endLng);

        return String.format("start:%s;%s;end:%s;%s", startLatString, startLngString, endLatString, endLngString);
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(PolylineOptions mPolyOptions, Route route) {

        String time = route.getDurationText();
        String meters = route.getDistanceText();
        String walkingDistanceString = meters + ", " + time;

        WalkingDistance distance = new WalkingDistance(walkingDistanceString, mPolyOptions);

        String key = getWalkingDistanceKey(tempStart, tempEnd);
        WalkingDistanceCache.getInstance().put(key, distance);

        this.walkingDistance = distance;

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                onDistanceComputedListener.onDistanceComputed(walkingDistance);
            }
        });
    }

    @Override
    public void onRoutingFailure() {
        this.walkingDistanceLastCoord = null;
        onDistanceComputedListener.onDistanceComputed(null);
    }

    @Override
    public void onRoutingCancelled() {
        onDistanceComputedListener.onDistanceComputed(null);
    }


    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public String getId() {
        return id;
    }

    public float getDistance() {
        return distance;
    }

    public boolean getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    @Override
    public boolean equals(Object object) {
        boolean sameSame = false;

        if (object != null && object instanceof Station) {
            sameSame = this.id.equals(((Station) object).getId());
        }

        return sameSame;
    }
}
