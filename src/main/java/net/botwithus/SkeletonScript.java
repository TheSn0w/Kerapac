package net.botwithus;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Equipment;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.events.EventBus;
import net.botwithus.rs3.events.impl.ServerTickedEvent;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.movement.Movement;
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
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.util.RandomGenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.botwithus.rs3.game.Client.getLocalPlayer;
import static net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer.LOCAL_PLAYER;


public class SkeletonScript extends LoopingScript {

    private BotState botState = BotState.IDLE;
    public boolean runScript;
    boolean UseScriptureOfWen;
    private final Queue<Runnable> abilityRotation = new LinkedList<>();
    private int prayerPointsThreshold = 5000;
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
    private boolean luckOfTheDwarvesUsed = false;
    private long lastVulnBombAttemptTime = 0;
    private int tickCounter = 0;

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
        this.sgc = new SkeletonScriptGraphicsContext(this.getConsole(), this);
        loadConfiguration();
        this.loopDelay = RandomGenerator.nextInt(100, 200);
        EventBus.EVENT_BUS.subscribe(this, ServerTickedEvent.class, this::onServerTick);
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
        weAreDead();
        //logBoss();
        switch (botState) {
            case IDLE -> {
                saveConfiguration();
                hasInteractedWithLootAll = false;
                hasInteractedWithStart = false;
                hasUsedInvokeDeath = false;
                luckOfTheDwarvesUsed = false;
                IdleDelays();
                DeactivatePrayers(); //just in case they're on
                deactivateScriptureOfWen();
                botState = BotState.PRAYER;
            }
            case PRAYER ->
                handleCampfire();

            case KERAPACPORTAL ->
                InteractWithColloseum();

            case INTERACTWITHDIALOG ->
                InteractWithDialog();

            case KERAPACPHASE1 ->
                kerapacPhase1();

            case KERAPACPHASE2 ->
                monitorKerapacAnimations();

            case LOOTING -> {
                firstAnimationEncountered = false;
                DeactivatePrayers();
                loot();
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

    private void IdleDelays() {
        if (getLocalPlayer() != null)
            if (getLocalPlayer().getCoordinate().getRegionId() != 13214) {
                useWarsRetreat();
            }
        Execution.delay(RandomGenerator.nextInt(1000, 2000));
        println("We're idle!");
        destroyKeyIfDetected();
        Execution.delay(RandomGenerator.nextInt(1000, 2000));
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
        int maxPrayerPoints = Skills.PRAYER.getLevel() * 100;
        int maxSummoningPoints = Skills.SUMMONING.getLevel() * 100;

        EntityResultSet<SceneObject> altarOfWarResults = SceneObjectQuery.newQuery().name("Altar of War").results();

        if (getLocalPlayer() != null && (getLocalPlayer().getPrayerPoints() < maxPrayerPoints || getLocalPlayer().getSummoningPoints() < maxSummoningPoints)) {
            if (!altarOfWarResults.isEmpty()) {
                SceneObject altar = altarOfWarResults.nearest();
                if (altar != null && altar.interact("Pray")) {
                    println("Prayer/Summoning points are below " + maxPrayerPoints + "," + maxSummoningPoints + ". Praying at Altar of War!");
                    boolean success = Execution.delayUntil(15000, () ->
                            getLocalPlayer().getPrayerPoints() >= maxPrayerPoints && getLocalPlayer().getSummoningPoints() >= maxSummoningPoints);
                    if (!success) {
                        println("Failed to restore Prayer/Summoning points within the timeout.");
                    }
                }
            } else {
                println("No Altar of War found.");
            }
        } else if (getLocalPlayer() != null) {
            if (getLocalPlayer().getPrayerPoints() >= maxPrayerPoints) {
                println("Were already at Maximum");
                handleCauldron();
            }
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
                    Execution.delayUntil(10000, () -> VarManager.getVarbitValue(26037) != 0);
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
                Execution.delay(RandomGenerator.nextInt(1750, 2500));
                manageFamiliarSummoning();
            }
        }
    }
    public void manageFamiliarSummoning() {
        boolean isFamiliarSummoned = isFamiliarSummoned();
        int familiarTimeRemaining = VarManager.getVarbitValue(6055);

        if (isFamiliarSummoned) {
            familiarTimeRemaining = VarManager.getVarbitValue(6055);
            println("Familiar time remaining: " + familiarTimeRemaining + " Minutes");
        }


        if (!isFamiliarSummoned || familiarTimeRemaining <= 5) {
            summonFamiliar();
        } else {
            int scrollsStored = getScrollsStored();
            boolean hasScrolls = hasScrollsInInventory();
            println("Backpack contains Scrolls: " + hasScrolls + ", Scrolls stored in Familiar: " + scrollsStored);

            if (scrollsStored <= 50 && hasScrolls) {
                println("Handling inventory scrolls...");
                handleInventoryScrolls();
            } else {
                println("Handling and interacting with crystal...");
                handleAndInteractWithCrystal();
            }
        }
    }

    private void handleInventoryScrolls() {
        if (hasScrollsInInventory()) {
            storeMaxScrolls();
        } else {
            println("No scrolls found in inventory.");
            handleAndInteractWithCrystal();
        }
    }

    private boolean hasScrollsInInventory() {
        ResultSet<Item> scrolls = InventoryItemQuery.newQuery(93).results();
        return scrolls.stream().anyMatch(item -> item.getName() != null && item.getName().toLowerCase().contains("scroll"));
    }
    private int getScrollsStored() {
        return VarManager.getVarbitValue(25412);
    }

    private void summonFamiliar() {
        ResultSet<Item> items = InventoryItemQuery.newQuery(93).results();

        Item itemToSummon = items.stream()
                .filter(item -> item.getName() != null && (item.getName().toLowerCase().contains("pouch") || item.getName().toLowerCase().contains("contract")))
                .findFirst()
                .orElse(null);

        if (itemToSummon != null) {
            println("Attempting to summon with: " + itemToSummon.getName());
            boolean success = Backpack.interact(itemToSummon.getName(), "Summon");
            Execution.delay(RandomGenerator.nextInt(1600, 2100));

            if (success) {
                println("Summoned familiar with: " + itemToSummon.getName());
                boolean familiarSummoned = Execution.delayUntil(10000, this::isFamiliarSummoned);
                if (familiarSummoned) {
                    println(itemToSummon.getName() + " is now summoned.");
                    manageFamiliarSummoning();
                } else {
                    println("Failed to confirm the summoning of familiar with " + itemToSummon.getName() + ".");
                    handleBank();
                }
            } else {
                println("Failed to summon familiar with: " + itemToSummon.getName());
                handleBank();
            }
        } else {
            println("No suitable 'pouch' or 'contract' items for summoning found in Backpack. Handling and interacting with crystal...");
            handleAndInteractWithCrystal();
        }
    }

    private void storeMaxScrolls() {
        println("Attempting to store scrolls in familiar.");
        boolean success = ComponentQuery.newQuery(662).componentIndex(78).results().first().interact(1);
        Execution.delay(RandomGenerator.nextInt(800, 1000));
        if (success) {
            println("Successfully stored scrolls in familiar.");
            handleAndInteractWithCrystal();
        } else {
            println("Failed to store scrolls in familiar.");
            botState = BotState.PRAYER;
        }
    }
    private boolean isFamiliarSummoned() {
        Component familiarComponent = ComponentQuery.newQuery(284).spriteId(26095).results().first();
        return familiarComponent != null;
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
                        ScriptConsole.println("Used Surge: " + ActionBar.useAbility("Surge"), new Object[0]);
                        return true;
                    }
                    return false;
                });

                if (surgedAtCorrectLocation) {
                    Execution.delay(RandomGenerator.nextInt(200, 400));
                    crystal.interact("Channel");
                    Execution.delayUntil(15000, () -> getLocalPlayer().getAdrenaline() == 1000);
                }
            }

            Execution.delay(RandomGenerator.nextInt(250, 500));
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

        if (useRuination && Ruination) {
            ScriptConsole.println("Activate Ruination: " + ActionBar.useAbility("Ruination"));
            Execution.delay(RandomGenerator.nextInt(600, 650));
        }
        if (useDeflectMagic && DeflectMagic) {
            ScriptConsole.println("Activate Deflect Magic: " + ActionBar.useAbility("Deflect Magic"));
            Execution.delay(RandomGenerator.nextInt(600, 650));
        }
        if (useProtectMagic && ProtectMagic) {
            ScriptConsole.println("Activate Protect from Magic: " + ActionBar.useAbility("Protect from Magic"));
            Execution.delay(RandomGenerator.nextInt(600, 650));
        }
        if (useSorrow && Sorrow) {
            ScriptConsole.println("Activate Sorrow: " + ActionBar.useAbility("Sorrow"));
            Execution.delay(RandomGenerator.nextInt(600, 650));
        }
    }

    private void useWarsRetreat() {
        if (getLocalPlayer() != null) {
            ScriptConsole.println("Used Wars Retreat: " + ActionBar.useAbility("War's Retreat Teleport"), new Object[0]);
            Execution.delay(RandomGenerator.nextInt(4000, 5000));
            botState = BotState.IDLE;
        }
    }

    public boolean WalkTo(int x, int y) {
        if (getLocalPlayer() != null) {
            Coordinate myPos = getLocalPlayer().getCoordinate();
            if (myPos.getX() != x && myPos.getY() != y) {

                if (!getLocalPlayer().isMoving()) {
                    println("Walking to: " + x + ", " + y);
                    Movement.walkTo(x, y, true);
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

    private final Coordinate kerapacPhase1StartCoord = null;

    public void kerapacPhase1() {
        Player localPlayer = getLocalPlayer();
        if (localPlayer == null) {
            println("Local player not found, aborting Kerapac phase 1.");
            return;
        }
        if (UseScriptureOfWen) {
            activateScriptureOfWen();
        }
        if (useoverload) {
            drinkOverloads();
        }
        if (useWeaponPoison) {
            useWeaponPoison();
        }
        if (useDarkness) {
            useDarkness();
        }

        Coordinate kerapacPhase1StartCoord = Client.getLocalPlayer().getCoordinate();
        Execution.delay(RandomGenerator.nextInt(1000, 1500));

        boolean firstSurgeSuccessful = false;
        boolean secondSurgeSuccessful = false;

        if (ActionBar.getCooldown("Surge") == 0) {
            ScriptConsole.println("Used Surge: " + ActionBar.useAbility("Surge"));
            Execution.delay(RandomGenerator.nextInt(1850, 1900));
            firstSurgeSuccessful = true;

            if (ActionBar.getCooldown("Surge") == 0) {
                ScriptConsole.println("Used Surge: " + ActionBar.useAbility("Surge"));
                Execution.delay(RandomGenerator.nextInt(1800, 1900));
                secondSurgeSuccessful = true;
            }
        } else {
            ScriptConsole.println("Surge is on cooldown.");
        }


        Coordinate expectedFirstSurgeCoord = kerapacPhase1StartCoord.derive(-10, 0, 0);
        Coordinate expectedSecondSurgeCoord = kerapacPhase1StartCoord.derive(-20, 0, 0);


        Coordinate currentPlayerCoord = Client.getLocalPlayer().getCoordinate();

        if (firstSurgeSuccessful && secondSurgeSuccessful && currentPlayerCoord.equals(expectedSecondSurgeCoord)) {
            ScriptConsole.println("Used Conjure Undead Army: " + ActionBar.useAbility("Conjure Undead Army"));
            Execution.delay(RandomGenerator.nextInt(100, 200));
        } else if (!currentPlayerCoord.equals(expectedSecondSurgeCoord)) {
            Coordinate adjustCoord = kerapacPhase1StartCoord.derive(-20, 0, 0);
            Movement.walkTo(adjustCoord.getX(), adjustCoord.getY(), true);
            Execution.delay(RandomGenerator.nextInt(1000, 1500));

            ScriptConsole.println("Used Conjure Undead Army: " + ActionBar.useAbility("Conjure Undead Army"));
            Execution.delay(RandomGenerator.nextInt(100, 200));
        }


        botState = BotState.KERAPACPHASE2;
    }

    private void monitorKerapacAnimations() {
        if (useprayer) {
            usePrayerOrRestorePots();
        }
        if (eatfood) {
            eatFood();
        }
        if (useSaraBrew) {
            UseSaraBrew();
        }
        if (useSaraBrewandBlubber) {
            UseSaraandBlubber();
        }
        activatePrayers();


        Npc kerapac = NpcQuery.newQuery().name("Kerapac, the bound").results().first();
        if (kerapac != null && getLocalPlayer() != null) {

            int animationID = kerapac.getAnimationId();
            handleAnimation(kerapac, animationID);

            if (getLocalPlayer().getTarget() != null && useVulnBomb) {
                int vulnDebuffVarbit = VarManager.getVarbitValue(1939);

                if (System.currentTimeMillis() - lastVulnBombAttemptTime > 5000 &&
                        vulnDebuffVarbit == 0 && kerapac.getCurrentHealth() > 100000 &&
                        Backpack.contains("Vulnerability bomb")) {

                    boolean success = ActionBar.useItem("Vulnerability bomb", "Throw");
                    ScriptConsole.println("Used Vulnerability Bomb: " + success);
                    lastVulnBombAttemptTime = System.currentTimeMillis();
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
                            ScriptConsole.println("Used Invoke Death: " + ActionBar.useAbility("Invoke Death"), new Object[0]);
                            hasUsedInvokeDeath = true;
                        }
                    }
                }

                if (useLuckoftheDwarves && !luckOfTheDwarvesUsed && phaseComponent != null && "Phase: 4".equals(phaseComponent.getText())) {
                    ResultSet<Item> luckResults = InventoryItemQuery.newQuery().name("Luck of the Dwarves").results();
                    if (!luckResults.isEmpty()) {
                        Item luckOfTheDwarves = luckResults.first();
                        if (luckOfTheDwarves != null && luckOfTheDwarves.getStackSize() > 0) {
                            boolean success = Backpack.interact(luckOfTheDwarves.getName(), "Wear");
                            if (success) {
                                println("Wearing 'Luck of the Dwarves'");
                                luckOfTheDwarvesUsed = true;
                            }
                        }
                    } else {
                        println("No 'Luck of the Dwarves' found!");
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
                    if (!firstAnimationEncountered) {
                        firstAnimationEncountered = true;
                        println("Ignoring the spawn animation.");
                        Execution.delayUntil(RandomGenerator.nextInt(5000, 10000), () -> kerapac.getAnimationId() == 34192);
                    } else {
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
                    int delayTime = RandomGenerator.nextInt(getMinDelay(), getMaxDelay());
                    println("Getting ready to Surge.");
                    println("Delaying for " + delayTime + " milliseconds before using Surge.");
                    Execution.delay(delayTime);
                    ScriptConsole.println("Used Surge: " + ActionBar.useAbility("Surge"));
                    surged = true;
                }

                if (surged && npc != null) {
                    Execution.delay(RandomGenerator.nextInt(600, 650));
                    npc.interact("Attack");
                    println("Attacking Kerapac after Surging.");
                    shouldSurge = false;
                    surged = false;
                }
                break;
            case 34195:
                if (getLocalPlayer() != null) {
                    Coordinate fiveTilesAway = getLocalPlayer().getCoordinate().derive(+5, 0, 0);

                    Movement.walkTo(fiveTilesAway.getX(), fiveTilesAway.getY(), true); {
                        println("`check the delays in the gui if you see this message`");
                        Execution.delayUntil(10000, () -> fiveTilesAway.equals(getLocalPlayer().getCoordinate()));
                        npc.interact("Attack");
                        println("Re-engaging Kerapac after walking away.");
                    }
                }
                break;
            case 34198:
                if (getLocalPlayer() != null && kerapac != null && kerapac.getCurrentHealth() >= 65000) {
                    Coordinate kerapacLocation = Objects.requireNonNull(kerapac.getCoordinate());

                    Movement.walkTo(kerapacLocation.getX(), kerapacLocation.getY(), true);
                    {
                        println("Moving towards Kerapac...");
                        Execution.delayUntil(10000, () -> !getLocalPlayer().isMoving());

                         if  (ActionBar.getCooldown("Anticipation") == 0 && !hasUsedAnticipation) {
                            ScriptConsole.println("Used Anticipation: " + ActionBar.useAbility("Anticipation"));
                            hasUsedAnticipation = true;
                        }
                        Execution.delayUntil(RandomGenerator.nextInt(2000, 3000), () -> kerapac.getAnimationId() != 34198);


                        if (kerapac.getOptions().contains("Attack")) {
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
                        Execution.delayUntil(RandomGenerator.nextInt(5000, 5500), () -> getLocalPlayer().isMoving());

                        if (getLocalPlayer().isMoving() && groundItem.getCoordinate() != null && Distance.between(getLocalPlayer().getCoordinate(), groundItem.getCoordinate()) > 10) {
                            ScriptConsole.println("Used Surge: " + ActionBar.useAbility("Surge"));
                            Execution.delay(RandomGenerator.nextInt(200, 250));
                        }

                        if (groundItem.getCoordinate() != null) {
                            Execution.delayUntil(RandomGenerator.nextInt(100, 200), () -> Distance.between(getLocalPlayer().getCoordinate(), groundItem.getCoordinate()) <= 10);
                        }

                        if (groundItem.interact("Take")) {
                            println("Taking " + itemName);
                            Execution.delay(RandomGenerator.nextInt(600, 700));
                        }

                        boolean interfaceOpened = Execution.delayUntil(15000, () -> Interfaces.isOpen(1622));
                        if (!interfaceOpened) {
                            println("Interface 1622 did not open. Attempting to interact with ground item again.");
                            if (groundItem.interact("Take")) {
                                println("Attempting to take " + itemName + " again.");
                                Execution.delay(RandomGenerator.nextInt(250, 300));
                            }
                        }
                        TimeOut();
                        LootAll();
                        break;
                    }
                    Execution.delay(RandomGenerator.nextInt(250, 350));
                }
                botState = BotState.WARSRETREAT;
            }
        }
    }

    private void TimeOut() {
        if (getLocalPlayer() != null) {
            if (System.currentTimeMillis() - lastAnimationTime > 30000) {
                print("No animation detected for 10 seconds, Teleporting to Wars.");
                botState = BotState.WARSRETREAT;
            }
        }
    }

    private long lastAnimationTime = System.currentTimeMillis();


    private void DeactivatePrayers() {
        boolean ruinationActive = VarManager.getVarbitValue(53280) != 0;
        boolean deflectMagicActive = VarManager.getVarbitValue(16768) != 0;
        boolean protectMagicActive = VarManager.getVarbitValue(16745) != 0;
        boolean sorrowActive = VarManager.getVarbitValue(53279) != 0;

        if (useRuination && ruinationActive) {
            ScriptConsole.println("Deactivated Ruination: " + ActionBar.useAbility("Ruination"));
        }
        if (useDeflectMagic && deflectMagicActive) {
            ScriptConsole.println("Deactivated Deflect Magic: " + ActionBar.useAbility("Deflect Magic"));
        }
        if (useProtectMagic && protectMagicActive) {
            ScriptConsole.println("Deactivated Protect from Magic: " + ActionBar.useAbility("Protect from Magic"));
        }
        if (useSorrow && sorrowActive) {
            ScriptConsole.println("Deactivated Sorrow: " + ActionBar.useAbility("Sorrow"));
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
                Execution.delay(RandomGenerator.nextInt(500, 600));
                println("Scripture of Wen activated successfully.");
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
                Execution.delay(RandomGenerator.nextInt(500, 600));
                println("Scripture of Wen deactivated.");
            }
        }
    }


    public void setPrayerPointsThreshold(int threshold) {
        this.prayerPointsThreshold = threshold;
    }

    public void setHealthThreshold(int healthThreshold) {
        this.healthThreshold = healthThreshold;
    }

    private boolean potionUsedSinceThreshold = false;

    public void usePrayerOrRestorePots() {
        if (getLocalPlayer() != null) {
            int currentPrayerPoints = getLocalPlayer().getPrayerPoints();

            if (currentPrayerPoints >= prayerPointsThreshold) {
                potionUsedSinceThreshold = false;
                return;
            }

            if (!potionUsedSinceThreshold) {
                ResultSet<Item> items = InventoryItemQuery.newQuery(93).results();

                Item prayerOrRestorePot = items.stream()
                        .filter(item -> item.getName() != null &&
                                (item.getName().toLowerCase().contains("prayer") ||
                                        item.getName().toLowerCase().contains("restore")))
                        .findFirst()
                        .orElse(null);

                if (prayerOrRestorePot != null) {
                    println("Attempting to drink " + prayerOrRestorePot.getName());
                    boolean success = Backpack.interact(prayerOrRestorePot.getName(), "Drink");
                    if (success) {
                        println("Used " + prayerOrRestorePot.getName());
                        potionUsedSinceThreshold = true;
                    } else {
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
                    return;
                }

                ResultSet<Item> items = InventoryItemQuery.newQuery(93)
                        .results();

                Item overloadItem = items.stream()
                        .filter(item -> item.getName() != null &&
                                item.getName().toLowerCase().contains("overload"))
                        .findFirst()
                        .orElse(null);

                if (overloadItem != null) {
                    println("Drinking overload " + overloadItem.getName() + " ID: " + overloadItem.getId());
                    boolean success = Backpack.interact(overloadItem.getName(), "Drink");

                    if (!success) {
                        println("Failed to drink " + overloadItem.getName());
                    }
                } else {
                    println("No overload found!");
                }
            }
        }
    }
    private boolean hasEatenFood = false;
    private int ticksAfterEating = 0;
    public void eatFood() {
        if (getLocalPlayer() != null) {
            int currentHealth = getLocalPlayer().getCurrentHealth();
            int maximumHealth = getLocalPlayer().getMaximumHealth();

            int healthPercentage = currentHealth * 100 / maximumHealth;
            if (healthPercentage < healthThreshold && !hasEatenFood) {
                ResultSet<Item> foodItems = InventoryItemQuery.newQuery(93).option("Eat").results();

                if (!foodItems.isEmpty()) {
                    Item food = foodItems.first();
                    if (food != null) {
                        println("Attempting to eat " + food.getName());
                        boolean success = Backpack.interact(food.getName(), 1);
                        if (success) {
                            println("Eating " + food.getName());
                            hasEatenFood = true;
                            ticksAfterEating = 0;
                        } else {
                            println("Failed to eat " + food.getName());
                        }
                    }
                } else {
                    println("No food found!");
                }
            }
        }
    }
    private void onServerTick(ServerTickedEvent event) {
        if (getLocalPlayer().getTarget().getAnimationId() == 34186) {
            println("Target animation detected, moving on to the next state.");
            botState = BotState.LOOTING;
            // Skipping initializeAbilityRotation and stopping further execution for this tick
            return;
        }

       /* if (firstAnimationEncountered && botState != BotState.LOOTING) {
            initializeAbilityRotation();
        }*/

        tickCounter++;

        if (hasEatenFood) {
            ticksAfterEating += 1;
            if (ticksAfterEating >= 4) {
                hasEatenFood = false;
                ticksAfterEating = 0;
            }
        }

        if (tickCounter >= 3 && !abilityRotation.isEmpty() && botState != BotState.LOOTING) {
            Runnable ability = abilityRotation.poll();
            ability.run();
            abilityRotation.add(ability); // Re-add for continuous rotation
            tickCounter = 0;
        }
    }


    private boolean hasInteractedWithLootAll = false;

    private void LootAll() {
        if (!hasInteractedWithLootAll) {
            ComponentQuery query = ComponentQuery.newQuery(1622);
            List<Component> components = query.componentIndex(22)
                    .results()
                    .stream()
                    .toList();

            if (!components.isEmpty() && components.get(0).interact(1)) {
                hasInteractedWithLootAll = true;
                println("Successfully interacted with LootAll component.");
            }
        }
    }

    private boolean hasInteractedWithStart = false;

    private void Start() {
        if (!hasInteractedWithStart) {
            ComponentQuery query = ComponentQuery.newQuery(1591);
            List<Component> components = query.componentIndex(60)
                    .results()
                    .stream()
                    .toList();

            if (!components.isEmpty() && components.get(0).interact(1)) {
                hasInteractedWithStart = true;
                println("Successfully interacted with Start component.");
            }
        }
    }

    public void useWeaponPoison() {
        Player localPlayer = getLocalPlayer();
        if (localPlayer != null && !localPlayer.isMoving()) {
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
                    Execution.delay(RandomGenerator.nextInt(600, 650));

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


    private long animationStart = -1;

    private void weAreDead() {
        // Check if the local player exists and the game state is LOGGED_IN
        if (Client.getLocalPlayer() != null && Client.getGameState() == Client.GameState.LOGGED_IN) {
            // Check if the player is dead, in combat, has been idle for too long, and is not in Wars Retreat region
            if (Client.getLocalPlayer().getCurrentHealth() == 0 || (!Client.getLocalPlayer().inCombat() && isPlayerIdleTooLong() && !isInWarsRetreatRegion())) {
                handlePlayerDeathOrIdle();
            }
        }
    }

    private boolean isPlayerIdleTooLong() {
        int idleAnimationId = -1;
        int targetStanceId = 2698;
        // Assuming getStanceId() is a method that exists and returns the current stance ID of the local player
        if (Client.getLocalPlayer() != null && Client.getLocalPlayer().getAnimationId() == idleAnimationId && getLocalPlayer().getStanceId() == targetStanceId) {
            if (animationStart == -1) {
                animationStart = System.currentTimeMillis();
            }
            return System.currentTimeMillis() - animationStart > 45000; // Player has been idle for more than 45 seconds
        } else {
            animationStart = -1; // Reset the timer
        }
        return false;
    }

    // Check if player is in Wars Retreat region
    private boolean isInWarsRetreatRegion() {
        if (Client.getLocalPlayer() != null) {
            Coordinate playerCoord = Client.getLocalPlayer().getCoordinate();
            int currentPlayerRegionId = ((playerCoord.getX() >> 6) << 8) + (playerCoord.getY() >> 6);
            int warsRetreatRegionId = ((3294 >> 6) << 8) + (10127 >> 6);
            return currentPlayerRegionId == warsRetreatRegionId;
        }
        return false;
    }

    private void handlePlayerDeathOrIdle() {
        DeactivatePrayers();
        Execution.delay(RandomGenerator.nextInt(5000, 7500));
        println("We're dead or have been idle too long, teleporting to Wars Retreat!");
        botState = BotState.WARSRETREAT;
        animationStart = -1;
    }

    private boolean isDarknessActive() {
        Component darkness = ComponentQuery.newQuery(284).spriteId(30122).results().first();
        return darkness != null;
    }

    private void useDarkness() {
        if (getLocalPlayer() != null) {
            if (!isDarknessActive()) {
                boolean success = ActionBar.useAbility("Darkness");
                ScriptConsole.println("Activated Darkness: " + success);
            }
        }
    }

    public void UseSaraBrew() {
        if (getLocalPlayer() != null) {
            int currentHealth = getLocalPlayer().getCurrentHealth();
            int maximumHealth = getLocalPlayer().getMaximumHealth();

            int healthPercentage = currentHealth * 100 / maximumHealth;

            if (healthPercentage < healthThreshold) {
                ResultSet<Item> items = InventoryItemQuery.newQuery(93).results();

                Item saraBrew = items.stream()
                        .filter(item -> item.getName() != null && item.getName().toLowerCase().contains("saradomin"))
                        .findFirst()
                        .orElse(null);

                if (saraBrew != null) {
                    println("Attempting to drink " + saraBrew.getName());
                    boolean success = Backpack.interact(saraBrew.getName(), "Drink");
                    if (success) {
                        println("Drinking " + saraBrew.getName());
                        Execution.delay(RandomGenerator.nextInt(600, 700));
                    } else {
                        println("Failed to drink " + saraBrew.getName());
                    }
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

                Execution.delayUntil(RandomGenerator.nextInt(600, 650), () -> {
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

    private int minDelay = 320;
    private int maxDelay = 360;

    public int getMinDelay() {
        return minDelay;
    }

    public void setMinDelay(int minDelay) {
        this.minDelay = minDelay;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(int maxDelay) {
        this.maxDelay = maxDelay;
    }

    void saveConfiguration() { //CIPHER PLEASE HELP ME WITH GETTING THE ADJUSTABLE SETTINGS TO SAVE, I DMED YOU BUT NO REPLY
        // Saving boolean settings
        this.configuration.addProperty("UseScriptureOfWen", String.valueOf(this.UseScriptureOfWen));
        this.configuration.addProperty("usePrayer", String.valueOf(this.useprayer));
        this.configuration.addProperty("useOverload", String.valueOf(this.useoverload));
        this.configuration.addProperty("useSorrow", String.valueOf(this.useSorrow));
        this.configuration.addProperty("HaveMobile", String.valueOf(this.HaveMobile));
        this.configuration.addProperty("useInvokeDeath", String.valueOf(this.useInvokeDeath));
        this.configuration.addProperty("useLuckOfTheDwarves", String.valueOf(this.useLuckoftheDwarves));
        this.configuration.addProperty("eatFood", String.valueOf(this.eatfood));
        this.configuration.addProperty("useCauldron", String.valueOf(this.useCauldron));
        this.configuration.addProperty("useVulnBomb", String.valueOf(this.useVulnBomb));
        this.configuration.addProperty("useRuination", String.valueOf(this.useRuination));
        this.configuration.addProperty("useDeflectMagic", String.valueOf(this.useDeflectMagic));
        this.configuration.addProperty("useProtectMagic", String.valueOf(this.useProtectMagic));
        this.configuration.addProperty("useSaraBrew", String.valueOf(this.useSaraBrew));
        this.configuration.addProperty("useSaraBrewandBlubber", String.valueOf(this.useSaraBrewandBlubber));
        this.configuration.addProperty("useWeaponPoison", String.valueOf(this.useWeaponPoison));
        this.configuration.addProperty("useDarkness", String.valueOf(this.useDarkness));
        this.configuration.addProperty("startAtPortal", String.valueOf(this.startAtPortal));
        this.configuration.addProperty("healthThreshold", String.valueOf(this.healthThreshold));
        this.configuration.addProperty("minDelay", String.valueOf(this.minDelay));
        this.configuration.addProperty("maxDelay", String.valueOf(this.maxDelay));
        this.configuration.addProperty("prayerPointsThreshold", String.valueOf(this.prayerPointsThreshold));
        if (ImGui.IsItemClicked(ImGui.MouseButton.LEFT_BUTTON)) {
            saveConfiguration();
        }
    }


    private void loadConfiguration() {
        try {
            this.UseScriptureOfWen = Boolean.parseBoolean(this.configuration.getProperty("UseScriptureOfWen"));
            this.useprayer = Boolean.parseBoolean(this.configuration.getProperty("usePrayer"));
            this.useoverload = Boolean.parseBoolean(this.configuration.getProperty("useOverload"));
            this.useSorrow = Boolean.parseBoolean(this.configuration.getProperty("useSorrow"));
            this.HaveMobile = Boolean.parseBoolean(this.configuration.getProperty("HaveMobile"));
            this.useInvokeDeath = Boolean.parseBoolean(this.configuration.getProperty("useInvokeDeath"));
            this.useLuckoftheDwarves = Boolean.parseBoolean(this.configuration.getProperty("useLuckOfTheDwarves"));
            this.eatfood = Boolean.parseBoolean(this.configuration.getProperty("eatFood"));
            this.useCauldron = Boolean.parseBoolean(this.configuration.getProperty("useCauldron"));
            this.useVulnBomb = Boolean.parseBoolean(this.configuration.getProperty("useVulnBomb"));
            this.useRuination = Boolean.parseBoolean(this.configuration.getProperty("useRuination"));
            this.useDeflectMagic = Boolean.parseBoolean(this.configuration.getProperty("useDeflectMagic"));
            this.useProtectMagic = Boolean.parseBoolean(this.configuration.getProperty("useProtectMagic"));
            this.useSaraBrew = Boolean.parseBoolean(this.configuration.getProperty("useSaraBrew"));
            this.useSaraBrewandBlubber = Boolean.parseBoolean(this.configuration.getProperty("useSaraBrewandBlubber"));
            this.useWeaponPoison = Boolean.parseBoolean(this.configuration.getProperty("useWeaponPoison"));
            this.useDarkness = Boolean.parseBoolean(this.configuration.getProperty("useDarkness"));
            this.startAtPortal = Boolean.parseBoolean(this.configuration.getProperty("startAtPortal"));
            this.setHealthThreshold(Integer.parseInt(this.configuration.getProperty("healthThreshold")));
            this.minDelay = Integer.parseInt(this.configuration.getProperty("minDelay"));
            this.maxDelay = Integer.parseInt(this.configuration.getProperty("maxDelay"));
            this.prayerPointsThreshold = Integer.parseInt(this.configuration.getProperty("prayerPointsThreshold"));


            println("Configuration loaded successfully.");
        } catch (Exception e) {
            println("Failed to load configuration. Using defaults.");
        }
    }
    private void logBoss() {
        if (getLocalPlayer() == null) return;

        Npc boss = NpcQuery.newQuery().name("Kerapac, the bound").results().first();
        if (boss == null) return;

        println("Boss Animation: " + boss.getAnimationId());
        printToTextFile("Boss Animation: " + boss.getAnimationId());
    }

    private void printToTextFile(String s) {
        File file = new File("C:\\Users\\Public\\Documents\\log.txt");
        try {
            FileWriter fileWriter = new FileWriter(file, true);
            fileWriter.write(s + "\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
   /* private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private void checkKerapacAttackable() {
        Npc kerapac = NpcQuery.newQuery().name("Kerapac, the bound").results().first();
        if (kerapac == null) {
            println("Kerapac not found, stopping checks.");
            return;
        }

        if (!kerapac.getOptions().contains("Attack")) {
            println("Kerapac is not attackable yet, checking again in 1 second...");
            scheduler.schedule(this::checkKerapacAttackable, 1, TimeUnit.SECONDS);
        } else {
            println("Kerapac is now attackable, initializing ability rotation.");
            initializeAbilityRotation();
        }
    }

    private void initializeAbilityRotation() {

        addToRotation(() -> useAbility("Touch of Death"));
        addToRotation(() -> useAbility("Threads of Fate"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Soul Sap"));
        addToRotation(() -> useAbility("Touch of Death"));
        addToRotation(() -> useAbility("Soul Sap"));
        //prefight while kerapac is spawning
        addToRotation(() -> useAbility("Conjure Undead Army"));
        addToRotation(() -> useAbility("Life Transfer"));
        addToRotation(() -> useAbility("Command Vengeful Ghost"));
        addToRotation(() -> useAbility("Invoke Death")); //surge before this
        addToRotation(() -> useAbility("Split Soul"));
        addToRotation(() -> useAbility("Command Skeleton Warrior"));
        //phase 1
        addToRotation(() -> useAbility("Ingenuity of the Humans" + "Smoke Cloud"));// equip Praesul Wand before this
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Bloat"));
        addToRotation(() -> useAbility("Death Skulls"));
        addToRotation(() -> useAbility("Volley of Souls"));
        addToRotation(() -> useAbility("Death Essence"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Touch of Death")); // walk under kerapac
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Command Skeleton Warrior"));
        addToRotation(() -> useAbility("Touch of Death"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Bloat"));
        addToRotation(() -> useAbility("Touch of Death"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Death Grasp"));
        addToRotation(() -> useAbility("Soul Sap"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Soul Sap"));
        addToRotation(() -> useAbility("Life Transfer"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        //Phase 2 - Need to time warp before rotation starts
        addToRotation(() -> useAbility("Living Death"));
        addToRotation(() -> useItem("Adrenaline Renewal (4)", "Drink"));
        addToRotation(() -> useAbility("Touch of Death"));
        addToRotation(() -> useAbility("Death Skulls"));
        addToRotation(() -> useAbility("Finger of Death"));
        addToRotation(() -> useAbility("Bloat"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack")); // walk under kerapac before this
        addToRotation(() -> useAbility("Death Skulls"));
        addToRotation(() -> useAbility("Touch of Death"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Death Grasp")); //
        addToRotation(() -> useAbility("Touch of Death"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Death Grasp"));
        addToRotation(() -> useAbility("Basic<nbsp>Attack"));
        addToRotation(() -> useAbility("Death Grasp"));


    }

    private void addToRotation(Runnable ability) {
        abilityRotation.add(ability);
    }


    private void useAbility(String abilityName) {
        if (ActionBar.getCooldownPrecise(abilityName) > 0) {
            return;
        }
        ActionBar.useAbility(abilityName);
        ScriptConsole.println("Used " + abilityName + ": " + ActionBar.useAbility(abilityName), new Object[0]);
        ActionBar.getCooldownPrecise(abilityName);
    }

    private void useItem(String itemName, String action) {
        if (ActionBar.getCooldownPrecise(itemName) > 0) {
            return;
        }

        boolean success = performItemAction(itemName, action);

        if (success) {
            ScriptConsole.println("Performed action " + action + " with " + itemName, new Object[0]);
        } else {
            ScriptConsole.println("Failed to perform action " + action + " with " + itemName, new Object[0]);
        }

    }

    *//**
     * Conceptual method to perform a specific action with an item from the action bar.
     * You'll need to adapt or implement this based on available API functionalities.
     *
     * @param itemName The name of the item.
     * @param action   The action to perform with the item.
     * @return true if the action was successfully performed; false otherwise.
     *//*
    private boolean performItemAction(String itemName, String action) {
        ActionBar.useItem(itemName, action);
        return false;
    }*/

}