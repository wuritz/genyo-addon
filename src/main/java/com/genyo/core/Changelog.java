package com.genyo.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genyo.Genyo;
import com.genyo.systems.config.GenyoConfig;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Changelog {

    private volatile String latestVersion = null;
    private String updateString = "";
    private boolean updateAvailable = false;

    public Changelog() {

    }

    private void init() {
        MeteorExecutor.execute(() -> {
            // Update latest version a
            String url = "https://api.github.com/repos/wuritz/genyo-addon/releases/latest";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode json = mapper.readTree(response.body());

                    latestVersion = json.get("tag_name").asText();

                    if (!Genyo.VERSION.toString().equals(latestVersion) && latestVersion != null) {
                        updateAvailable = true;
                    }
                }
            } catch (IOException | InterruptedException e) {
                Genyo.LOG.error(e.getMessage());
            }
        });
    }

    public void render(DrawContext context) {
        if (latestVersion == null) init();

        int x = 3;
        int y = GenyoConfig.get().textPosition.get() == GenyoConfig.TextPosition.Center ? (mc.currentScreen.height / 2) - mc.textRenderer.fontHeight : 4 ;
        context.drawTextWithShadow(mc.textRenderer, "Genyo %s".formatted(Genyo.VERSION.toString()), x, y, -1);

        y += mc.textRenderer.fontHeight + 2;

        // Is there a new update available?
        if (updateAvailable) {
            updateString = "New update is available";
            context.drawTextWithShadow(mc.textRenderer, updateString, x, y, Color.ORANGE.getRGB());
        }
    }

    public boolean onClicked(double mouseX, double mouseY) {
        if (updateString == "") return false;

        int y = GenyoConfig.get().textPosition.get() == GenyoConfig.TextPosition.Center ? (mc.currentScreen.height / 2) - mc.textRenderer.fontHeight : 4
            + mc.textRenderer.fontHeight + 2;
        int x = 3;
        int width = mc.textRenderer.getWidth(updateString);

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + mc.textRenderer.fontHeight + 2) {
            String url = "https://github.com/wuritz/genyo-addon/releases";

            Util.getOperatingSystem().open(url);

            return true;
        }

        return false;
    }

}
