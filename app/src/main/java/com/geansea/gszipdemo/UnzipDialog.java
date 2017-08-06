package com.geansea.gszipdemo;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.geansea.gszip.GsZipEntry;
import com.geansea.gszip.GsZipEntryNode;

import java.text.SimpleDateFormat;
import java.util.Locale;

class UnzipDialog extends Dialog {
    UnzipDialog(@NonNull Context context, @NonNull GsZipEntryNode node, boolean crcMatched) {
        super(context, true, new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        final ViewGroup nullParent = null;
        View contentView = layoutInflater.inflate(R.layout.dialog_unzip, nullParent);
        addContentView(contentView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(contentView);

        TextView nameView = (TextView) contentView.findViewById(R.id.name);
        TextView sizeView = (TextView) contentView.findViewById(R.id.size);
        TextView crcView = (TextView) contentView.findViewById(R.id.crc);
        TextView timeView = (TextView) contentView.findViewById(R.id.time);

        GsZipEntry entry = node.getEntry();
        if (entry != null) {
            String nameText = node.getName();
            String sizeText = String.format(Locale.getDefault(), "%d/%d",
                    entry.getCompressedSize(), entry.getOriginalSize());
            String crcText = String.format(Locale.getDefault(), "%08X %s",
                    entry.getCRC(), crcMatched ? "Pass" : "Fail");
            String timeText = new SimpleDateFormat("yyyy-mm-dd HH:mm", Locale.getDefault()).format(entry.getTime());
            nameView.setText(nameText);
            sizeView.setText(sizeText);
            crcView.setText(crcText);
            timeView.setText(timeText);
        }
    }
}
