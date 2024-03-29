package net.botwithus;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Equipment;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.events.EventBus;
import net.botwithus.rs3.events.impl.InventoryUpdateEvent;
import net.botwithus.rs3.events.impl.ServerTickedEvent;
import net.botwithus.rs3.game.*;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.js5.types.ItemType;
import net.botwithus.rs3.game.js5.types.vars.VarDomainType;
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
import net.botwithus.rs3.input.GameInput;
import net.botwithus.rs3.input.KeyboardInput;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.util.RandomGenerator;
import net.botwithus.rs3.util.Regex;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.botwithus.rs3.game.Client.getLocalPlayer;


public class SkeletonScript extends LoopingScript {

    private BotState botState = BotState.IDLE;
    public boolean runScript;
    boolean useEssenceOfFinality;
    boolean useVolleyofSouls;
    boolean dontuseWarsRetreat;
    boolean useScriptureOfJas;
    boolean overloadEnabled;
    boolean LantadymeIncence;
    boolean KwuarmIncence;
    boolean TorstolIncence;
    boolean UseScriptureOfWen;
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
    private boolean scriptRunning = false;
    private Instant scriptStartTime;

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
        TRANSITION,
        WARSRETREAT,
        RESTART_SCRIPT,
        DEATHS_OFFICE,
    }


    public SkeletonScript(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.sgc = new SkeletonScriptGraphicsContext(this.getConsole(), this);
        loadConfiguration();
        this.loopDelay = 100;
    }

    public void startScript() {
        println("Attempting to start script...");
        if (!scriptRunning) {
            scriptRunning = true;
            scriptStartTime = Instant.now();
            println("Script started at: " + scriptStartTime);
        } else {
            println("Attempted to start script, but it is already running.");
        }
    }

    public void stopScript() {
        if (scriptRunning) {
            scriptRunning = false;
            Instant stopTime = Instant.now();
            println("Script stopped at: " + stopTime);
            long duration = Duration.between(scriptStartTime, stopTime).toMillis();
            println("Script ran for: " + duration + " milliseconds.");
        } else {
            println("Attempted to stop script, but it is not running.");
        }
    }


    private boolean kerapacPortalInitialized = false;

    @Override
    public void onLoop() {
        if (getLocalPlayer() != null && Client.getGameState() == Client.GameState.LOGGED_IN) {

            if (!scriptRunning) {
                return;
            }
        }
        if (!kerapacPortalInitialized) {
            botState = startAtPortal ? BotState.KERAPACPORTAL : BotState.IDLE;
            kerapacPortalInitialized = true;
        }
        if (NpcQuery.newQuery().name("Death").results().first() != null) {
            Execution.delay(RandomGenerator.nextInt(5000, 10000));
            DeathsOffice();
        }

        switch (botState) {
            case IDLE -> {
                IdleDelays();
                DeactivatePrayers();
                if (UseScriptureOfWen) {
                    deactivateScriptureOfWen();
                }
                if (useScriptureOfJas) {
                    deactivateScriptureOfJas();
                }
                botState = BotState.PRAYER;
            }
            case PRAYER -> handleCampfire();

            case KERAPACPORTAL -> {
                InteractWithColloseum();
                {
                    hasInteractedWithLootAll = false;
                    hasInteractedWithStart = false;
                    hasUsedInvokeDeath = false;
                    luckOfTheDwarvesUsed = false;
                    darknessActivated = false;
                    messagePrinted = false;

                }
            }

            case INTERACTWITHDIALOG -> InteractWithDialog();

            case KERAPACPHASE1 -> kerapacPhase1();

            case KERAPACPHASE2 -> {
                monitorKerapacAnimations();
            }


            case LOOTING -> {
                firstAnimationEncountered = false;
                DeactivatePrayers();
                loot();
                ++loopCounter;
            }
            case TRANSITION -> {
                Transition();
            }
            case WARSRETREAT -> {
                useWarsRetreat();
            }
            case RESTART_SCRIPT -> {
                restartScript();
            }
            case DEATHS_OFFICE -> {
                DeathsOffice();
            }
        }
    }

    private boolean isPlayerDead() {
        LocalPlayer player = Client.getLocalPlayer();
        return player != null && player.getCurrentHealth() == 0;
    }

    private void IdleDelays() {
        if (!scriptRunning) {
            return;
        }
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
        if (!scriptRunning) {
            return;
        }
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
        if (!scriptRunning) {
            return;
        }

        EntityResultSet<SceneObject> altarOfWarResults = SceneObjectQuery.newQuery().name("Altar of War").results();

        if (!altarOfWarResults.isEmpty()) {
            SceneObject altar = altarOfWarResults.nearest();
            if (altar != null && altar.interact("Pray")) {
                println("Praying at Altar of War!");
                Execution.delay(RandomGenerator.nextInt(4000, 4500));
                handleCauldron();
            }
        }
    }

    private void handleCauldron() {
        if (!scriptRunning) {
            return;
        }
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
        if (!scriptRunning) {
            return;
        }
        EntityResultSet<SceneObject> BankChest = SceneObjectQuery.newQuery().name("Bank chest").results();
        if (getLocalPlayer() == null)
            return;
        if (!BankChest.isEmpty()) {
            SceneObject bank = BankChest.nearest();
            if (bank != null) {
                bank.interact("Load Last Preset from");
                println("Loading preset!");
                Execution.delay(RandomGenerator.nextInt(2500, 3000));

                boolean healthFull = Execution.delayUntil(15000, () -> getLocalPlayer().getCurrentHealth() == getLocalPlayer().getMaximumHealth());
                if (healthFull) {
                    println("Player health is now full.");
                } else {
                    println("Timed out waiting for player health to be full.");
                }

                handleIncense();
            }
        }
    }

    public void manageFamiliarSummoning() {
        if (!scriptRunning) {
            return;
        }
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

    private void handleIncense() {
        if (!scriptRunning) {
            return;
        }
        if (LantadymeIncence) {
            lantadymeIncenseSticks();
            Execution.delay(RandomGenerator.nextInt(1000, 1500));
        }
        if (KwuarmIncence) {
            kwuarmIncenseSticks();
            Execution.delay(RandomGenerator.nextInt(1000, 1500));
        }
        if (TorstolIncence) {
            torstolIncenseSticks();
            Execution.delay(RandomGenerator.nextInt(1000, 1500));
        }
        manageFamiliarSummoning();
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
        if (!scriptRunning) {
            return;
        }
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
        boolean success = Objects.requireNonNull(ComponentQuery.newQuery(662).componentIndex(78).results().first()).interact(1);
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
        if (!scriptRunning) {
            return;
        }
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
        if (!scriptRunning) {
            return;
        }
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
        if (!scriptRunning) {
            return;
        }
        if (getLocalPlayer() != null) {
            ScriptConsole.println("Used Wars Retreat: " + ActionBar.useAbility("War's Retreat Teleport"), new Object[0]);
            Execution.delay(RandomGenerator.nextInt(4000, 5000));
            botState = BotState.IDLE;
        }
    }

    private void InteractWithColloseum() {
        if (!scriptRunning) {
            return;
        }
        Execution.delay(RandomGenerator.nextInt(2000, 2500));
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
        if (!scriptRunning) {
            return;
        }
        if (Interfaces.isOpen(1591)) {
            Execution.delay(RandomGenerator.nextInt(800, 1000));
            Start();
            println("Interacting with dialog!");
            Execution.delay(RandomGenerator.nextInt(1200, 1700));
            botState = BotState.KERAPACPHASE1;
        }
    }

    public void kerapacPhase1() {
        if (!scriptRunning) {
            return;
        }
        Npc kerapac = NpcQuery.newQuery().name("Kerapac, the bound").results().first();
        if (kerapac == null && getLocalPlayer() == null) {
            return;
        }
        if (UseScriptureOfWen) {
            activateScriptureOfWen();
        }
        if (useScriptureOfJas) {
            activateScriptureOfJas();
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
                Execution.delay(RandomGenerator.nextInt(1850, 1900));
                secondSurgeSuccessful = true;
            }
        } else {
            ScriptConsole.println("Surge is on cooldown.");
        }


        Coordinate expectedSecondSurgeCoord = kerapacPhase1StartCoord.derive(-20, 0, 0);


        Coordinate currentPlayerCoord = Client.getLocalPlayer().getCoordinate();

        if (firstSurgeSuccessful && secondSurgeSuccessful && currentPlayerCoord.equals(expectedSecondSurgeCoord)) {
            conjure();
            Execution.delay(RandomGenerator.nextInt(100, 200));
        } else if (!currentPlayerCoord.equals(expectedSecondSurgeCoord)) {
            Coordinate adjustCoord = kerapacPhase1StartCoord.derive(-20, 0, 0);
            Movement.walkTo(adjustCoord.getX(), adjustCoord.getY(), true);
            Execution.delay(RandomGenerator.nextInt(1000, 1500));
            conjure();
            Execution.delay(RandomGenerator.nextInt(100, 200));
        }


        botState = BotState.KERAPACPHASE2;
    }

    private void conjure() {
        if (!scriptRunning) {
            return;
        }
        if (ActionBar.containsAbility("Conjure Undead Army")) {
            ScriptConsole.println("Used Conjure Undead Army: " + ActionBar.useAbility("Conjure Undead Army"));
        } else {
            ScriptConsole.println("Used Conjure Vengeful Ghost: " + ActionBar.useAbility("Conjure Vengeful Ghost"));
            Execution.delay(net.botwithus.rs3.util.RandomGenerator.nextInt(1850, 1900));
            ScriptConsole.println("Used Conjure Skeleton Warrior: " + ActionBar.useAbility("Conjure Skeleton Warrior"));
        }
    }
    private boolean kerapacAnimationActive = false;
    private boolean animation34193Started = false;

    private void monitorKerapacAnimations() {
        if (!scriptRunning) {
            return;
        }
        Npc kerapac = NpcQuery.newQuery().name("Kerapac, the bound").results().first();

        if (kerapac == null || isPlayerDead() || getLocalPlayer() == null) {
            botState = BotState.DEATHS_OFFICE;
            return;
        }
        int animationID = kerapac.getAnimationId();

        if (animationID == 34193) {
            animation34193Started = true;
        }

        kerapacAnimationActive = animationID == 34194 || animationID == 34198 || animationID == 34195 || (animation34193Started && animationID == 34193);

        if (animation34193Started && animationID == 34194) {
            animation34193Started = false;
            kerapacAnimationActive = false;
            println("Handled Kerapac animation sequence. Ready to proceed with actions.");
        }

        if (kerapacAnimationActive) {
    } else {
            animation34193Started = false;
            kerapacAnimationActive = false;
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
        if (useDarkness) {
            useDarkness();
        }
        if (useoverload) {
            drinkOverloads();
        }
        if (useWeaponPoison) {
            useWeaponPoison();
        }
        if (useprayer) {
            usePrayerOrRestorePots();
        }
        activatePrayers();
        TeleportToWarOnHealth();
        DeathsOffice();


        int NecrosisStacks = VarManager.getVarValue(VarDomainType.PLAYER, 10986);
        int RisidualSouls = VarManager.getVarValue(VarDomainType.PLAYER, 11035);

        if (useVolleyofSouls) {
            if (!kerapacAnimationActive && !animation34193Started && kerapac.getAnimationId() != 34198 && kerapac.getAnimationId() != 34199 && kerapac.getAnimationId() != 34194 && kerapac.getAnimationId() != 34202
                    && RisidualSouls == 5) {
                println("Risidual Souls: " + RisidualSouls);
                ScriptConsole.println("Used Volley of Souls: " + ActionBar.useAbility("Volley of Souls"));
                Execution.delay(RandomGenerator.nextInt(1780, 1820));
            }
        }

        if (useEssenceOfFinality) {
            if (!kerapacAnimationActive && !animation34193Started && kerapac.getAnimationId() != 34198 && kerapac.getAnimationId() != 34199 && kerapac.getAnimationId() != 34194 && kerapac.getAnimationId() != 34202
                    && ActionBar.getCooldownPrecise("Essence of Finality") == 0 && getLocalPlayer().getAdrenaline() >= 300
                    & ComponentQuery.newQuery(291).spriteId(55524).results().isEmpty() && NecrosisStacks >= 12) {
                if (ActionBar.getCooldown("Death Skulls") >= 5) {
                    println("Necrosis stacks: " + NecrosisStacks);
                    ScriptConsole.println("Used Death Grasp: " + ActionBar.useAbility("Essence of Finality"));
                    Execution.delay(RandomGenerator.nextInt(1780, 1820));
                }
            }
        }

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
                if (VarManager.getVarbitValue(53247) == 0) {
                    if (ActionBar.getCooldownPrecise("Invoke Death") == 0) {
                        ScriptConsole.println("Used Invoke Death: " + ActionBar.useAbility("Invoke Death"), new Object[0]);
                        hasUsedInvokeDeath = true;

                    }
                }
            }

            equipLuckOfTheDwarves();
        }
    }


    private void equipLuckOfTheDwarves() {
        ComponentQuery phaseQuery = ComponentQuery.newQuery(1181).componentIndex(21);
        ResultSet<Component> phaseResults = phaseQuery.results();
        Component phaseComponent = phaseResults.first();
        if (useLuckoftheDwarves && !luckOfTheDwarvesUsed && phaseComponent != null && "Phase: 4".equals(phaseComponent.getText())) {


            ResultSet<Item> luckResults = InventoryItemQuery.newQuery(93).name("Luck of the Dwarves").results();
            if (!luckResults.isEmpty()) {
                Item luckOfTheDwarves = luckResults.first();
                if (luckOfTheDwarves != null && luckOfTheDwarves.getStackSize() > 0) {
                    boolean success = Backpack.interact(luckOfTheDwarves.getName(), "Wear");
                    if (success) {
                        println("Equipped: " + luckOfTheDwarves.getName());
                        luckOfTheDwarvesUsed = true;
                        switchedBack = false;
                    }
                }
            } else {
                println("No 'Luck of the Dwarves' found!");
            }
        }
    }

    private boolean switchedBack = false;

    private void switchBackToPreviousRing() {
        if (luckOfTheDwarvesUsed && !switchedBack) {
            ResultSet<Item> rings = InventoryItemQuery.newQuery(93)
                    .name("Reaver's ring", "Ring of death")
                    .results();

            Item ringToEquip = rings.stream()
                    .filter(ring -> Objects.equals(ring.getName(), "Reaver's ring") || Objects.equals(ring.getName(), "Ring of death"))
                    .findFirst()
                    .orElse(null);

            if (ringToEquip != null) {
                boolean success = Backpack.interact(ringToEquip.getName(), "Wear");
                if (success) {
                    println("Equipped: " + ringToEquip.getName());
                } else {
                    println("Failed to equip: " + ringToEquip.getName());
                }
                switchedBack = true;
            } else {
                if (!switchedBack) {
                    println("No 'Reaver's ring' or 'Ring of death' found in the backpack.");
                    switchedBack = true;
                }
            }
        }
    }


    private boolean shouldSurge = false;
    private boolean surged = false;
    private boolean hasUsedAnticipation = false;
    private boolean surgeMessagePrinted = false;
    private boolean firstAnimationEncountered = false;
    private int lastAnimationId = -1;

    private void handleAnimation(Npc npc, int animationId) {
        if (!scriptRunning) {
            return;
        }
        Npc kerapac = NpcQuery.newQuery().name("Kerapac, the bound").results().first();
        if (kerapac != null && getLocalPlayer() != null) {
            TeleportToWarOnHealth();
            switch (animationId) {
                case 34193:
                    lastAnimationId = 34193;
                    break;
                case 34194:
                    if (lastAnimationId == 34193 && kerapac.getCurrentHealth() <= 180000) {
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
                    }
                    lastAnimationId = -1;
                    break;
                case 34195:
                    Coordinate fiveTilesAway = getLocalPlayer().getCoordinate().derive(+7, 0, 0);

                    Movement.walkTo(fiveTilesAway.getX(), fiveTilesAway.getY(), true);
                {
                    println("`check the delays in the gui if you see this message`");
                    Execution.delayUntil(10000, () -> fiveTilesAway.equals(getLocalPlayer().getCoordinate()));
                    if (kerapac.getOptions().contains("Attack")) {
                        kerapac.interact("Attack");
                        println("Attacking Kerapac after walking away.");
                    }
                }
                break;
                case 34198:
                    if (kerapac.getCurrentHealth() >= 65000) {
                        Coordinate kerapacLocation = Objects.requireNonNull(kerapac.getCoordinate());

                        Movement.walkTo(kerapacLocation.getX(), kerapacLocation.getY(), true);
                        {
                            println("Moving towards Kerapac...");
                            Execution.delayUntil(10000, () -> !getLocalPlayer().isMoving());

                            if (ActionBar.getCooldown("Anticipation") == 0 && !hasUsedAnticipation) {
                                ScriptConsole.println("Used Anticipation: " + ActionBar.useAbility("Anticipation"));
                                hasUsedAnticipation = true;
                            }
                            Execution.delay(RandomGenerator.nextInt(4200, 5000));


                            if (kerapac.getOptions().contains("Attack")) {
                                hasUsedAnticipation = false;
                                kerapac.interact("Attack");
                                println("Attacking Kerapac after reaching his location.");
                            }
                        }
                    }
                    break;
                case 34186:
                    botState = BotState.LOOTING;
                    break;
            }
        }
    }

    private void loot() {
        if (!scriptRunning) {
            return;
        }
        if (getLocalPlayer() != null) {

            EntityResultSet<GroundItem> groundItems = GroundItemQuery.newQuery().results();
            if (!groundItems.isEmpty()) {
                GroundItem groundItem = groundItems.random();
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
                        println("Taking " + groundItem.getName() + "...");
                        Execution.delay(RandomGenerator.nextInt(600, 700));
                    }

                    boolean interfaceOpened = Execution.delayUntil(15000, () -> Interfaces.isOpen(1622));
                    if (!interfaceOpened) {
                        println("Interface 1622 did not open. Attempting to interact with ground item again.");
                        if (groundItem.interact("Take")) {
                            println("Attempting to take " + groundItem.getName() + " again...");
                            Execution.delay(RandomGenerator.nextInt(250, 300));
                        }
                    }
                    LootAll();
                    updateAndDisplayCumulativeLootValue();
                }
                Execution.delay(RandomGenerator.nextInt(1000, 1500));
                if (dontuseWarsRetreat) {
                    switchBackToPreviousRing();
                    botState = BotState.TRANSITION;
                } else {
                    botState = BotState.WARSRETREAT;
                }
            }
        }
    }

    private void Transition() {
        if (!scriptRunning) {
            return;
        }
        if (Client.getLocalPlayer() != null) {

            Pattern itemPattern = Pattern.compile("prayer|restore", Pattern.CASE_INSENSITIVE);
            int familiarTimeRemaining = VarManager.getVarbitValue(6055);

            long prayerOrRestoreItemCount = InventoryItemQuery.newQuery(93)
                    .name(itemPattern)
                    .results()
                    .stream()
                    .count();

            boolean containsTwoOrMorePrayerOrRestoreItems = prayerOrRestoreItemCount >= 2;

            long foodItemCount = InventoryItemQuery.newQuery(93)
                    .option("Eat")
                    .results()
                    .stream()
                    .count();

            boolean containsAtLeastThreeFoodItems = foodItemCount >= 3;
            boolean hasMoreThanTwoEmptySlots = Backpack.countFreeSlots() > 2; // Note: Changed to strictly more than 2

            ScriptConsole.println("Backpack has more than 2 empty slots: " + hasMoreThanTwoEmptySlots);
            ScriptConsole.println("Contains 2 or more Prayer/Restore Potions: " + containsTwoOrMorePrayerOrRestoreItems);
            ScriptConsole.println("Contains at least 3 food items: " + containsAtLeastThreeFoodItems);
            ScriptConsole.println("Familiar time remaining: " + familiarTimeRemaining + " Minutes");

            if (hasMoreThanTwoEmptySlots && containsTwoOrMorePrayerOrRestoreItems && containsAtLeastThreeFoodItems) {
                Objects.requireNonNull(SceneObjectQuery.newQuery().name("Gate").results().nearest()).interact("Exit");
                Execution.delayUntil(5000, () -> Interfaces.isOpen(1188));
                if (Interfaces.isOpen(1188)) {
                    Execution.delay(RandomGenerator.nextInt(300, 500));
                    MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77856776);
                    Execution.delay(RandomGenerator.nextInt(9000, 10000));
                    botState = BotState.KERAPACPORTAL;
                }
            } else {
                ScriptConsole.println("Criteria not met for transition to KERAPACPORTAL. Transitioning to WARSRETREAT.");
                botState = BotState.WARSRETREAT;
            }
        }
    }

    private void TeleportToWarOnHealth() {
        if (!scriptRunning) {
            return;
        }
        if (eatfood || useSaraBrew || useSaraBrewandBlubber) {
            final int warsRetreatRegionId = 13214; // Assuming 13214 is the region ID for War's Retreat
            LocalPlayer player = Client.getLocalPlayer();
            if (player != null) {
                double healthPercentage = (double) player.getCurrentHealth() / player.getMaximumHealth() * 100;
                if (healthPercentage < healthThreshold) {
                    ResultSet<Item> items = InventoryItemQuery.newQuery().results();

                    Pattern healingItemPattern = Pattern.compile("saradomin", Pattern.CASE_INSENSITIVE);

                    boolean hasHealingItem = items.stream().anyMatch(item -> {
                        if (item.getName() != null && healingItemPattern.matcher(item.getName()).find()) {
                            return true;
                        }
                        ItemType itemType = item.getConfigType();
                        if (itemType != null) {
                            return itemType.getBackpackOptions().contains("Eat");
                        }
                        return false;
                    });

                    if (!hasHealingItem) {
                        println("No food or Saradomin potions found in backpack. Attempting to teleport to War's Retreat due to low health.");
                        ActionBar.useAbility("War's Retreat Teleport");

                        // Wait a bit for the teleport action to start/finish
                        Execution.delay(5000); // Adjust delay as necessary for the teleport

                        // Re-fetch the player's position after the delay
                        player = Client.getLocalPlayer();
                        if (player != null && player.getCoordinate().getRegionId() == warsRetreatRegionId) {
                            println("Successfully teleported to War's Retreat.");
                            botState = BotState.IDLE;
                        } else {
                            println("Teleport attempt failed or player is not in War's Retreat.");
                            // Handle failure or wrong destination here
                        }
                    }
                }
            }
        }
    }

    int cumulativeLootValue = 0;

    private void updateAndDisplayCumulativeLootValue() {
        if (Interfaces.isOpen(1622)) {
            Component valueScan = ComponentQuery.newQuery(1622).componentIndex(3).results().last();
            if (valueScan != null) {
                String detectedString = valueScan.getText();
                String numberWithSuffix = extractNumberWithSuffix(detectedString);
                if (!"Error".equals(numberWithSuffix)) {
                    try {
                        int valueToAdd = parseValueWithSuffix(numberWithSuffix);
                        cumulativeLootValue += valueToAdd;
                        println("Cumulative Loot Value: " + cumulativeLootValue + "K");
                    } catch (NumberFormatException e) {
                        println("Number format error: " + e.getMessage());
                    }
                }
            } else {
                println("Component not found");
            }
        }
    }

    public String extractNumberWithSuffix(String source) {
        String regex = "(?i)([\\d,]+)(k|M)?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);

        if (matcher.find()) {
            return matcher.group();
        }
        return "Error";
    }

    private int parseValueWithSuffix(String numberWithSuffix) {
        String cleanNumber = numberWithSuffix.replace(",", "").toLowerCase();
        try {
            if (cleanNumber.endsWith("k")) {
                return Integer.parseInt(cleanNumber.substring(0, cleanNumber.length() - 1));
            } else if (cleanNumber.endsWith("m")) {
                return Integer.parseInt(cleanNumber.substring(0, cleanNumber.length() - 1)) * 1000;
            } else {
                return Integer.parseInt(cleanNumber);
            }
        } catch (NumberFormatException e) {
            println("Error parsing '" + numberWithSuffix + "': " + e.getMessage());
            return -1;
        }
    }


    private void DeactivatePrayers() {
        if (!scriptRunning) {
            return;
        }
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


    private void activateScriptureOfWen() {
        if (VarManager.getVarbitValue(30605) == 0 && VarManager.getVarbitValue(30604) >= 60) {
            println("Activating Scripture of Wen.");
            Equipment.interact(Equipment.Slot.POCKET, "Activate/Deactivate");
        } else {
            println("Scripture of Wen already active or not enough Time Remaining.");
        }
    }

    private void deactivateScriptureOfWen() {
        if (VarManager.getVarbitValue(30605) == 1) {
            println("Deactivating Scripture of Wen.");
            Equipment.interact(Equipment.Slot.POCKET, "Activate/Deactivate");
        }
    }


    public void setPrayerPointsThreshold(int threshold) {
        this.prayerPointsThreshold = threshold;
    }

    public void setHealthThreshold(int healthThreshold) {
        this.healthThreshold = healthThreshold;
    }


    public class RegexUtil {
        public static Pattern getPatternForPrayerOrRestore() {
            return Pattern.compile(".*prayer.*|.*restore.*", Pattern.CASE_INSENSITIVE);
        }
    }

    private static long lastPrayerOrRestoreUse = 0; // Static variable to track the last use time

    public void usePrayerOrRestorePots() {
        long currentTime = System.currentTimeMillis();

        if (getLocalPlayer() != null && !kerapacAnimationActive && !animation34193Started && (currentTime - lastPrayerOrRestoreUse) > 2000) { // Check if 2 seconds have passed
            int currentPrayerPoints = getLocalPlayer().getPrayerPoints();
            if (currentPrayerPoints < prayerPointsThreshold) {
                ResultSet<Item> items = InventoryItemQuery.newQuery().results();
                Pattern pattern = RegexUtil.getPatternForPrayerOrRestore();

                Item prayerOrRestorePot = items.stream()
                        .filter(item -> item.getName() != null && pattern.matcher(item.getName()).find())
                        .findFirst()
                        .orElse(null);

                if (prayerOrRestorePot != null) {
                    println("Attempting to drink " + prayerOrRestorePot.getName());
                    boolean success = Backpack.interact(prayerOrRestorePot.getName(), "Drink");

                    if (success) {
                        println("Drinking " + prayerOrRestorePot.getName());
                        lastPrayerOrRestoreUse = currentTime; // Update the last use time on success
                    } else {
                        println("Failed to use " + prayerOrRestorePot.getName());
                    }
                } else {
                    println("No Prayer or Restore pots found.");
                }
            }
        }
    }

    Pattern overloads = Pattern.compile(Regex.getPatternForContainsString("overload").pattern(), Pattern.CASE_INSENSITIVE);

    private static long lastOverloadUse = 0; // Static variable to track the last use time

    public void drinkOverloads() {
        long currentTime = System.currentTimeMillis();

        if (getLocalPlayer() != null && VarManager.getVarbitValue(26037) == 0 && !kerapacAnimationActive && !animation34193Started && (currentTime - lastOverloadUse) > 2000) { // Check if 2 seconds have passed
            ResultSet<Item> items = InventoryItemQuery.newQuery().results();

            Item overloadPot = items.stream()
                    .filter(item -> item.getName() != null && overloads.matcher(item.getName()).find())
                    .findFirst()
                    .orElse(null);

            if (overloadPot != null) {
                println("Attempting to drink " + overloadPot.getName());
                boolean success = Backpack.interact(overloadPot.getName(), "Drink");

                if (success) {
                    println("Drinking " + overloadPot.getName());
                    lastOverloadUse = currentTime; // Update the last use time on success
                } else {
                    println("Failed to use " + overloadPot.getName());
                }
            } else {
                println("No Overload pots found.");
            }
        }
    }
    private static long lastFoodEatTime = 0; // Static variable to track the last time food was eaten

    public void eatFood() {
        long currentTime = System.currentTimeMillis();

        if (!kerapacAnimationActive && !animation34193Started) {
            if (getLocalPlayer() != null) {
                if (getLocalPlayer().getAnimationId() == 18001)
                    return;

                int currentHealth = getLocalPlayer().getCurrentHealth();
                int maximumHealth = getLocalPlayer().getMaximumHealth();

                int healthPercentage = currentHealth * 100 / maximumHealth;
                if (healthPercentage < healthThreshold && (currentTime - lastFoodEatTime) > 2000) { // Ensure the cooldown has elapsed
                    ResultSet<Item> foodItems = InventoryItemQuery.newQuery(93).option("Eat").results();

                    if (!foodItems.isEmpty()) {
                        Item food = foodItems.first();
                        if (food != null) {
                            boolean success = Backpack.interact(food.getName(), 1);
                            if (success) {
                                println("Eating " + food.getName());
                                lastFoodEatTime = currentTime; // Update the last eat time on success
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
    }

    private boolean hasInteractedWithLootAll = false;

    private void LootAll() {
        if (!hasInteractedWithLootAll) {
            Execution.delay(RandomGenerator.nextInt(1500, 2000));

            ComponentQuery lootAllQuery = ComponentQuery.newQuery(1622);
            List<Component> components = lootAllQuery.componentIndex(22).results().stream().toList();

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
                Execution.delay(RandomGenerator.nextInt(800, 1000));
                hasInteractedWithStart = true;
                println("Successfully interacted with Start component.");
            }
        }
    }
    private static long lastWeaponPoisonUse = 0; // Static variable to track the last use time

    public void useWeaponPoison() {
        long currentTime = System.currentTimeMillis();
        Player localPlayer = getLocalPlayer();

        if (localPlayer != null && !kerapacAnimationActive && !animation34193Started) {
            if (VarManager.getVarbitValue(2102) <= 3 && localPlayer.getAnimationId() != 18068 && (currentTime - lastWeaponPoisonUse) > 2000) { // Adding check for 2 seconds cooldown
                ResultSet<Item> items = InventoryItemQuery.newQuery().results();
                Pattern poisonPattern = Pattern.compile("weapon poison\\+*?", Pattern.CASE_INSENSITIVE);

                Item weaponPoisonItem = items.stream()
                        .filter(item -> {
                            if (item.getName() == null) return false;
                            Matcher matcher = poisonPattern.matcher(item.getName());
                            return matcher.find();
                        })
                        .findFirst()
                        .orElse(null);

                if (weaponPoisonItem != null) {
                    println("Applying " + weaponPoisonItem.getName() + " ID: " + weaponPoisonItem.getId());
                    Backpack.interact(weaponPoisonItem.getName(), "Apply");
                    println(weaponPoisonItem.getName() + "Has been applied");
                }
            }
        }
    }

    private void kwuarmIncenseSticks() {
        ResultSet<Item> backpackResults = InventoryItemQuery.newQuery(93)
                .name("Kwuarm incense sticks")
                .results();
        println("Backpack contains Kwuarm incense sticks: " + !backpackResults.isEmpty());

        if (!backpackResults.isEmpty()) {
            ResultSet<Component> componentResults = ComponentQuery.newQuery(284)
                    .spriteId(47709)
                    .results();
            println("Buff needs to be activated: " + componentResults.isEmpty());

            if (componentResults.isEmpty()) {
                Item kwuarm = backpackResults.first();
                if (kwuarm != null) {
                    String option;
                    if (overloadEnabled) {
                        if (kwuarm.getStackSize() > 6) {
                            option = "Overload";
                        } else {

                            println("Overload option selected but only " + kwuarm.getStackSize() + " sticks available. 6 required.");
                            option = "Light";
                        }
                    } else {
                        option = "Light";
                    }

                    if (Backpack.interact(kwuarm.getName(), option)) {
                        println("Interaction successful with Kwuarm incense sticks using option: " + option);
                    } else {
                        println("Failed to interact with Kwuarm incense sticks using option: " + option);
                    }
                }
            }
        } else {
            println("No Kwuarm incense sticks found or buff is already active.");
        }
    }

    private void lantadymeIncenseSticks() {
        ResultSet<Item> backpackResults = InventoryItemQuery.newQuery(93)
                .name("Lantadyme incense sticks")
                .results();
        println("Backpack contains Lantadyme incense sticks: " + !backpackResults.isEmpty());

        if (!backpackResults.isEmpty()) {
            ResultSet<Component> componentResults = ComponentQuery.newQuery(284)
                    .spriteId(47713)
                    .results();
            println("Buff needs to be activated: " + componentResults.isEmpty());

            if (componentResults.isEmpty()) {
                Item lantadyme = backpackResults.first();
                if (lantadyme != null) {
                    String option;
                    if (overloadEnabled) {
                        if (lantadyme.getStackSize() > 6) {
                            option = "Overload";
                        } else {

                            println("Overload option selected but only " + lantadyme.getStackSize() + " sticks available. 6 required.");
                            option = "Light";
                        }
                    } else {
                        option = "Light";
                    }

                    if (Backpack.interact(lantadyme.getName(), option)) {
                        println("Interaction successful with Lantadyme incense sticks using option: " + option);
                    } else {
                        println("Failed to interact with Lantadyme incense sticks using option: " + option);
                    }
                }
            }
        } else {
            println("No Lantadyme incense sticks found or buff is already active.");
        }
    }

    private void torstolIncenseSticks() {
        ResultSet<Item> backpackResults = InventoryItemQuery.newQuery(93)
                .name("Torstol incense sticks")
                .results();
        println("Backpack contains Torstol incense sticks: " + !backpackResults.isEmpty());

        if (!backpackResults.isEmpty()) {
            ResultSet<Component> componentResults = ComponentQuery.newQuery(284)
                    .spriteId(47715)
                    .results();
            println("Buff needs to be activated: " + componentResults.isEmpty());

            if (componentResults.isEmpty()) {
                Item torstol = backpackResults.first();
                if (torstol != null) {
                    String option;
                    if (overloadEnabled) {
                        if (torstol.getStackSize() > 6) {
                            option = "Overload";
                        } else {
                            option = "Light";
                            println("Overload option selected but only " + torstol.getStackSize() + " sticks available. 6 required.");
                        }
                    } else {
                        option = "Light";
                    }
                    if (Backpack.interact(torstol.getName(), option)) {
                        println("Interaction successful with Torstol incense sticks using option: " + option);
                    } else {
                        println("Failed to interact with Torstol incense sticks using option: " + option);
                    }
                }
            }
        } else {
            println("No Torstol incense sticks found or buff is already active.");
        }
    }

    private boolean darknessActivated = false;
    private boolean messagePrinted = false;

    private boolean isDarknessActive() {
        Component darkness = ComponentQuery.newQuery(284).spriteId(30122).results().first();
        return darkness != null || darknessActivated;
    }

    private void useDarkness() {
        if (getLocalPlayer() != null && !kerapacAnimationActive && !animation34193Started) {
            if (!isDarknessActive()) {
                boolean success = ActionBar.useAbility("Darkness");
                ScriptConsole.println("Activated Darkness: " + success);
                if (success) {
                    darknessActivated = true;
                }
                messagePrinted = false;
            } else {
                if (!messagePrinted && darknessActivated) {
                    messagePrinted = true; // Set the flag after printing the message
                }
            }
        }
    }

    public void UseSaraBrew() {
        if (!kerapacAnimationActive && !animation34193Started) {
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
                        } else {
                            println("Failed to drink " + saraBrew.getName());
                        }
                    } else {
                        println("No Saradomin brews found!");
                    }
                }
            }
        }
    }

    private void UseSaraandBlubber() {
        if (!kerapacAnimationActive && !animation34193Started) {
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
                }
            }
        }
    }

    public void destroyKeyIfDetected() { // did chatgpt convert this correct Cipher? also i dont have comp.interact come up on my thing
        Component thKey = ComponentQuery.newQuery(1473).itemName("Key token").results().first();
        if (thKey != null) {
            Execution.delay(RandomGenerator.nextInt(1500, 2000));
            if (Interfaces.isOpen(1183)) {
                Execution.delay(RandomGenerator.nextInt(1500, 2000));
                println("Destroy key confirmed: " + MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 77529093));
            } else {
                println("Destroy key from inventory: " + thKey.interact("Destroy"));
            }
        } else {
            println("No TH key found to destroy.");
        }
    }

    private void activateScriptureOfJas() {
        if (VarManager.getVarbitValue(30605) == 0 && VarManager.getVarbitValue(30604) >= 60) {
            println("Activating Scripture of Jas.");
            Equipment.interact(Equipment.Slot.POCKET, "Activate/Deactivate");
        } else {
            println("Scripture of Jas already active or not enough Time Remaining.");
        }
    }


    private void deactivateScriptureOfJas() {
        if (VarManager.getVarbitValue(30605) == 1) {
            println("Deactivating Scripture of Jas.");
            Equipment.interact(Equipment.Slot.POCKET, "Activate/Deactivate");
        }
    }

    public void DeathsOffice() {
        Npc death = NpcQuery.newQuery().name("Death").results().nearest();
        if (getLocalPlayer() != null) {
            if (death != null) {
                Execution.delay(RandomGenerator.nextInt(8000, 10000));
                interactWithDeath();
            }
        }
    }

    private void interactWithDeath() {
        Npc death = NpcQuery.newQuery().name("Death").results().nearest();

        if (death.interact("Reclaim items")) {
            println("Interaction initiated. Waiting for interface 1626 to open.");
            if (Execution.delayUntil(5000, () -> Interfaces.isOpen(1626))) {
                println("Successfully opened interface 1626. Moving to reclaim confirmation.");
                Execution.delay(RandomGenerator.nextInt(3500, 5000));
                confirmReclaim(); // Proceed to confirm reclaim if successful.
            } else {
                println("Failed to open interface 1626 after interacting with Death.");
            }
        } else {
            println("Failed to initiate interaction with Death.");
        }
    }

    private void confirmReclaim() {
        if (!Interfaces.isOpen(1626)) {
            println("Interface 1626 is not open. Cannot confirm reclaim.");
            return;
        }

        ComponentQuery query = ComponentQuery.newQuery(1626);
        List<Component> components = query.componentIndex(47).results().stream().toList();
        if (!components.isEmpty() && components.get(0).interact(1)) {
            println("Reclaim confirmation initiated. Waiting for finalization option.");
            Execution.delay(RandomGenerator.nextInt(3500, 5000));
            finalizeReclamation(); // Proceed to finalize reclamation if successful.
        } else {
            println("Failed to confirm reclaim with Death.");
        }
    }

    private void finalizeReclamation() {
        if (!Interfaces.isOpen(1626)) {
            println("Interface 1626 is not open. Cannot finalize reclaim.");
            return;
        }

        ComponentQuery query = ComponentQuery.newQuery(1626);
        List<Component> components = query.componentIndex(72).results().stream().toList();
        if (!components.isEmpty() && components.get(0).interact(1)) {
            println("Reclaim finalized. Moving to post-reclaim actions.");
            Execution.delay(RandomGenerator.nextInt(3500, 5000));
            botState = BotState.WARSRETREAT; // Update bot state to continue with the script flow.
        } else {
            println("Failed to finalize reclaim with Death.");
        }
    }


    private int minDelay = 550;
    private int maxDelay = 600;

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

    void saveConfiguration() {
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
        this.configuration.addProperty("dontuseWarsRetreat", String.valueOf(this.dontuseWarsRetreat));
        this.configuration.addProperty("useScriptureOfJas", String.valueOf(this.useScriptureOfJas));
        this.configuration.addProperty("KwuarmIncence", String.valueOf(this.KwuarmIncence));
        this.configuration.addProperty("LantadymeIncence", String.valueOf(this.LantadymeIncence));
        this.configuration.addProperty("TorstolIncence", String.valueOf(this.TorstolIncence));
        this.configuration.addProperty("useEssenceOfFinality", String.valueOf(this.useEssenceOfFinality));
        this.configuration.addProperty("useVolleyofSouls", String.valueOf(this.useVolleyofSouls));

        this.configuration.save();
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
            this.dontuseWarsRetreat = Boolean.parseBoolean(this.configuration.getProperty("dontuseWarsRetreat"));
            this.useScriptureOfJas = Boolean.parseBoolean(this.configuration.getProperty("useScriptureOfJas"));
            this.KwuarmIncence = Boolean.parseBoolean(this.configuration.getProperty("KwuarmIncence"));
            this.LantadymeIncence = Boolean.parseBoolean(this.configuration.getProperty("LantadymeIncence"));
            this.TorstolIncence = Boolean.parseBoolean(this.configuration.getProperty("TorstolIncence"));
            this.useEssenceOfFinality = Boolean.parseBoolean(this.configuration.getProperty("useEssenceOfFinality"));
            this.useVolleyofSouls = Boolean.parseBoolean(this.configuration.getProperty("useVolleyofSouls"));


            println("Configuration loaded successfully.");
        } catch (Exception e) {
            println("Failed to load configuration. Using defaults.");
        }
    }
}