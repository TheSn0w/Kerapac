package net.botwithus;

import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class SkeletonScriptGraphicsContext extends ScriptGraphicsContext {
    private SkeletonScript script;
    private Instant startTime;
    private long scriptStartTime;
    private String healthFeedbackMessage = "";
    private String prayerFeedbackMessage = "";
    private String prayerPointsThresholdStr = "5000";
    private String healthThresholdStr = "50";
    private int loopCounter = 0;

    private static float RGBToFloat(int rgbValue) {
        return rgbValue / 255.0f;
    }

    public SkeletonScriptGraphicsContext(ScriptConsole scriptConsole, SkeletonScript script) {
        super(scriptConsole);
        this.script = script;
        this.startTime = Instant.now();
    }


    @Override
    public void drawSettings() {
        ImGui.PushStyleColor(21, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Button color
        ImGui.PushStyleColor(18, RGBToFloat(255), RGBToFloat(255), RGBToFloat(255), 1.0f); // Checkbox Tick color
        ImGui.PushStyleColor(5, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Border Colour
        ImGui.PushStyleColor(2, RGBToFloat(0), RGBToFloat(0), RGBToFloat(0), 0.9f); // Background color
        ImGui.PushStyleColor(7, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Checkbox Background color
        ImGui.PushStyleColor(11, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Header Colour
        ImGui.PushStyleColor(22, RGBToFloat(64), RGBToFloat(67), RGBToFloat(67), 1.0f); // Highlighted button color
        ImGui.PushStyleColor(27, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //ImGUI separator Colour
        ImGui.PushStyleColor(30, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //Corner Extender colour
        ImGui.PushStyleColor(31, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //Corner Extender colour
        ImGui.PushStyleColor(32, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //Corner Extender colour
        ImGui.PushStyleColor(33, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //Corner Extender colour
        ImGui.PushStyleColor(34, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); //Corner Extender colour


        ImGui.SetWindowSize(200.f, 200.f);
        if (ImGui.Begin("Kerapac", ImGuiWindowFlag.None.getValue())) {
            ImGui.PushStyleVar(1, 10.f, 5f);
            ImGui.PushStyleVar(2, 10.f, 5f); //spacing between side of window and checkbox
            ImGui.PushStyleVar(3, 10.f, 5f);
            ImGui.PushStyleVar(4, 10.f, 5f);
            ImGui.PushStyleVar(5, 10.f, 5f);
            ImGui.PushStyleVar(6, 10.f, 5f);
            ImGui.PushStyleVar(7, 10.f, 5f);
            ImGui.PushStyleVar(8, 10.f, 5f); //spacing between seperator and text
            ImGui.PushStyleVar(9, 10.f, 5f);
            ImGui.PushStyleVar(10, 10.f, 5f);
            ImGui.PushStyleVar(11, 10.f, 5f); // button sizes
            ImGui.PushStyleVar(12, 10.f, 5f);
            ImGui.PushStyleVar(13, 10.f, 5f);
            ImGui.PushStyleVar(14, 10.f, 5f); // spaces between options ontop such as overlays, debug etc
            ImGui.PushStyleVar(15, 10.f, 5f); // spacing between Text/tabs and checkboxes
            if (ImGui.BeginTabBar("Options", ImGuiWindowFlag.None.getValue())) {
                script.runScript = ImGui.Checkbox("Run Script", script.runScript);
                if (ImGui.Button("Restart & Teleport to War's Retreat (DEBUG)")) {
                    script.restartScript();
                }
                displayLoopCount();
                ImGui.SeparatorText("Combat Options");
                script.startAtPortal = ImGui.Checkbox("Start at Portal", script.startAtPortal);
                script.useCauldron = ImGui.Checkbox("Use War's Retreat Cauldron", script.useCauldron);
                script.HaveMobile = ImGui.Checkbox("Have Mobile for wars surge?", script.HaveMobile);
                script.UseScriptureOfWen = ImGui.Checkbox("Use Scripture of Wen", script.UseScriptureOfWen);
                script.useoverload = ImGui.Checkbox("Use Overload", script.useoverload);
                script.useWeaponPoison = ImGui.Checkbox("Use Weapon Poison", script.useWeaponPoison);
                script.useProtectMagic = ImGui.Checkbox("Use Protect from Magic", script.useProtectMagic);
                script.useDeflectMagic = ImGui.Checkbox("Use Deflect Magic", script.useDeflectMagic);
                script.useRuination = ImGui.Checkbox("Use Ruination", script.useRuination);
                script.useVulnBomb = ImGui.Checkbox("Use Vulnerability bomb", script.useVulnBomb);
                script.useInvokeDeath = ImGui.Checkbox("Use Invoke Death", script.useInvokeDeath);
                script.useLuckoftheDwarves = ImGui.Checkbox("Use Luck of the Dwarves Switch", script.useLuckoftheDwarves);
                ImGui.SeparatorText("Food/Prayer Options");
                script.useSaraBrew = ImGui.Checkbox("Drink Saradomin Brew", script.useSaraBrew);
                script.useSaraBrewandBlubber = ImGui.Checkbox("Drink Saradomin Brew and Blubber", script.useSaraBrewandBlubber);
                script.eatfood = ImGui.Checkbox("Eat Food", script.eatfood);
                ImGui.SetItemWidth(40);
                healthThresholdStr = ImGui.InputText("Health Threshold (%)", healthThresholdStr);
                ImGui.SameLine();
                if (ImGui.Button("Set Health Threshold")) {
                    try {
                        int newHealthThreshold = Integer.parseInt(healthThresholdStr.trim());
                        if (newHealthThreshold >= 0 && newHealthThreshold <= 100) {
                            script.setHealthThreshold(newHealthThreshold);
                            healthFeedbackMessage = "Health Threshold updated successfully to: " + newHealthThreshold;
                        } else {
                            healthFeedbackMessage = "Entered value must be within 0-100.";
                        }
                    } catch (NumberFormatException e) {
                        healthFeedbackMessage = "Invalid number format for Health Threshold.";
                    }
                }
                ImGui.SameLine();
                ImGui.Text("PRESS BUTTON TO ENABLE");
                if (!healthFeedbackMessage.isEmpty()) {
                    ImGui.Text(healthFeedbackMessage);
                }
                script.useprayer = ImGui.Checkbox("Use Prayer/Restore Pots/Flasks", script.useprayer);
                ImGui.SetItemWidth(60);
                prayerPointsThresholdStr = ImGui.InputText("Prayer Points Threshold", prayerPointsThresholdStr);
                ImGui.SameLine();

                if (ImGui.Button("Set Prayer Threshold")) {
                    try {
                        int newThreshold = Integer.parseInt(prayerPointsThresholdStr.trim());
                        if (newThreshold >= 0) {
                            script.setPrayerPointsThreshold(newThreshold);
                            prayerFeedbackMessage = "Threshold updated successfully to: " + newThreshold;
                        } else {
                            prayerFeedbackMessage = "Entered value must be non-negative.";
                        }
                    } catch (NumberFormatException e) {
                        prayerFeedbackMessage = "Invalid number format.";
                    }
                }
                ImGui.SameLine();
                ImGui.Text("PRESS BUTTON TO ENABLE");

                if (!prayerFeedbackMessage.isEmpty()) {
                    ImGui.Text(prayerFeedbackMessage);
                    ImGui.EndTabBar();
                    ImGui.End();
                }

            }
        }
        ImGui.PopStyleVar(100);
        ImGui.PopStyleColor(100);
    }
    private void displayLoopCount(){
        int loopCount = script.getLoopCounter();
        ImGui.Text("Number of Kills: " + loopCount);

        // Calculate the elapsed time
        Duration elapsedTime = Duration.between(startTime, Instant.now());

        // Calculate and display Runs Per Hour
        float runsPerHour = calculatePerHour(elapsedTime, loopCount);
        ImGui.Text(String.format("Kills Per Hour: %.2f", runsPerHour));

    }
    private float calculatePerHour(Duration elapsed, int quantity) {
        long elapsedSeconds = elapsed.getSeconds();
        if (elapsedSeconds == 0) return 0;
        return (float) quantity / elapsedSeconds * 3600;
    }
}