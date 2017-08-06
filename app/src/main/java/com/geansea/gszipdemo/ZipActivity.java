package com.geansea.gszipdemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ZipActivity extends AppCompatActivity {
    private static final int FOLDER_SELECT_CODE = 54321;

    private Button packButton;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zip);

        packButton = (Button) findViewById(R.id.packButton);
        editText = (EditText) findViewById(R.id.editText);

        packButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(Intent.createChooser(intent, "Choose a folder"), FOLDER_SELECT_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FOLDER_SELECT_CODE && resultCode == Activity.RESULT_OK) {
            Uri treeUri = data.getData();
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            for (DocumentFile file : pickedDir.listFiles()) {
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
