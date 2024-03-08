package net.botwithus;

import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.imgui.ImGui;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class SkeletonScriptGraphicsContext extends ScriptGraphicsContext {
    private SkeletonScript script;
    private Instant startTime;
    private String healthFeedbackMessage = "";
    private String prayerFeedbackMessage = "";
    private String prayerPointsThresholdStr = "5000";
    private String healthThresholdStr = "50";
    private String minDelayStr = "320";
    private String maxDelayStr = "360";
    private String delayUpdateFeedback = ""; // Store feedback messages
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
        ImGui.PushStyleColor(0, RGBToFloat(173), RGBToFloat(216), RGBToFloat(230), 0.8f); // Button color
        ImGui.PushStyleColor(21, RGBToFloat(47), RGBToFloat(79), RGBToFloat(79), 1.0f); // Button color
        ImGui.PushStyleColor(18, RGBToFloat(173), RGBToFloat(216), RGBToFloat(230), 1.0f); // Checkbox Tick color
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
                if (ImGui.BeginTabItem("Item Toggles", ImGuiWindowFlag.None.getValue())) {
                    script.runScript = ImGui.Checkbox("Run Script", script.runScript);
                    if (ImGui.Button("Restart & Teleport to War's Retreat (DEBUG)")) {
                        script.restartScript();
                    }
                    displayLoopCount();
                    ImGui.SeparatorText("Combat Options");
                    script.startAtPortal = ImGui.Checkbox("Start outside Kerapac Entrance", script.startAtPortal);
                    script.useCauldron = ImGui.Checkbox("Use War's Retreat Cauldron", script.useCauldron);
                    script.HaveMobile = ImGui.Checkbox("Have Mobile for wars surge?", script.HaveMobile);
                    script.UseScriptureOfWen = ImGui.Checkbox("Use Scripture of Wen", script.UseScriptureOfWen);
                    script.useoverload = ImGui.Checkbox("Use Overload", script.useoverload);
                    script.useWeaponPoison = ImGui.Checkbox("Use Weapon Poison", script.useWeaponPoison);
                    if (ImGui.Checkbox("Use Protect from Magic", script.useProtectMagic)) {
                        script.useProtectMagic = true;
                        script.useDeflectMagic = false;
                    }
                    ImGui.SameLine();
                    if (ImGui.Checkbox("Use Deflect Magic", script.useDeflectMagic)) {
                        script.useDeflectMagic = true;
                        script.useProtectMagic = false;
                    }

                    if (ImGui.Checkbox("Use Ruination", script.useRuination)) {
                        script.useRuination = true;
                        script.useSorrow = false;
                    }
                    ImGui.SameLine();
                    if (ImGui.Checkbox("Use Sorrow", script.useSorrow)) {
                        script.useSorrow = true;
                        script.useRuination = false;
                    }
                    script.useVulnBomb = ImGui.Checkbox("Use Vulnerability bomb", script.useVulnBomb);
                    script.useInvokeDeath = ImGui.Checkbox("Use Invoke Death", script.useInvokeDeath);
                    script.useDarkness = ImGui.Checkbox("Use Darkness", script.useDarkness);
                    script.useLuckoftheDwarves = ImGui.Checkbox("Use Luck of the Dwarves Switch", script.useLuckoftheDwarves);
                    ImGui.EndTabItem();
                }
                if (ImGui.BeginTabItem("Thresholds", ImGuiWindowFlag.None.getValue())) {
                    ImGui.PushStyleColor(0, RGBToFloat(173), RGBToFloat(216), RGBToFloat(230), 0.8f); // Button color
                    ImGui.SeparatorText("Food/Prayer Options");
                    script.useSaraBrew = ImGui.Checkbox("Drink Saradomin Brew", script.useSaraBrew);
                    script.useSaraBrewandBlubber = ImGui.Checkbox("Drink Saradomin Brew and Blubber", script.useSaraBrewandBlubber);
                    script.eatfood = ImGui.Checkbox("Eat Food", script.eatfood);
                    ImGui.SetItemWidth(50);
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
                    ImGui.SetItemWidth(50);
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
                    }
                    ImGui.SeparatorText("Execution Delay Range (ms) When to Surge during Kerapac Jump phase");
                    ImGui.SetItemWidth(50);
                    minDelayStr = ImGui.InputText("Min Delay", minDelayStr);
                    ImGui.SetItemWidth(50);
                    maxDelayStr = ImGui.InputText("Max Delay", maxDelayStr);

                    if (ImGui.Button("Update Delays")) {
                        try {
                            int newMinDelay = Integer.parseInt(minDelayStr.trim());
                            int newMaxDelay = Integer.parseInt(maxDelayStr.trim());
                            if (newMinDelay >= 0 && newMaxDelay >= newMinDelay) { // Ensure max delay is not less than min delay and both are non-negative
                                script.setMinDelay(newMinDelay);
                                script.setMaxDelay(newMaxDelay);
                                delayUpdateFeedback = "Delays updated successfully.";
                            } else {
                                delayUpdateFeedback = "Error: Max Delay must be >= Min Delay and both non-negative.";
                            }
                        } catch (NumberFormatException e) {
                            delayUpdateFeedback = "Invalid input format. Please enter integers only.";
                        }
                    }

                    if (!delayUpdateFeedback.isEmpty()) {
                        ImGui.Text(delayUpdateFeedback); // Display the feedback message
                    }
                    ImGui.EndTabItem();
                }
                if (ImGui.BeginTabItem("Instructions", ImGuiWindowFlag.None.getValue())) {
                    ImGui.PushStyleColor(0, RGBToFloat(173), RGBToFloat(216), RGBToFloat(230), 0.8f); // Button color
                    ImGui.SeparatorText("Follow These Steps");
                    ImGui.Text("1. Activate Script from BWU Scripts Menu -> Open Settings.");
                    ImGui.Text("2. Choose Start Point (Anywhere is default) or Kerapac Entrance.");
                    ImGui.Text("3. Have portal available to Kerapac.");
                    ImGui.Text("4. Have Adrenaline Crystal Unlocked.");
                    ImGui.Text("5. Have Bank preset ready, test this beforehand.");
                    ImGui.Text("6. Have Altar of War Unlocked.");
                    ImGui.Text("7. Have Revo Enabled.");
                    ImGui.Text("8. Have abilities on action bar & Items on action bar.");
                    ImGui.Text("9. Have Double Surge UNLOCKED.");
                    ImGui.Text("10. IF EATING TICK AND CLICK SET THRESHOLD.");
                    ImGui.Text("11. IF PRAYER TICK AND CLICK SET THRESHOLD.");
                    ImGui.Text("12. IF CHANGING DELAYS, CLICK UPDATE - (this can be done during the fight).");
                    ImGui.Text("13. will support all familiars and scrolls.");
                    ImGui.Text("14. if using familiar, have in bank preset, otherwise it will skip it.");
                    ImGui.Text("15. if using scrolls, have in bank preset.");
                    ImGui.Text("16. The Thresholds tab will not save on `Loading Script` (for now).");
                    ImGui.Text("17. Have decent revo bar & Damage.");
                    ImGui.Text("18. Have Ability Queing turned ON.");
                    ImGui.Text("19. i would suggest having, backpack, familiar setting and Equipment on show.");
                    ImGui.Text("20. MUST HAVE AREALOOT ENABLED (ENABLE ALL 4 OPTIONS IN LOOT SYSTEM.");
                    ImGui.SeparatorText("Version 1.0.0");
                    ImGui.Text("- This script is still in development, please report any bugs to the developer.");
                    ImGui.Text("- Change Log: ");
                    ImGui.Text("- Added walking if second surge is not detected during pre fight");
                    ImGui.Text("- Added support for all familiars and scrolls");
                    ImGui.Text("- Added support for all prayers Levels for the altar of war");
                    ImGui.Text("- Added Customisable Delays for Kerapac Jump Phase");
                    ImGui.Text("- Added Start Timer");
                    ImGui.Text("- Will now teleport if inactive for 30 seconds before/after Kerapac fight");
                    ImGui.Text("- Added Persistant Settings");

                }
                ImGui.EndTabItem();
            }
            ImGui.EndTabBar();
            ImGui.End();
        }
        ImGui.PopStyleVar(100);
        ImGui.PopStyleColor(100);
    }

    private void displayLoopCount() {
        int loopCount = script.getLoopCounter();
        ImGui.Text("Number of Kills: " + loopCount);

        Duration elapsedTime = Duration.between(startTime, Instant.now());
        float runsPerHour = calculatePerHour(elapsedTime, loopCount);
        ImGui.Text(String.format("Kills Per Hour: %.2f", runsPerHour));

        // Calculate hours, minutes, and seconds from elapsedTime
        long hours = elapsedTime.toHours();
        long minutes = elapsedTime.toMinutesPart();
        long seconds = elapsedTime.toSecondsPart();

        // Display the running time
        ImGui.Text(String.format("Time Running: %02d:%02d:%02d", hours, minutes, seconds));
    }

    private float calculatePerHour(Duration elapsed, int quantity) {
        long elapsedSeconds = elapsed.getSeconds();
        if (elapsedSeconds == 0) return 0;
        return (float) quantity / elapsedSeconds * 3600;
    }
}