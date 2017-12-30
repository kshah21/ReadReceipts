package com.kshah21.readreceipts.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kshah21.readreceipts.R;


/**
 * Adapter class for RecyclerView used in Drawer
 */
public class DrawerAdapter extends RecyclerView.Adapter<DrawerAdapter.ViewHolder> {

    private String[] dataSet;
    private OnDrawerItemClickListener clickListener;

    /**
     * Custom ViewHolder
     */
    public static class ViewHolder extends RecyclerView.ViewHolder{
        public final TextView textView;

        public ViewHolder(TextView itemView) {
            super(itemView);
            textView = itemView;
        }
    }

    /**
     * Interface for onClick events
     */
    public interface OnDrawerItemClickListener {
        public void onDrawerClick(View view, int position);
    }

    /**
     * Constructor
     */
    public DrawerAdapter(String[] dataSet, OnDrawerItemClickListener clickListener){
        this.dataSet = dataSet;
        this.clickListener = clickListener;
    }

    /**
     * Create ViewHolder views
     */
    @Override
    public DrawerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.drawer_list_item,parent,false);
        TextView textView = view.findViewById(R.id.list_item_text);
        return new ViewHolder(textView);
    }

    /**
     * Bind ViewHolder views
     */
    @Override
    public void onBindViewHolder(DrawerAdapter.ViewHolder holder, final int position) {
        holder.textView.setText(dataSet[position]);
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onDrawerClick(view, position);
            }
        });
    }

    /**
     * Obtain item count
     */
    @Override
    public int getItemCount() {
        return dataSet.length;
    }
}