package com.update.vodka;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Converter {
    public static JSONObject convertTKQuest(String questContent, String questId) throws JSONException {
        JSONArray tkNodes = new JSONArray(questContent);
        JSONArray nodes = new JSONArray();
        JSONArray connections = new JSONArray();
        JSONArray chapters = new JSONArray();
        Map<Integer, String> nodeIdToUUID = new HashMap<>();

        if (questId == null) {
            questId = "temp_quest_id";
        }

        List<Integer> nonEmptyIds = new ArrayList<>();
        for (int i = 0; i < tkNodes.length(); i++) {
            JSONObject tkNode = tkNodes.getJSONObject(i);
            int oldId = tkNode.getInt("id");
            String text = tkNode.optString("text", "");
            JSONArray answers = tkNode.optJSONArray("answers");
            if (!text.isEmpty() || (answers != null && answers.length() > 0)) {
                nonEmptyIds.add(oldId);
            }
        }

        Collections.sort(nonEmptyIds);
        
        int totalNodes = nonEmptyIds.size();
        int chapterCount = (int) Math.ceil(totalNodes / 100.0);
        
        Map<Integer, String> chapterIdMap = new HashMap<>();
        for (int chapterIndex = 0; chapterIndex < chapterCount; chapterIndex++) {
            String chapterUUID = generateDeterministicUUID("chapter_" + questId + "_" + chapterIndex);
            chapterIdMap.put(chapterIndex, chapterUUID);
            
            JSONObject chapter = new JSONObject();
            chapter.put("id", chapterUUID);
            chapter.put("name", "Текстовый квест " + (chapterIndex + 1));
            chapter.put("questId", questId);
            
            JSONObject cameraState = new JSONObject();
            cameraState.put("offsetX", chapterIndex * 500.0);
            cameraState.put("offsetY", 0);
            cameraState.put("scale", 1.0);
            chapter.put("cameraState", cameraState);
            
            chapters.put(chapter);
        }

        for (int oldId : nonEmptyIds) {
            String uuid = generateDeterministicUUID("node_" + questId + "_" + oldId);
            nodeIdToUUID.put(oldId, uuid);
        }

        String infoNodeId = generateDeterministicUUID("info_node_" + questId);
        JSONObject infoNode = createInfoNode(infoNodeId, chapterIdMap.get(0));
        nodes.put(infoNode);

        String firstRealNodeId = null;
        if (nodeIdToUUID.containsKey(1)) {
            firstRealNodeId = nodeIdToUUID.get(1);
        } else if (!nonEmptyIds.isEmpty()) {
            firstRealNodeId = nodeIdToUUID.get(nonEmptyIds.get(0));
        }

        if (firstRealNodeId != null) {
            JSONObject infoConnection = new JSONObject();
            infoConnection.put("id", generateDeterministicUUID("conn_info_to_first_" + questId));
            infoConnection.put("fromId", infoNodeId);
            infoConnection.put("toId", firstRealNodeId);
            connections.put(infoConnection);
        }

        for (int i = 0; i < tkNodes.length(); i++) {
            JSONObject tkNode = tkNodes.getJSONObject(i);
            int oldId = tkNode.getInt("id");
            if (!nonEmptyIds.contains(oldId)) {
                continue;
            }
            
            String text = tkNode.optString("text", "");
            JSONArray answers = tkNode.optJSONArray("answers");

            int nodeIndex = nonEmptyIds.indexOf(oldId);
            int chapterIndex = nodeIndex / 100;
            String chapterId = chapterIdMap.get(chapterIndex);

            JSONObject node = new JSONObject();
            node.put("id", nodeIdToUUID.get(oldId));
            node.put("chapterId", chapterId);

            String title = text.length() > 30 ? text.substring(0, 30) + "..." : text;
            if (title.isEmpty()) title = "Узел " + oldId;
            node.put("title", title);

            int positionInChapter = nodeIndex % 100;
            node.put("x", (positionInChapter % 10) * 200 + chapterIndex * 2500 + 300);
            node.put("y", (positionInChapter / 10) * 150 + 100);

            JSONObject content = new JSONObject();
            JSONArray items = new JSONArray();

            if (!text.isEmpty()) {
                JSONObject textItem = new JSONObject();
                textItem.put("id", generateDeterministicUUID("text_" + questId + "_" + oldId));
                textItem.put("type", "text");
                textItem.put("text", text);
                items.put(textItem);
            }

            if (answers != null) {
                for (int j = 0; j < answers.length(); j++) {
                    JSONObject answer = answers.getJSONObject(j);
                    int targetId = answer.getInt("id");
                    String answerText = answer.getString("text");

                    if (!nonEmptyIds.contains(targetId)) {
                        continue;
                    }

                    JSONObject button = new JSONObject();
                    button.put("id", generateDeterministicUUID("button_" + questId + "_" + oldId + "_" + targetId));
                    button.put("type", "button");
                    button.put("text", answerText);
                    button.put("targetNodeId", nodeIdToUUID.get(targetId));
                    items.put(button);

                    JSONObject connection = new JSONObject();
                    connection.put("id", generateDeterministicUUID("conn_" + questId + "_" + oldId + "_" + targetId));
                    connection.put("fromId", nodeIdToUUID.get(oldId));
                    connection.put("toId", nodeIdToUUID.get(targetId));
                    connections.put(connection);
                }
            }

            content.put("items", items);
            node.put("content", content);
            nodes.put(node);
        }

        JSONObject result = new JSONObject();
        result.put("nodes", nodes);
        result.put("connections", connections);
        result.put("chapters", chapters);
        result.put("startNodeId", infoNodeId);

        return result;
    }
    
    private static JSONObject createInfoNode(String nodeId, String chapterId) throws JSONException {
        JSONObject node = new JSONObject();
        node.put("id", nodeId);
        node.put("chapterId", chapterId);
        node.put("title", "Информация о квесте");
        node.put("x", 100);
        node.put("y", 100);

        JSONObject content = new JSONObject();
        JSONArray items = new JSONArray();

        JSONObject textItem = new JSONObject();
        textItem.put("id", generateDeterministicUUID("info_text_" + nodeId));
        textItem.put("type", "text");
        textItem.put("text", "Данный квест был сконвертирован с помощью программы Vodka из \"Текстовые квесты — Играй и пиши\". Он не является оригинальной работой, созданной в Meander.\n\nПриложение Vodka разработано для конвертации квестов из формата ТК в формат Meander.");
        items.put(textItem);

        JSONObject buttonItem = new JSONObject();
        buttonItem.put("id", generateDeterministicUUID("info_button_" + nodeId));
        buttonItem.put("type", "button");
        buttonItem.put("text", "Принять и начать квест");
        items.put(buttonItem);

        content.put("items", items);
        node.put("content", content);

        return node;
    }
    
    private static String generateDeterministicUUID(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes()).toString();
    }
}