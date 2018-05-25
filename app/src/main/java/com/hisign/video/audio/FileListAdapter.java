package com.hisign.video.audio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hisign.video.R;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/25
 */

class FileListAdapter extends BaseAdapter{

    private Context mContext;

    private List<File> list;

    public FileListAdapter(Context mContext, List<File> list) {
        this.mContext = mContext;
        this.list = list;
    }

    @Override
    public int getCount() {
        if (list != null){
            return list.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.adapter_audio_list, null);
            convertView.setTag(viewHolder);
            viewHolder.name = (TextView) convertView.findViewById(R.id.adapter_file_list_name);
            viewHolder.size = (TextView) convertView.findViewById(R.id.adapter_file_list_create_size);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.name.setText(list.get(position).getName());
        viewHolder.size.setText(formatFileSize(list.get(position).length()));

        return convertView;
    }

    public class ViewHolder{
        TextView name;
        TextView size;
    }

    public String formatFileSize(long fileSize){
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileSize < 1024){
            fileSizeString = df.format((double) fileSize) +"B";
        }else if (fileSize < 1048576){//1024*1024
            fileSizeString = df.format((double) fileSize/1024)+"K";
        }else if (fileSize < 1073741824){//1024*1024*1024
            fileSizeString = df.format((double) fileSize/1048576)+"M";
        }else {
            fileSizeString = df.format((double) fileSize/1073741824)+"G";
        }
        return fileSizeString;
    }
}
