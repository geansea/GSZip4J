package com.geansea.gszipdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.geansea.gszip.GsZipEntry;
import com.geansea.gszip.GsZipEntryNode;
import com.geansea.gszip.GsZipFile;
import com.geansea.gszip.util.GsZipUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class UnzipActivity extends AppCompatActivity {
    private static final int FILE_SELECT_CODE = 12345;

    private ListView entryListView;
    private GsZipFile zipFile;
    private GsZipEntryNode folderNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unzip);

        Button openButton = (Button) findViewById(R.id.openButton);
        entryListView = (ListView) findViewById(R.id.listView);
        zipFile = null;

        if (openButton != null) {
            openButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/zip");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(Intent.createChooser(intent, "Choose a zip file"), FILE_SELECT_CODE);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_SELECT_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String path = uri.getPath();
            zipFile = GsZipFile.create(path);
            if (zipFile != null) {
                if (zipFile.needPassword()) {
                    zipFile.setPassword("geanseadex/move_photo");
                }
                folderNode = zipFile.getEntryTree();
                entryListView.setAdapter(new ZipEntryAdapter(this, folderNode));
                entryListView.setOnItemClickListener(new ZipEntryClickListener());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void clickParent() {
        if (folderNode.getParent() != null) {
            folderNode = folderNode.getParent();
            entryListView.setAdapter(new ZipEntryAdapter(this, folderNode));
        } else {
            Toast.makeText(getApplicationContext(), "No parent", Toast.LENGTH_SHORT).show();
        }
    }

    private void clickChild(int index) {
        ArrayList<GsZipEntryNode> children = folderNode.getChildren();
        GsZipEntryNode child = children.get(index);
        GsZipEntry entry = child.getEntry();
        if (child.isFile() && entry != null) {
            boolean crcMatched = false;
            InputStream is = zipFile.getInputStream(entry.getIndex());
            if (is != null) {
                try {
                    int crc = GsZipUtil.getStreamCRC(is);
                    crcMatched = (crc == entry.getCRC());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            UnzipDialog dialog = new UnzipDialog(this, child, crcMatched);
            dialog.show();
        } else {
            folderNode = child;
            entryListView.setAdapter(new ZipEntryAdapter(this, folderNode));
        }
    }

    private class ZipEntryAdapter extends BaseAdapter {
        private final @NonNull LayoutInflater layoutInflater;
        private final @NonNull ArrayList<GsZipEntryNode> list;
        private final @NonNull String folderName;
        private final boolean hasParent;

        ZipEntryAdapter(@NonNull Context context, @NonNull GsZipEntryNode folderNode){
            layoutInflater = LayoutInflater.from(context);
            list = folderNode.getChildren();
            folderName = folderNode.getName();
            hasParent = (folderNode.getParent() != null);
        }

        final class ViewHolder {
            ImageView iconView;
            TextView nameView;
        }

        @Override
        public int getCount() {
            return list.size() + (hasParent ? 1 : 0);
        }

        @Override
        public Object getItem(int position) {
            return "";
        }

        @Override
        public long getItemId(int position) {
            return position - (hasParent ? 1 : 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.list_unzip, null);
                holder = new ViewHolder();
                holder.iconView = (ImageView) convertView.findViewById(R.id.icon);
                holder.nameView = (TextView) convertView.findViewById(R.id.name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            int iconId = R.drawable.ic_back;
            String name = folderName;
            if (!hasParent || position > 0) {
                int index = position - (folderName.isEmpty() ? 0 : 1);
                GsZipEntryNode node = list.get(index);
                GsZipEntry entry = node.getEntry();
                iconId = (node.isFile() ? R.drawable.ic_file : R.drawable.ic_folder);
                name = node.getName() + ((node.isFile() && entry != null && entry.isEncrypted()) ? " *" : "");
            }

            holder.iconView.setImageResource(iconId);
            holder.nameView.setText(name);
            return convertView;
        }
    }

    private class ZipEntryClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (id < 0) {
                clickParent();
            } else {
                clickChild((int) id);
            }
        }
    }
}
