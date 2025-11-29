package com.update.vodka;

import android.content.Context;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Prelude {
    public static File createArchive(Context context, String questContent, String userTitle) {
        try {
            File tempDir = context.getCacheDir();
            File archiveFile = new File(tempDir, "converted_quest_" + System.currentTimeMillis() + ".zip");
            
            FileOutputStream fos = new FileOutputStream(archiveFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            
            addConfigJson(context, zos, questContent, userTitle);
            addNodesJson(zos, questContent);
            addPreviewFromAssets(context, zos);
            
            zos.close();
            fos.close();
            
            return archiveFile;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private static void addConfigJson(Context context, ZipOutputStream zos, String questContent, String userTitle) {
        try {
            InputStream is = context.getAssets().open("config.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String configContent = stringBuilder.toString();
            
            JSONObject config = new JSONObject(configContent);
            
            String questId = UUID.randomUUID().toString();
            String timestamp = java.time.LocalDateTime.now().toString();
            
            config.put("id", questId);
            config.put("title", userTitle != null && !userTitle.trim().isEmpty() ? userTitle : "Новый квест");
            config.put("author", "Vodka: ТК в meander");
            config.put("created", timestamp);
            config.put("lastOpened", timestamp);
            config.put("updated", timestamp);
            
            JSONObject converted = Converter.convertTKQuest(questContent, questId);
            JSONArray connections = converted.getJSONArray("connections");
            String startNodeId = converted.optString("startNodeId", null);
            JSONArray chapters = converted.optJSONArray("chapters");
            
            if (chapters != null) {
                for (int i = 0; i < chapters.length(); i++) {
                    JSONObject chapter = chapters.getJSONObject(i);
                    chapter.put("questId", questId);
                }
                config.put("tags", chapters);
            }
            
            config.put("connections", connections);
            
            if (startNodeId != null && !startNodeId.isEmpty()) {
                config.put("startNodeId", startNodeId);
            }
            
            ZipEntry entry = new ZipEntry("config.json");
            zos.putNextEntry(entry);
            zos.write(config.toString().getBytes("UTF-8"));
            zos.closeEntry();
            
            reader.close();
            is.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void addNodesJson(ZipOutputStream zos, String questContent) {
        try {
            JSONObject converted = Converter.convertTKQuest(questContent, null);
            JSONArray nodes = converted.getJSONArray("nodes");
            
            JSONObject nodesJson = new JSONObject();
            nodesJson.put("nodes", nodes);
            
            ZipEntry entry = new ZipEntry("nodes.json");
            zos.putNextEntry(entry);
            zos.write(nodesJson.toString().getBytes("UTF-8"));
            zos.closeEntry();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void addPreviewFromAssets(Context context, ZipOutputStream zos) {
        try {
            InputStream is = context.getAssets().open("preview.png");
            ZipEntry entry = new ZipEntry("preview.png");
            zos.putNextEntry(entry);
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            
            zos.closeEntry();
            is.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}