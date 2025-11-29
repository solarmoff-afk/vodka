package com.update.vodka;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.*;

public class MainActivity extends Activity {
    private static final int PICK_QUEST_FILE = 1;
    private static final int EXPORT_ARCHIVE = 2;
    private EditText editTextTitle;
    private File currentArchive;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        editTextTitle = (EditText) findViewById(R.id.editTextTitle);
        Button buttonLoadQuest = (Button) findViewById(R.id.buttonLoadQuest);
        
        buttonLoadQuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        startActivityForResult(Intent.createChooser(intent, "Выберите .quest файл"), PICK_QUEST_FILE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_QUEST_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                processSelectedFile(data.getData());
            }
        } else if (requestCode == EXPORT_ARCHIVE && resultCode == RESULT_OK) {
            if (data != null && currentArchive != null) {
                exportArchiveToLocation(data.getData());
            }
        }
    }
    
    private void processSelectedFile(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            
            String questContent = readStreamWithEncoding(is, "UTF-8");
            if (questContent == null || questContent.trim().isEmpty()) {
                is = getContentResolver().openInputStream(uri);
                questContent = readStreamWithEncoding(is, "windows-1251");
            }
            
            if (questContent == null || questContent.trim().isEmpty()) {
                Toast.makeText(this, "Не удалось прочитать файл", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String userTitle = editTextTitle.getText().toString();
            
            currentArchive = Prelude.createArchive(this, questContent, userTitle);
            
            if (currentArchive != null && currentArchive.exists()) {
                exportArchive();
            } else {
                Toast.makeText(this, "Ошибка создания архива", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка чтения файла", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String readStreamWithEncoding(InputStream is, String encoding) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        	
            reader.close();
            return stringBuilder.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    private void exportArchive() {
        Intent exportIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        exportIntent.addCategory(Intent.CATEGORY_OPENABLE);
        exportIntent.setType("application/zip");
        exportIntent.putExtra(Intent.EXTRA_TITLE, "converted_quest.zip");
        
        startActivityForResult(exportIntent, EXPORT_ARCHIVE);
    }
    
    private void exportArchiveToLocation(Uri destinationUri) {
        try {
            InputStream in = new FileInputStream(currentArchive);
            OutputStream out = getContentResolver().openOutputStream(destinationUri);
            
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            
            in.close();
            out.flush();
            out.close();
            
            if (currentArchive.delete()) {
                Toast.makeText(this, "Архив успешно экспортирован", Toast.LENGTH_SHORT).show();
            }
            
            currentArchive = null;
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка экспорта", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentArchive != null && currentArchive.exists()) {
            currentArchive.delete();
        }
    }
}