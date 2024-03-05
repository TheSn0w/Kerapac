package net.botwithus;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Equipment;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.items.GroundItemQuery;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.Player;
import net.botwithus.rs3.game.scene.entities.item.GroundItem;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.util.RandomGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static net.botwithus.rs3.game.Client.getLocalPlayer;
import static net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer.LOCAL_PLAYER;


public class SkeletonScript extends LoopingScript {

    private BotState botState = BotState.IDLE;
    public boolean runScript;
    boolean UseScriptureOfWen;
    private int prayerPointsThreshold = 1000;
    boolean useprayer;
    boolean useoverload;
    boolean HaveMobile;
    boolean useInvokeDeath;
    boolean useLuckoftheDwarves;
    boolean eatfood;
    boolean useCauldron;
    boolean useVulnBomb;
    boolean useRuination;
    boolean useDeflectMagic;
    boolean useProtectMagic;
    boolean useSaraBrew;
    boolean useSaraBrewandBlubber;
    boolean useWeaponPoison;

    private int healthThreshold = 50;
    boolean startAtPortal;


    enum BotState {
        IDLE,
        PRAYER,
        BANKING,
        PORTAL,
        CAULDRON,
        KERAPACPORTAL,
        INTERACTWITHDIALOG,
        KERAPAC,
        LOOTING,
        WARSRETREAT,
        RESTART_SCRIPT,
    }


    public SkeletonScript(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.sgc = new SkeletonScriptGraphicsContext(getConsole(), this);
    }
    private boolean kerapacPortalInitialized = false;

    @Override
    public void onLoop() {
        if (getLocalPlayer() != null && Client.getGameState() == Client.GameState.LOGGED_IN && !runScript) {
            return;
        }
        if (!kerapacPortalInitialized) {
            if (startAtPortal) {
                botState = BotState.KERAPACPORTAL;
            } else {
                botState = BotState.IDLE; // This is implicit if startAtPortal is false, might be redundant
            }
            kerapacPortalInitialized = true; // Ensure this initial adjustment happens only once
        }
        switch (botState) {
            case IDLE -> {
                println("We're idle!");
                Execution.delay(RandomGenerator.nextInt(1000, 3000));
                botState = BotState.PRAYER;
            }
            case PRAYER -> {
                hasInteractedWithLootAll = false;
                hasInteractedWithStart = false;
                if (getLocalPlayer() != null && getLocalPlayer().getPrayerPoints() < 9000) {
                    useAltarOfWar();
                } else {
                    botState = BotState.CAULDRON;
                    println("Prayer points are over 9000!");
                }
            }
            case CAULDRON -> {
                if (VarManager.getVarbitValue(26037) == 0 && (useCauldron)) {
                    useCauldron();
                } else { botState = BotState.BANKING;
                    println("Cauldron buff is active!");
                }
            }
            case BANKING -> {
                UseBankChest();
            }
            case PORTAL -> {
                walkToPortal();
            }
            case KERAPACPORTAL -> {
                InteractWithColloseum();
            }
            case INTERACTWITHDIALOG -> {
                InteractWithDialog();
            }
            case KERAPAC -> {
                kerapacPhase1();

            }
            case LOOTING -> {
                deactivateScriptureOfWen();
                loot();
            }
            case WARSRETREAT -> {
                useWarsRetreat();
            }
            case RESTART_SCRIPT -> {
                restartScript();
            }
        }
    }

    public void restartScript() {
        this.botState = BotState.WARSRETREAT;

        // Reset any specific flags or variables
        this.hasInteractedWithLootAll = false;
        this.hasInteractedWithStart = false;
        this.runScript = false; // Assuming you want to start running the script immediately
        println("Script has been restarted.");
    }

    private void activatePrayers() {
        boolean Ruination = VarManager.getVarbitValue(53280) == 0;
        boolean DeflectMagic = VarManager.getVarbitValue(16768) == 0;
        boolean ProtectMagic = VarManager.getVarbitValue(16745) == 0;


        if (Ruination && useRuination) {
            ActionBar.usePrayer("Ruination");
            Execution.delay(RandomGenerator.nextInt(10, 20));
        }
        if (DeflectMagic && useDeflectMagic) {
            ActionBar.usePrayer("Deflect Magic");
            Execution.delay(RandomGenerator.nextInt(10, 20));
        }
        if (ProtectMagic && useProtectMagic) {
            ActionBar.usePrayer("Protect Magic");
            Execution.delay(RandomGenerator.nextInt(10, 20));
        }
    }

    private void useWarsRetreat() {
        if (getLocalPlayer() != null) {
            ActionBar.useAbility("War's Retreat Teleport");
            println("Using Wars Retreat!");
            Execution.delay(RandomGenerator.nextInt(2000, 3000));
            botState = BotState.PRAYER;
        }
    }

    private void useAltarOfWar() {
        if(getLocalPlayer() == null)
            return;

        EntityResultSet<SceneObject> query = SceneObjectQuery.newQuery().name("Altar of War").results();
            if (!query.isEmpty()) {
                SceneObject altar = query.nearest();
                if (altar != null) {
                    altar.interact("Pray");
                    println("Praying!");
                    Execution.delayUntil(5000, () ->
                        getLocalPlayer().getPrayerPoints() >= 9000
                    );
                    botState = BotState.CAULDRON;
                }else {
                    println("Failed to interact with Altar of War.");
                }
            }
    }

    private void UseBankChest() {
        if(getLocalPlayer() == null)
            return;

        boolean success = false;
            EntityResultSet<SceneObject> query = SceneObjectQuery.newQuery().name("Bank chest").results();
            if (!query.isEmpty()) {
                println("Loading preset!");
                SceneObject bankChest = query.nearest();
                if(bankChest != null) {
                    bankChest.interact("Load Last Preset from");
                    success = true;
                }
                if(success) {
                    Execution.delay(RandomGenerator.nextInt(4000, 5000));
                }
                //wait at bank if we aren't full health
                if (getLocalPlayer().getCurrentHealth() < getLocalPlayer().getMaximumHealth()) {
                    println("Healing up!");
                    Execution.delay(RandomGenerator.nextInt(4000, 5000));
                } else {
                    botState = BotState.PORTAL;
                }
            }
    }

    private void walkToPortal() {
        Player localPlayer = getLocalPlayer();
        if (localPlayer == null) {
            println("Local player not found.");
            return;
        }

        EntityResultSet<SceneObject> sceneObjectQuery = SceneObjectQuery.newQuery().name("Adrenaline crystal").results();
        if (sceneObjectQuery.isEmpty()) {
            println("Adrenaline crystal not found.");
            return;
        }

        SceneObject portal = sceneObjectQuery.nearest();
        if (portal == null) {
            println("Nearest portal not found.");
            return;
        }

        portal.interact("Channel");
        println("Attempting to get adrenaline...");

        boolean hasSurged = false;
        while (Distance.between(getLocalPlayer().getCoordinate(), portal.getCoordinate()) > 0) {
            Coordinate playerCoord = getLocalPlayer().getCoordinate();

            // Check if the player is between the specified coordinates and Surge is off cooldown
            if (!hasSurged && (HaveMobile) && playerCoord.getX() <= 3295 && playerCoord.getX() >= 3293 &&
                    playerCoord.getY() == 10134 && ActionBar.getCooldown("Surge") == 0) {

                ActionBar.useAbility("Surge");
                println("Using 'Surge' to reach the portal quicker.");
                hasSurged = true; // Prevents multiple surges

                // Wait for the surge effect to complete
                Execution.delay(200);
            }

            if (hasSurged) {
                // Step 2: After using Surge, interact with the portal again
                portal.interact("Channel");
                break;
            }

            Execution.delay(100); // Check every 100 milliseconds
        }

        // Wait until adrenaline is charged or a timeout occurs
        boolean adrenalineCharged = Execution.delayUntil(15000, () -> getLocalPlayer().getAdrenaline() >= 1000);
        if (adrenalineCharged) {
            println("Adrenaline charged, attempting to interact with the Kerapac portal.");
            interactWithKerapacPortal(); // Method to interact with Kerapac portal
        } else {
            println("Adrenaline not charged within timeout, retrying...");
            walkToPortal(); // Recursive call to retry
        }
    }

    private void interactWithKerapacPortal() {
        EntityResultSet<SceneObject> kerapacPortalQuery = SceneObjectQuery.newQuery().name("Portal (Kerapac)").results();
        if (!kerapacPortalQuery.isEmpty()) {
            SceneObject kerapacPortal = kerapacPortalQuery.nearest();
            if (kerapacPortal != null) {
                kerapacPortal.interact("Enter");
                println("Interacting with portal...");
                botState = BotState.KERAPACPORTAL;
                Execution.delay(RandomGenerator.nextInt(2000, 3000));
            } else {
                println("Portal did not become available. Retrying...");
                walkToPortal(); // Recursive call to retry
            }
        } else {
            println("Portal did not become available. Retrying...");
            walkToPortal(); // Recursive call to retry
        }
    }

    public boolean WalkTo(int x, int y) {
        if (getLocalPlayer() != null) {
            Coordinate myPos = getLocalPlayer().getCoordinate();
            if (myPos.getX() != x && myPos.getY() != y) {

                if (!getLocalPlayer().isMoving()) {
                    println("Walking to: " + x + ", " + y);
                    Travel.walkTo(x, y);
                    Execution.delay(RandomGenerator.nextInt(300, 500));
                }
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private void InteractWithColloseum() {
        Execution.delay(RandomGenerator.nextInt(1250, 1500));
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().id(120046).option("Enter").results();
        if (!results.isEmpty()) {
            SceneObject colloseum = results.nearest();
            if(colloseum != null) {
                colloseum.interact("Enter");
                println("Entering colloseum!");
                botState = BotState.INTERACTWITHDIALOG;
            }
        }
    }

    private void InteractWithDialog() {
        if (Interfaces.isOpen(1591)) {
            Execution.delay(RandomGenerator.nextInt(800, 1000));
            Start();
            println("Interacting with dialog!");
            Execution.delay(RandomGenerator.nextInt(1200, 1700));
            botState = BotState.KERAPAC;
        }
    }

    private Coordinate kerapacPhase1StartCoord = null;

    public void kerapacPhase1() {
        if(getLocalPlayer() == null)
            return;

        kerapacPhase1StartCoord = getLocalPlayer().getCoordinate();

        Execution.delay(RandomGenerator.nextInt(1000, 1500));
        Coordinate currentCoord = getLocalPlayer().getCoordinate();
        ActionBar.useAbility("Surge");
        println("Ability 'Surge' used. Waiting for cooldown...");

        Execution.delay(1800);

        while (ActionBar.getCooldown("Surge") > 0) {
            Execution.delay(100);
        }

        ActionBar.useAbility("Surge");
        println("Ability 'Surge' used again after cooldown.");
        Execution.delay(1800);
        ActionBar.useAbility("Conjure Undead Army");
        activatePrayers();


        boolean shouldSurge = false;
        boolean surged = false;
        boolean firstAnimationSequence = true;


        while (true) {
            if (UseScriptureOfWen) {
                manageScriptureOfWen();
                Execution.delay(RandomGenerator.nextInt(10, 20));



            }
            EntityResultSet<Npc> npcs = NpcQuery.newQuery().name("Kerapac, the bound").results();



            for (Npc npc : npcs) {
                int animationId = npc.getAnimationId();

                if (firstAnimationSequence) {
                    if (animationId == 34193) {
                        shouldSurge = true; // Set shouldSurge to true upon the first animation
                    } else if (animationId == 34194 && shouldSurge) {
                        println("Skipping first encounter animations.");
                        firstAnimationSequence = false; // Reset for subsequent encounters
                        shouldSurge = false; // Reset shouldSurge as we skip the first sequence
                        Execution.delay(2000);
                        npc.interact("Attack");
                        println("Attacking Kerapac after skipping the first animation sequence.");
                    }
                } else {
                    // Logic for subsequent encounters after skipping the first animation sequence
                    if (animationId == 34193) {
                        shouldSurge = true;
                    } else if (animationId == 34194 && shouldSurge && !surged) {
                        println("Hes Flying High.... dodging this shit...");
                        Execution.delay(RandomGenerator.nextInt(550, 560));
                        ActionBar.useAbility("Surge");
                        println("Dodged MWAHA!");
                        surged = true;
                    }

                    if (surged) {
                        if (npc.validate()) {
                            Execution.delay(RandomGenerator.nextInt(100, 200));
                            npc.interact("Attack");
                            println("Attacking Kerapac after dodging.");
                            shouldSurge = false;
                            surged = false;
                        }

                    } else if (animationId == 34198 && npc.getCurrentHealth() >= 65000) {
                        println("Kerapac tryna stun us,, WHAAAA ....");
                        Coordinate kerapacLocation = Objects.requireNonNull(npc.getCoordinate());

                        if (Travel.walkTo(kerapacLocation)) {
                            while (Distance.between(getLocalPlayer().getCoordinate(), kerapacLocation) > 1) {

                                if (ActionBar.getCooldown("Anticipation") == 0) {
                                    ActionBar.useAbility("Anticipation");
                                    println("Tried to use 'Anticipation' to avoid Kerapac's stun.");
                                }
                                Execution.delay(100);
                            }

                            Execution.delay(RandomGenerator.nextInt(1750, 1850));
                            npc.interact("Attack");
                            println("Attacking Kerapac after reaching his location.");
                        }
                    } else if (animationId == 34195) {
                        Coordinate bottomLeft = currentCoord.derive(-23, -10, 0);
                        Coordinate topRight = currentCoord.derive(+6, +11, 0);
                        Area.Rectangular instanceArea = new Area.Rectangular(bottomLeft, topRight);


                        Coordinate threeTilesAway = currentCoord.derive(-3, 0, 0);


                        if (instanceArea.contains(threeTilesAway)) {
                            if (Travel.walkTo(threeTilesAway)) {
                                println("Were toooo slow, moving away!.");
                                Execution.delay(RandomGenerator.nextInt(1750, 1850));
                                npc.interact("Attack");
                                println("Re-engaging Kerapac after walking away.");
                            }
                        } else {
                            println("Desired location is out of instance bounds. Adjusting movement...");

                        }
                    } else if (animationId == 34186) {
                        println("Bitch is dead, lol lets see what goodies we got :D ");
                        botState = BotState.LOOTING;
                        return; // Exit if the condition is met and the action is performed
                    }
                    if (Interfaces.isOpen(1181)) {
                        // Query for a specific component related to game phases
                        ComponentQuery phaseQuery = ComponentQuery.newQuery(1181).componentIndex(21);
                        ResultSet<Component> phaseResults = phaseQuery.results();
                        Component phaseComponent = phaseResults.first();

                        // Check for phase 4 and if Invoke Death should be used
                        if (useInvokeDeath && phaseComponent != null && "Phase: 4".equals(phaseComponent.getText())) {
                            // Query for a specific component by sprite ID
                            ComponentQuery query = ComponentQuery.newQuery(1490).spriteId(30100);
                            ResultSet<Component> results = query.results();

                            // If no results found and cooldown is zero, use the ability
                            if (results.isEmpty()) {
                                if (ActionBar.getCooldown("Invoke Death") == 0) {
                                    ActionBar.useAbility("Invoke Death");
                                    println("Used 'Invoke Death'");
                                    Execution.delay(600);
                                }
                            }
                        }

                        // Additional logic for wearing "Luck of the Dwarves" under certain conditions
                        if (useLuckoftheDwarves && phaseComponent != null && "Phase: 4".equals(phaseComponent.getText())) {
                            ResultSet<Item> luckResults = InventoryItemQuery.newQuery().name("Luck of the Dwarves").results();
                            if (!luckResults.isEmpty()) {
                                Item luckOfTheDwarves = luckResults.first();
                                if (luckOfTheDwarves != null && luckOfTheDwarves.getStackSize() > 0) {
                                    boolean success = ActionBar.useItem("Luck of the Dwarves", "Wear");
                                    if (success) {
                                        println("Wearing 'Luck of the Dwarves'");
                                    }
                                }
                            }
                        }
                    }
                    if (useprayer)
                        usePrayerOrRestorePots();
                    Execution.delay(RandomGenerator.nextInt(50, 100));
                    if (useoverload)
                        drinkOverloads();
                    Execution.delay(RandomGenerator.nextInt(50, 100));
                    if (eatfood)
                        eatFood();
                    Execution.delay(RandomGenerator.nextInt(50, 100));
                    if (useWeaponPoison)
                        useWeaponPoison();
                    Execution.delay(RandomGenerator.nextInt(50, 100));




                    if (getLocalPlayer().getTarget() != null && useVulnBomb) {
                        int vulnDebuffVarbit = VarManager.getVarbitValue(1939);
                        if (vulnDebuffVarbit == 0 && npc.getCurrentHealth() > 100000 && Backpack.contains("Vulnerability bomb")) {
                            boolean success = ActionBar.useItem("Vulnerability bomb", "Throw");
                            if (success) {
                                println("Eat this vuln bomb!   " + getLocalPlayer().getTarget().getName());
                                Execution.delayUntil(RandomGenerator.nextInt(2000, 3000), () -> !getLocalPlayer().inCombat());
                            }else {
                                println("Failed to use Vulnerability bomb.");
                            }
                        }
                    }
                }
            }
        }
    }

    private void loot() {
        if (getLocalPlayer() != null) {
            if (!getLocalPlayer().inCombat()) {
                return;
            }
            List<String> itemNames = Arrays.asList(
                    "Coins", "Cannonball", "Hydrix bolt tips",
                    "Large plated orikalkum salvage", "Dragonkin bones",
                    "Inert adrenaline crystal", "Royal dragonhide",
                    "Soul rune", "Uncut dragonstone", "Light animica stone spirit",
                    "Fire battlestaff", "Kerapac's wrist wraps",
                    "Greater Concentrated blast ability codex", "Scripture of Jas"
            );

            final List<String> finalItemNames = new ArrayList<>(itemNames);

            boolean lootDetected = GroundItemQuery.newQuery().name(finalItemNames.toArray(new String[0])).results().isEmpty();

            if (!lootDetected && kerapacPhase1StartCoord != null) {
                println("Looting items...");
                if (WalkTo(kerapacPhase1StartCoord.getX(), kerapacPhase1StartCoord.getY())) {
                    Execution.delayUntil(60000, () -> !GroundItemQuery.newQuery().name(finalItemNames.toArray(new String[0])).results().isEmpty());
                }
            }

            for (String itemName : finalItemNames) {
                EntityResultSet<GroundItem> groundItems = GroundItemQuery.newQuery().name(itemName).results();
                if (!groundItems.isEmpty()) {
                    GroundItem groundItem = groundItems.nearest();
                    if (groundItem != null) {
                        groundItem.interact("Take");
                        Execution.delayUntil(5000, () -> getLocalPlayer().isMoving());
                        if (getLocalPlayer().isMoving() && groundItem.getCoordinate() != null && Distance.between(getLocalPlayer().getCoordinate(), groundItem.getCoordinate()) > 10) {
                            ActionBar.useAbility("Surge");
                            Execution.delay(200); // Wait after surging
                        }
                        if (groundItem.getCoordinate() != null) {
                            Execution.delayUntil(5000, () -> Distance.between(getLocalPlayer().getCoordinate(), groundItem.getCoordinate()) <= 10); // Wait for the player to walk to the item
                        }
                        if (groundItem.interact("Take")) {
                            println("Taking " + itemName);
                            Execution.delay(RandomGenerator.nextInt(700, 900));
                        }

                        Execution.delayUntil(15000, () -> Interfaces.isOpen(1622));
                        Execution.delay(RandomGenerator.nextInt(2000, 3000));
                        LootAll();
                        break;
                    }
                    Execution.delay(RandomGenerator.nextInt(400, 600));
                }
            }

            boolean useRuination = VarManager.getVarbitValue(53280) == 1;
            boolean useDeflectMagic = VarManager.getVarbitValue(16768) == 1;
            boolean useProtectMagic = VarManager.getVarbitValue(16745) == 1;


            if (useRuination) {
                ActionBar.usePrayer("Ruination");
                Execution.delay(RandomGenerator.nextInt(10, 20));
            }
            if (useDeflectMagic) {
                ActionBar.usePrayer("Deflect Magic");
                Execution.delay(RandomGenerator.nextInt(10, 20));
            }
            if (useProtectMagic) {
                ActionBar.usePrayer("Protect Magic");
                Execution.delay(RandomGenerator.nextInt(10, 20));
            }

            botState = BotState.WARSRETREAT;
        }
    }


    private boolean scriptureOfWenActive = false;

    public void manageScriptureOfWen() {
        if (LOCAL_PLAYER.inCombat() && !scriptureOfWenActive) {
            updateScriptureOfWenActivation();
        } else if (!LOCAL_PLAYER.inCombat() && scriptureOfWenActive) {
            updateScriptureOfWenActivation();
        }
    }

    private void updateScriptureOfWenActivation() {
        // Querying the component to check if Scripture of Wen is active
        boolean isActive = isScriptureOfWenActive();

        boolean shouldBeActive = shouldActivateScriptureOfWen();

        if (shouldBeActive && !isActive) {
            activateScriptureOfWen();
        } else if (!shouldBeActive && isActive) {
            deactivateScriptureOfWen();
        }
    }

    private boolean isScriptureOfWenActive() {
        ComponentQuery query = ComponentQuery.newQuery(284).spriteId(52117);
        ResultSet<Component> results = query.results();
        return !results.isEmpty();
    }

    private void activateScriptureOfWen() {
        if (!isScriptureOfWenActive()) {
            println("Activating Scripture of Wen.");
            if (Equipment.interact(Equipment.Slot.POCKET, "Activate/Deactivate")) {
                println("Scripture of Wen activated successfully.");
                scriptureOfWenActive = true;
            } else {
                println("Failed to activate Scripture of Wen.");
            }
        }
    }

    private void deactivateScriptureOfWen() {
        if (isScriptureOfWenActive()) {
            println("Deactivating Scripture of Wen.");
            Equipment.interact(Equipment.Slot.POCKET, "Activate/Deactivate");
            {
                Execution.delay(RandomGenerator.nextInt(300, 500));
                println("Scripture of Wen deactivated.");
                scriptureOfWenActive = false;
            }
        }
    }

    private boolean shouldActivateScriptureOfWen() {
        if(getLocalPlayer() == null)
            return false;

        return getLocalPlayer().inCombat() && (UseScriptureOfWen);
    }

    public void setPrayerPointsThreshold(int threshold) {
        this.prayerPointsThreshold = threshold;
    }

    public void setHealthThreshold(int healthThreshold) {
        this.healthThreshold = healthThreshold;
    }

    public void usePrayerOrRestorePots() {
        Player localPlayer = getLocalPlayer();
        if (localPlayer != null) {
            int currentPrayerPoints = LOCAL_PLAYER.getPrayerPoints();
            if (currentPrayerPoints < prayerPointsThreshold) {
                ResultSet<Item> items = InventoryItemQuery.newQuery(93).results();

                Item prayerOrRestorePot = items.stream()
                        .filter(item -> item.getName() != null &&
                                (item.getName().toLowerCase().contains("prayer") ||
                                        item.getName().toLowerCase().contains("restore")))
                        .findFirst()
                        .orElse(null);

                if (prayerOrRestorePot != null) {
                    println("Drinking " + prayerOrRestorePot.getName());
                    boolean success = Backpack.interact(prayerOrRestorePot.getName(), "Drink");
                    Execution.delay(RandomGenerator.nextInt(1600, 2100));

                    if (!success) {
                        println("Failed to use " + prayerOrRestorePot.getName());
                    }
                } else {
                    println("No Prayer or Restore pots found.");
                }
            }
        }
    }


    public void drinkOverloads() {
        Player localPlayer = getLocalPlayer();
        if (localPlayer != null && !localPlayer.isMoving()) {
            if (VarManager.getVarbitValue(26037) == 0) {
                if (localPlayer.getAnimationId() == 18000) {
                    return; // Already performing an action, possibly drinking.
                }

                ResultSet<Item> items = InventoryItemQuery.newQuery()
                        .results();

                Item overloadItem = items.stream()
                        .filter(item -> item.getName() != null &&
                                item.getName().toLowerCase().contains("overload"))
                        .findFirst()
                        .orElse(null);

                if (overloadItem != null) {
                    println("Drinking overload " + overloadItem.getName() + " ID: " + overloadItem.getId());
                    boolean success = Backpack.interact(overloadItem.getName(), "Drink");
                    Execution.delay(RandomGenerator.nextInt(500, 600));

                    if (!success) {
                        println("Failed to drink " + overloadItem.getName());
                    }
                } else {
                    println("No overload found!");
                }
            }
        }
    }

    public void eatFood() {
        if (getLocalPlayer() != null) {
            if (getLocalPlayer().getCurrentHealth() * 100 / getLocalPlayer().getMaximumHealth() < healthThreshold) {
                ResultSet<Item> food = InventoryItemQuery.newQuery(93).option("Eat").results();
                if (!food.isEmpty()) {
                    Item eat = food.first();
                    if(eat != null) {
                        Backpack.interact(eat.getName(), 1);
                        println("Eating " + eat.getName());
                        Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> getLocalPlayer().getCurrentHealth() > 8000);
                    }else {
                        println("Failed to eat!");
                    }
                } else {
                    println("No food found!");
                }
            }
        }
    }

    private boolean hasInteractedWithLootAll = false;

    private void LootAll() {
        if (!hasInteractedWithLootAll) {
            ComponentQuery query = ComponentQuery.newQuery(1622); // Interface ID
            List<Component> components = query.componentIndex(22) // Component index
                    .results()
                    .stream()
                    .toList();

            if (!components.isEmpty() && components.get(0).interact(1)) { // Perform the "Select" interaction
                hasInteractedWithLootAll = true;
                println("Successfully interacted with LootAll component.");
            }
        }
    }

    private boolean hasInteractedWithStart = false;

    private void Start() {
        if (!hasInteractedWithStart) {
            ComponentQuery query = ComponentQuery.newQuery(1591); // Interface ID
            List<Component> components = query.componentIndex(60) // Component index
                    .results()
                    .stream()
                    .toList();

            if (!components.isEmpty() && components.get(0).interact(1)) { // Perform the "Select" interaction
                hasInteractedWithStart = true;
                println("Successfully interacted with Start component.");
            }
        }
    }

    public void useWeaponPoison() {
        Player localPlayer = getLocalPlayer();
        if (localPlayer != null && !localPlayer.isMoving()) {
            // Check if interface 284 does NOT contain a component with sprite ID 30095 to indicate weapon poison is not already applied
            if (!hasComponentWithSpriteId(284, 30095)) {

                ResultSet<Item> items = InventoryItemQuery.newQuery()
                        .results();

                Item weaponPoisonItem = items.stream()
                        .filter(item -> item.getName() != null &&
                                item.getName().toLowerCase().contains("weapon poison"))
                        .findFirst()
                        .orElse(null);

                if (weaponPoisonItem != null) {
                    println("Applying " + weaponPoisonItem.getName() + " ID: " + weaponPoisonItem.getId());
                    boolean success = Backpack.interact(weaponPoisonItem.getName(), "Apply");
                    Execution.delay(RandomGenerator.nextInt(500, 600));

                    if (!success) {
                        println("Failed to apply " + weaponPoisonItem.getName());
                    }
                } else {
                    println("No weapon poison found!");
                }
            }
        }
    }

    private boolean hasComponentWithSpriteId(int interfaceId, int spriteId) {
        ResultSet<Component> components = ComponentQuery.newQuery(interfaceId)
                .spriteId(spriteId)
                .results();
        return !components.isEmpty();
    }
    private void useCauldron() {
        if (getLocalPlayer() == null) {
            return;
        }

        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().id(127472).option("Drink from").results();

        if (!results.isEmpty()) {
            SceneObject cauldron = results.nearest();
            if (cauldron != null) {
                cauldron.interact("Drink from");
                println("Drinking from Cauldron!");
                Execution.delay(RandomGenerator.nextInt(2000, 3000));
                botState = BotState.BANKING; // Update this to the appropriate next state
            } else {
                println("Failed to interact with the Cauldron.");
            }
        }
    }
}