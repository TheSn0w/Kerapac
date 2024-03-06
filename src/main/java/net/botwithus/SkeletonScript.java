package net.botwithus;

import net.botwithus.api.game.hud.Dialog;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Equipment;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.items.GroundItemQuery;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.characters.player.Player;
import net.botwithus.rs3.game.scene.entities.item.GroundItem;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.util.RandomGenerator;
import net.botwithus.rs3.events.EventBus;
import net.botwithus.rs3.events.impl.ServerTickedEvent;

import java.util.*;
import java.util.concurrent.*;

import static net.botwithus.rs3.game.Client.getLocalPlayer;
import static net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer.LOCAL_PLAYER;


public class SkeletonScript extends LoopingScript {

    private BotState botState = BotState.IDLE;
    public boolean runScript;
    boolean UseScriptureOfWen;
    private int prayerPointsThreshold = 1000;
    boolean useprayer;
    boolean useoverload;
    boolean useSorrow;
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
    boolean useDarkness;
    private int loopCounter = 0;
    private int healthThreshold = 50;
    boolean startAtPortal;
    private boolean hasUsedInvokeDeath = false;

    public int getLoopCounter() {
        return loopCounter;
    }


    enum BotState {
        IDLE,
        PRAYER,
        KERAPACPORTAL,
        INTERACTWITHDIALOG,
        KERAPACPHASE1,
        KERAPACPHASE2,
        LOOTING,
        WARSRETREAT,
        RESTART_SCRIPT,
    }


    public SkeletonScript(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.sgc = new SkeletonScriptGraphicsContext(getConsole(), this);

        EventBus.EVENT_BUS.subscribe(this, ServerTickedEvent.class, this::onServerTicked);
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
                botState = BotState.IDLE;
            }
            kerapacPortalInitialized = true;
        }
        switch (botState) {
            case IDLE -> {
                hasInteractedWithLootAll = false;
                hasInteractedWithStart = false;
                hasUsedInvokeDeath = false;
                Execution.delay(RandomGenerator.nextInt(3000, 4000));
                int warsRetreatRegionId = ((3294 >> 6) << 8) + (10127 >> 6);
                Coordinate playerCoord = Client.getLocalPlayer().getCoordinate();
                int currentPlayerRegionId = ((playerCoord.getX() >> 6) << 8) + (playerCoord.getY() >> 6);
                if (currentPlayerRegionId != warsRetreatRegionId) {

                    useWarsRetreat();
                }
                println("We're idle!");
                destroyKeyIfDetected();
                Execution.delay(RandomGenerator.nextInt(1000, 2000));
                botState = BotState.PRAYER;
            }
            case PRAYER -> {
                handleCampfire();
            }
            case KERAPACPORTAL -> {
                InteractWithColloseum();
            }
            case INTERACTWITHDIALOG -> {
                InteractWithDialog();
            }
            case KERAPACPHASE1 -> {
                kerapacPhase1();
            }
            case KERAPACPHASE2 -> {
                monitorKerapacAnimations();
            }
            case LOOTING -> {
                deactivateScriptureOfWen();
                loot();
                DeativatePrayers();
            }
            case WARSRETREAT -> {
                ++loopCounter;
                useWarsRetreat();
            }
            case RESTART_SCRIPT -> {
                restartScript();
            }
        }
    }

    private void handleCampfire() {
        EntityResultSet<SceneObject> Campfire = SceneObjectQuery.newQuery().name("Campfire").option("Warm hands").results();
        if (getLocalPlayer() == null)
            return;
        ComponentQuery query = ComponentQuery.newQuery(284).spriteId(10931);
        ResultSet<Component> results = query.results();

        if (results.isEmpty() && !Campfire.isEmpty()) {
            SceneObject campfire = Campfire.nearest();
            if (campfire != null) {
                campfire.interact("Warm hands");
                println("Warming hands!");
                Execution.delayUntil(10000, () -> !results.isEmpty());
                handlePraying();
            }
        } else if (!results.isEmpty()) {
            println("Campfire buff is already active!");
            handlePraying();
        }
    }

    private void handlePraying() {
        EntityResultSet<SceneObject> altarOfWarResults = SceneObjectQuery.newQuery().name("Altar of War").results();
        if (getLocalPlayer() != null && getLocalPlayer().getPrayerPoints() < 9000) {
            if (!altarOfWarResults.isEmpty()) {
                SceneObject altar = altarOfWarResults.nearest();
                if (altar != null) {
                    altar.interact("Pray");
                    println("Prayer is below 9000, we're praying at the Altar of War!");
                    Execution.delayUntil(15000, () -> getLocalPlayer().getPrayerPoints() >= 9000);
                }
            }
        } else if (getLocalPlayer().getPrayerPoints() >= 9000) {
            println("Prayer is already high!");
            handleCauldron();
        }
    }

    private void handleCauldron() {
        if (VarManager.getVarbitValue(26037) == 0 && useCauldron) {
            EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().id(127472).option("Drink from").results();

            if (!results.isEmpty()) {
                SceneObject cauldron = results.nearest();
                if (cauldron != null) {
                    cauldron.interact("Drink from");
                    println("Drinking from Cauldron!");
                    Execution.delay(RandomGenerator.nextInt(2000, 3000));
                    handleBank();
                } else {
                    println("Failed to interact with the Cauldron.");
                }
            }
        } else {
            handleBank();
        }
    }

    private void handleBank() {
        EntityResultSet<SceneObject> BankChest = SceneObjectQuery.newQuery().name("Bank chest").results();
        if (getLocalPlayer() == null)
            return;
        if (!BankChest.isEmpty()) {
            SceneObject bank = BankChest.nearest();
            if (bank != null) {
                bank.interact("Load Last Preset from");
                println("Loading preset!");
                Execution.delay(RandomGenerator.nextInt(3000, 4000));
                handleAndInteractWithCrystal();
            }
        }
    }

    private void handleAndInteractWithCrystal() {
        if (getLocalPlayer() == null) {
            return;
        }


        EntityResultSet<SceneObject> crystals = SceneObjectQuery.newQuery().id(114749).option("Channel").results();
        SceneObject crystal = crystals.nearest();

        if (crystal != null) {
            if (!HaveMobile) {
                crystal.interact("Channel");
                Execution.delayUntil(15000, () -> getLocalPlayer().getAdrenaline() == 1000);
            } else {
                crystal.interact("Channel");
                boolean surgedAtCorrectLocation = Execution.delayUntil(15000, () -> {
                    Coordinate playerCoord = getLocalPlayer().getCoordinate();
                    if (playerCoord.getX() <= 3295 && playerCoord.getY() <= 10134) {
                        ActionBar.useAbility("Surge");
                        println("Surging");
                        return true;
                    }
                    return false;
                });

                if (surgedAtCorrectLocation) {
                    Execution.delay(200);
                    crystal.interact("Channel");
                    Execution.delayUntil(15000, () -> getLocalPlayer().getAdrenaline() == 1000);
                }
            }

            Execution.delay(RandomGenerator.nextInt(100, 200));
            handleKerepacPortal();
        }
    }

    private void handleKerepacPortal() {
        EntityResultSet<SceneObject> kerapacPortalQuery = SceneObjectQuery.newQuery().name("Portal (Kerapac)").results();
        if (!kerapacPortalQuery.isEmpty()) {
            SceneObject kerapacPortal = kerapacPortalQuery.nearest();
            if (kerapacPortal != null) {
                kerapacPortal.interact("Enter");
                println("Interacting with portal...");
                botState = BotState.KERAPACPORTAL;
            }
        }
    }

    public void restartScript() {
        this.botState = BotState.IDLE;

        this.hasInteractedWithLootAll = false;
        this.hasInteractedWithStart = false;
        this.runScript = false;
        println("Script has been restarted.");
    }

    private void activatePrayers() {
        boolean Ruination = VarManager.getVarbitValue(53280) == 0;
        boolean DeflectMagic = VarManager.getVarbitValue(16768) == 0;
        boolean ProtectMagic = VarManager.getVarbitValue(16745) == 0;
        boolean Sorrow = VarManager.getVarbitValue(53279) == 0;


        if (Ruination && useRuination) {
            ActionBar.usePrayer("Ruination");
            Execution.delay(RandomGenerator.nextInt(10, 20));
        }
        if (DeflectMagic && useDeflectMagic) {
            ActionBar.usePrayer("Deflect Magic");
            Execution.delay(RandomGenerator.nextInt(10, 20));
        }
        if (ProtectMagic && useProtectMagic) {
            ActionBar.usePrayer("Protect from Magic");
            Execution.delay(RandomGenerator.nextInt(10, 20));
        }
        if (Sorrow && useSorrow) {
            ActionBar.usePrayer("Sorrow");
            Execution.delay(RandomGenerator.nextInt(10, 20));
        }
    }

    private void useWarsRetreat() {
        if (getLocalPlayer() != null) {
            ActionBar.useAbility("War's Retreat Teleport");
            println("Using Wars Retreat!");
            Execution.delay(RandomGenerator.nextInt(2000, 3000));
            botState = BotState.IDLE;
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
            if (colloseum != null) {
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
            botState = BotState.KERAPACPHASE1;
        }
    }

    private Coordinate kerapacPhase1StartCoord = null;

    public void kerapacPhase1() {
        Player localPlayer = getLocalPlayer();
        if (localPlayer == null) {
            println("Local player not found, aborting Kerapac phase 1.");
            return;
        }

        kerapacPhase1StartCoord = localPlayer.getCoordinate();
        Execution.delay(RandomGenerator.nextInt(1000, 1500));

        if (ActionBar.getCooldown("Surge") == 0) {
            ActionBar.useAbility("Surge");
            println("Ability 'Surge' used. Waiting for cooldown...");
            Execution.delay(1800);

            if (ActionBar.getCooldown("Surge") == 0) {
                ActionBar.useAbility("Surge");
                println("Ability 'Surge' used again after cooldown.");
                Execution.delay(1800);
            }
        }

        if (ActionBar.getCooldown("Conjure Undead Army") == 0) {
            ActionBar.useAbility("Conjure Undead Army");
            println("Ability 'Conjure Undead Army' used.");
            Execution.delay(RandomGenerator.nextInt(100, 200));
        }

        activatePrayers();

        if (UseScriptureOfWen) {
            manageScriptureOfWen();
        }
        Execution.delay(RandomGenerator.nextInt(10, 20));

        if (useoverload) {
            drinkOverloads();
        }
        Execution.delay(RandomGenerator.nextInt(50, 100));

        if (useWeaponPoison) {
            useWeaponPoison();
        }
        Execution.delay(RandomGenerator.nextInt(50, 100));

        botState = BotState.KERAPACPHASE2;
    }

    private void monitorKerapacAnimations() {
        if (useprayer) {
            usePrayerOrRestorePots();
        }
        Execution.delay(RandomGenerator.nextInt(10, 20));
        if (eatfood) {
            eatFood();
        }
        Execution.delay(RandomGenerator.nextInt(10, 20));
        if (useoverload) {
            drinkOverloads();
        }
        Execution.delay(RandomGenerator.nextInt(10, 20));

        if (useWeaponPoison) {
            useWeaponPoison();
        }
        Execution.delay(RandomGenerator.nextInt(10, 20));
        if (useDarkness) {
            useDarkness();
        }
        Execution.delay(RandomGenerator.nextInt(10, 20));
        if (useSaraBrew) {
            UseSaraBrew();
        }
        Execution.delay(RandomGenerator.nextInt(10, 20));
        if (useSaraBrewandBlubber) {
            UseSaraandBlubber();
        }
        Execution.delay(RandomGenerator.nextInt(10, 20));


        weAreDead();

        Npc kerapac = NpcQuery.newQuery().name("Kerapac, the bound").results().first();
        if (kerapac != null && getLocalPlayer() != null) {

            int animationID = kerapac.getAnimationId();
            handleAnimation(kerapac, animationID);

            if (getLocalPlayer().getTarget() != null && useVulnBomb) {
                int vulnDebuffVarbit = VarManager.getVarbitValue(1939);
                if (vulnDebuffVarbit == 0 && kerapac.getCurrentHealth() > 100000 && Backpack.contains("Vulnerability bomb")) {
                    boolean success = ActionBar.useItem("Vulnerability bomb", "Throw");
                    if (success) {
                        println("Eat this vuln bomb!   " + getLocalPlayer().getTarget().getName());
                        Execution.delayUntil(RandomGenerator.nextInt(2000, 3000), () -> !getLocalPlayer().inCombat());
                    } else {
                        println("Failed to use Vulnerability bomb.");
                    }
                }
            }
            if (Interfaces.isOpen(1181)) {
                ComponentQuery phaseQuery = ComponentQuery.newQuery(1181).componentIndex(21);
                ResultSet<Component> phaseResults = phaseQuery.results();
                Component phaseComponent = phaseResults.first();

                if (useInvokeDeath && phaseComponent != null && "Phase: 4".equals(phaseComponent.getText()) && !hasUsedInvokeDeath) {
                    ComponentQuery query = ComponentQuery.newQuery(1490).spriteId(30100);
                    ResultSet<Component> results = query.results();

                    if (results.isEmpty()) {
                        if (ActionBar.getCooldown("Invoke Death") == 0) {
                            ActionBar.useAbility("Invoke Death");
                            println("Used 'Invoke Death'");
                            hasUsedInvokeDeath = true;
                            Execution.delay(600);
                        }
                    }
                }

                if (useLuckoftheDwarves && kerapac.getCurrentHealth() < 60000 && "Phase: 4".equals(phaseComponent.getText())) {
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
        }
    }

    private boolean shouldSurge = false;
    private boolean surged = false;
    private boolean hasUsedAnticipation = false;
    private boolean surgeMessagePrinted = false;
    private boolean firstAnimationEncountered = false;

    private void handleAnimation(Npc npc, int animationId) {
        Npc kerapac = NpcQuery.newQuery().name("Kerapac, the bound").results().first();
        if (kerapac == null && getLocalPlayer() == null) {
            println("Npc not found, aborting animation handling.");
            return;
        }
        switch (animationId) {
            case 34193:
                if (kerapac != null && kerapac.getCurrentHealth() <= 180000) {
                    // Check if this is the first time encountering the animation
                    if (!firstAnimationEncountered) {
                        firstAnimationEncountered = true; // Mark the first animation as encountered
                        println("Ignoring the spawn animation.");
                        Execution.delay(4000); // Add a delay of 4000ms for the first animation encounter
                    } else {
                        // For subsequent encounters, proceed with the surge logic
                        shouldSurge = true;
                        if (!surgeMessagePrinted) {
                            println("Detected animation where surging should be considered.");
                            surgeMessagePrinted = true;
                        }
                    }
                }
                break;
            case 34194:
                if (shouldSurge && !surged) {
                    println("He's Flying High.... dodging this.");
                    Execution.delay(RandomGenerator.nextInt(350, 400));
                    ActionBar.useAbility("Surge");
                    println("Dodged MWAHA!");
                    surged = true;
                }

                if (surged && npc.validate()) {
                    Execution.delay(RandomGenerator.nextInt(100, 200));
                    npc.interact("Attack");
                    println("Attacking Kerapac after dodging.");
                    shouldSurge = false;
                    surged = false;
                }
                break;
            case 34195:
                if (getLocalPlayer() != null) {
                    Coordinate threeTilesAway = getLocalPlayer().getCoordinate().derive(+3, 0, 0);

                    if (Travel.walkTo(threeTilesAway)) {
                        println("Were toooo slow, moving away!.");
                        Execution.delay(RandomGenerator.nextInt(1750, 1850));
                        npc.interact("Attack");
                        println("Re-engaging Kerapac after walking away.");
                    }
                }
                return;
            case 34198:
                if (getLocalPlayer() != null && kerapac != null && kerapac.getCurrentHealth() >= 65000) {
                    println("Kerapac trying to stun us, WHAAAA ....");
                    Coordinate kerapacLocation = Objects.requireNonNull(kerapac.getCoordinate());

                    // Move to Kerapac's location
                    if (Travel.walkTo(kerapacLocation)) {
                        println("Moving towards Kerapac...");

                        if (ActionBar.getCooldown("Anticipation") == 0 && !hasUsedAnticipation) {
                            ActionBar.useAbility("Anticipation");
                            println("Used 'Anticipation' to avoid Kerapac's stun.");
                            hasUsedAnticipation = true;
                        }
                        Execution.delayUntil(5000, () -> kerapac.getAnimationId() != 34198);


                        if (kerapac.validate()) {
                            hasUsedAnticipation = false;
                            kerapac.interact("Attack");
                            println("Attacking Kerapac after reaching his location.");
                        }
                    }
                }
                break;
            case 34186:
                println("Target animation detected, moving on to the next state.");
                botState = BotState.LOOTING;
                break;
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
                            Execution.delayUntil(100, () -> Distance.between(getLocalPlayer().getCoordinate(), groundItem.getCoordinate()) <= 10); // Wait for the player to walk to the item
                        }
                        if (groundItem.interact("Take")) {
                            println("Taking " + itemName);
                            Execution.delay(RandomGenerator.nextInt(600, 700));
                        }

                        Execution.delayUntil(15000, () -> Interfaces.isOpen(1622));
                        Execution.delay(RandomGenerator.nextInt(800, 1000));
                        LootAll();
                        break;
                    }
                    Execution.delay(RandomGenerator.nextInt(200, 300));
                }

                botState = BotState.WARSRETREAT;
            }
            if (getLocalPlayer().getAnimationId() == -1 && !getLocalPlayer().isMoving()) {
                if (System.currentTimeMillis() - lastAnimationTime > 10000) {
                    print("No animation detected for 10 seconds, Teleporting to Wars.");
                    botState = BotState.WARSRETREAT;
                }
            }
        }
    }
    private long lastAnimationTime = System.currentTimeMillis();
    private void DeativatePrayers() {
        boolean useRuination1 = VarManager.getVarbitValue(53280) == 1;
        boolean useDeflectMagic1 = VarManager.getVarbitValue(16768) == 1;
        boolean useProtectMagic1 = VarManager.getVarbitValue(16745) == 1;
        boolean useSorrow1 = VarManager.getVarbitValue(53279) == 1;


        if (useRuination1 && (useRuination)) {
            ActionBar.usePrayer("Ruination");
            Execution.delay(RandomGenerator.nextInt(10, 15));
        }
        if (useDeflectMagic1 && useDeflectMagic) {
            ActionBar.usePrayer("Deflect Magic");
            Execution.delay(RandomGenerator.nextInt(10, 15));
        }
        if (useProtectMagic1 && useProtectMagic) {
            ActionBar.usePrayer("Protect Magic");
            Execution.delay(RandomGenerator.nextInt(10, 15));
        }
        if (useSorrow1 && useSorrow) {
            ActionBar.usePrayer("Sorrow");
            Execution.delay(RandomGenerator.nextInt(10, 15));
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
        if (getLocalPlayer() == null)
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
                    if (eat != null) {
                        Backpack.interact(eat.getName(), 1);
                        println("Eating " + eat.getName());
                        Execution.delayUntil(RandomGenerator.nextInt(300, 500), () -> getLocalPlayer().getCurrentHealth() > 8000);
                    } else {
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


    private boolean weAreDead() {
        if (getLocalPlayer() != null && getLocalPlayer().getCurrentHealth() == 0) {
            boolean useRuination = VarManager.getVarbitValue(53280) == 1;
            boolean useDeflectMagic = VarManager.getVarbitValue(16768) == 1;
            boolean useProtectMagic = VarManager.getVarbitValue(16745) == 1;
            boolean useSorrow = VarManager.getVarbitValue(53279) == 1;


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
            if (useSorrow) {
                ActionBar.usePrayer("Sorrow");
                Execution.delay(RandomGenerator.nextInt(10, 20));
            }
            Execution.delay(RandomGenerator.nextInt(5000, 7500));
            println("We're dead, teleporting to Wars Retreat!");
            botState = BotState.WARSRETREAT;
            return true; // Indicate that the player is dead
        }
        return false; // Player is not dead
    }

    private void exitGate() {
        Execution.delay(RandomGenerator.nextInt(1250, 1500));
        EntityResultSet<SceneObject> results = SceneObjectQuery.newQuery().name("Gate").option("Exit").results();

        if (!results.isEmpty()) {
            SceneObject gate = results.nearest();
            if (gate != null && gate.interact("Exit")) {
                println("Exiting colosseum!");
                boolean success = Execution.delayUntil(5000, () -> Interfaces.isOpen(1188));
                if (success) {
                    exitDialog();
                } else {
                    exitGate();
                    println("Failed to interact with the gate.");
                }
                return;
            }
        }
        println("Gate not found or unable to interact.");
    }

    private void exitDialog() {
        Execution.delay(RandomGenerator.nextInt(200, 300));
        Dialog.interact("Yes, exit.");
        println("Interacting with dialog!");
        Execution.delay(RandomGenerator.nextInt(1200, 1700));
        botState = BotState.KERAPACPORTAL;
    }

    private boolean isDarknessActive() {
        Component darkness = ComponentQuery.newQuery(284).spriteId(30122).results().first();
        return darkness != null;
    }

    private void useDarkness() {
        if (getLocalPlayer() != null) {
            if (!isDarknessActive()) {
                ActionBar.useAbility("Darkness");
                println("Using darkness!");
                Execution.delay(RandomGenerator.nextInt(2000, 3000));
            }
        }
    }

    private void UseSaraBrew() {
        if (Client.getLocalPlayer() != null) {
            if (Client.getLocalPlayer().getCurrentHealth() * 100 / Client.getLocalPlayer().getMaximumHealth() < healthThreshold) {
                ResultSet<Item> items = InventoryItemQuery.newQuery().results();

                Item saraBrew = items.stream()
                        .filter(item -> item.getName() != null && item.getName().toLowerCase().contains("saradomin"))
                        .findFirst()
                        .orElse(null);

                if (saraBrew != null) {
                    Backpack.interact(saraBrew.getName(), "Drink");
                    println("Drinking " + saraBrew.getName());
                    Execution.delayUntil(RandomGenerator.nextInt(1800, 2000), () -> {
                        LocalPlayer player = Client.getLocalPlayer();
                        if (player != null) {
                            double healthPercentage = (double) player.getCurrentHealth() / player.getMaximumHealth() * 100;
                            return healthPercentage > 90;
                        }
                        return false;
                    });
                } else {
                    println("No Saradomin brews found!");
                }
            }
        }
    }

    private void UseSaraandBlubber() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player != null) {
            double healthPercentage = (double) player.getCurrentHealth() / player.getMaximumHealth() * 100;
            if (healthPercentage < healthThreshold) {
                ResultSet<Item> items = InventoryItemQuery.newQuery().results();

                Item saraBrew = items.stream()
                        .filter(item -> item.getName() != null && item.getName().toLowerCase().contains("saradomin"))
                        .findFirst()
                        .orElse(null);

                if (saraBrew != null) {
                    Backpack.interact(saraBrew.getName(), "Drink");
                    println("Drinking " + saraBrew.getName());
                } else {
                    println("No Saradomin brews found!");
                }

                Item blubberItem = items.stream()
                        .filter(item -> item.getName() != null && item.getName().toLowerCase().contains("blubber"))
                        .findFirst()
                        .orElse(null);

                if (blubberItem != null) {
                    Backpack.interact(blubberItem.getName(), "Eat");
                    println("Eating " + blubberItem.getName());
                } else {
                    println("No blubber items found!");
                }

                Execution.delayUntil(RandomGenerator.nextInt(1800, 2000), () -> {
                    LocalPlayer currentPlayer = Client.getLocalPlayer();
                    if (currentPlayer != null) {
                        double currentHealthPercentage = (double) currentPlayer.getCurrentHealth() / currentPlayer.getMaximumHealth() * 100;
                        return currentHealthPercentage > 90;
                    }
                    return false;
                });
            }
        }
    }
    public void destroyKeyIfDetected() { // did chatgpt convert this correct Cipher? also i dont have comp.interact come up on my thing
        Random rand = new Random();
        Component thKey = ComponentQuery.newQuery(1473).itemName("Key token").results().first();
        if (thKey != null) {
            Execution.delay(rand.nextInt(80) + 50);
            if (Interfaces.isOpen(1183)) {
                println("Destroy key confirmed: " + MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77529093));
            } else {
                println("Destroy key from inventory: " + thKey.interact("Destroy"));
            }
        } else {
            println("No TH key found to destroy.");
        }
    }
    private void onServerTicked(ServerTickedEvent event) {
        // Your logic here, e.g., logging the tick count
        System.out.println("Server ticked, tick count: " + event.getTicks());
    }
}

