package com.example.uber3.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uber3.R;
import com.example.uber3.network.model.favorite.FavoriteRouteResponse;

import java.util.List;

public class FavoriteRoutesAdapter
        extends RecyclerView.Adapter<FavoriteRoutesAdapter.VH> {

    public interface OnFavoriteClick {
        void onClick(FavoriteRouteResponse route);
    }

    private final List<FavoriteRouteResponse> routes;
    private final OnFavoriteClick listener;

    public FavoriteRoutesAdapter(
            List<FavoriteRouteResponse> routes,
            OnFavoriteClick listener
    ) {
        this.routes = routes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_route, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        FavoriteRouteResponse r = routes.get(i);

        h.tvRoute.setText(
                r.from.address + " → " + r.to.address
        );

        if (r.stops != null && !r.stops.isEmpty()) {
            StringBuilder sb = new StringBuilder("Stops: ");

            for (int j = 0; j < r.stops.size(); j++) {
                sb.append(r.stops.get(j).address);
                if (j < r.stops.size() - 1) sb.append(", ");
            }

            h.tvStops.setText(sb.toString());
            h.tvStops.setVisibility(View.VISIBLE);
        } else {
            h.tvStops.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v ->
                listener.onClick(r));
    }

    @Override
    public int getItemCount() {
        return routes.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvRoute;
        TextView tvStops;

        VH(View v) {
            super(v);
            tvRoute = v.findViewById(R.id.tvRoute);
            tvStops = v.findViewById(R.id.tvStops);
        }
    }
}
