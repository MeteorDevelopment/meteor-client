package minegame159.meteorclient.modules.player
import baritone.api.BaritoneAPI
import meteordevelopment.orbit.EventHandler
import minegame159.meteorclient.events.entity.player.ItemUseCrosshairTargetEvent
import minegame159.meteorclient.events.world.TickEvent
import minegame159.meteorclient.modules.Categories
import minegame159.meteorclient.modules.Module
import minegame159.meteorclient.modules.Modules
import minegame159.meteorclient.modules.combat.AnchorAura
import minegame159.meteorclient.modules.combat.BedAura
import minegame159.meteorclient.modules.combat.CrystalAura
import minegame159.meteorclient.modules.combat.KillAura
import minegame159.meteorclient.settings.*
import minegame159.meteorclient.utils.Utils
import minegame159.meteorclient.utils.player.ChatUtils
import minegame159.meteorclient.utils.player.Rotations
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.potion.PotionUtil
import net.minecraft.util.Hand
import java.util.ArrayList
class AutoPot:Module(Categories.Player, "auto-pot", "Automatically Drinks Potions") {
  private val sgGeneral = settings.getDefaultGroup()
  private val Healing = sgGeneral.add(BoolSetting.Builder()
                                      .name("Healing")
                                      .description("Enables healing potions.")
                                      .defaultValue(true)
                                      .build()
                                     )
  private val Strength = sgGeneral.add(BoolSetting.Builder()
                                       .name("Strength")
                                       .description("Enables strength potions.")
                                       .defaultValue(true)
                                       .build()
                                      )
  private val useSplashPots = sgGeneral.add(BoolSetting.Builder()
                                            .name("Splash-Pots")
                                            .description("Allow the use of splash pots")
                                            .defaultValue(true)
                                            .build()
                                           )
  private val health = sgGeneral.add(IntSetting.Builder()
                                     .name("health")
                                     .description("If health goes below this point, Healing Pot will trigger.")
                                     .defaultValue(15)
                                     .min(0)
                                     .sliderMax(20)
                                     .build()
                                    )
  private val pauseAuras = sgGeneral.add(BoolSetting.Builder()
                                         .name("pause-auras")
                                         .description("Pauses all auras when eating.")
                                         .defaultValue(true)
                                         .build()
                                        )
  private val pauseBaritone = sgGeneral.add(BoolSetting.Builder()
                                            .name("pause-baritone")
                                            .description("Pause baritone when eating.")
                                            .defaultValue(true)
                                            .build()
                                           )
  private val lookDown = sgGeneral.add(BoolSetting.Builder()
                                       .name("rotate")
                                       .description("Forces you to rotate downwards when throwing bottles.")
                                       .defaultValue(true)
                                       .build()
                                      )
  private val slot:Int = 0
  private val prevSlot:Int = 0
  private val drinking:Boolean = false
  private val splashing:Boolean = false
  private val wasAura = ArrayList<Class<out Module>>()
  private val wasBaritone:Boolean = false
  //Gilded's first module, lets see how much i'll die making this
  //TODO:Rework everything to accept all pots
  //TODO: Does strength work better if you throw it up? will check.
  fun onDeactivate() {
    if (drinking) stopDrinking()
    if (splashing) stopSplashing()
  }
  @EventHandler
  private fun onTick(event:TickEvent.Pre) {
    if (Healing.get())
    {
      if (ShouldDrinkHealth())
      {
        //Heal Pot Slot
        val slot = HealingpotionSlot()
        //Slot Not Invalid
        if (slot != -1)
        {
          startDrinking()
        }
        else if (HealingpotionSlot() == -1 && useSplashPots.get())
        {
          slot = HealingSplashpotionSlot()
          if (slot != -1)
          {
            startSplashing()
          }
        }
      }
      if (drinking)
      {
        if (ShouldDrinkHealth())
        {
          if (isNotPotion(mc.player.inventory.getStack(slot)))
          {
            slot = HealingpotionSlot()
            if (slot == -1)
            {
              ChatUtils.moduleInfo(this, "Ran out of Pots while drinking")
              stopDrinking()
              return
            }
          }
          else
          changeSlot(slot)
        }
        drink()
        if (ShouldNotDrinkHealth())
        {
          ChatUtils.moduleInfo(this, "Health Full")
          stopDrinking()
          return
        }
      }
      if (splashing)
      {
        if (ShouldDrinkHealth())
        {
          if (isNotSplashPotion(mc.player.inventory.getStack(slot)))
          {
            slot = HealingSplashpotionSlot()
            if (slot == -1)
            {
              ChatUtils.moduleInfo(this, "Ran out of Pots while splashing")
              stopSplashing()
              return
            }
            else
            changeSlot(slot)
          }
          splash()
          if (ShouldNotDrinkHealth())
          {
            ChatUtils.moduleInfo(this, "Health Full")
            stopSplashing()
            return
          }
        }
      }
    }
    if (Strength.get())
    {
      if (ShouldDrinkStrength())
      {
        //Strength Pot Slot
        val slot = StrengthpotionSlot()
        //Slot Not Invalid
        if (slot != -1)
        {
          startDrinking()
        }
        else if (StrengthpotionSlot() == -1 && useSplashPots.get())
        {
          slot = StrengthSplashpotionSlot()
          if (slot != -1)
          {
            startSplashing()
          }
        }
      }
      if (drinking)
      {
        if (ShouldDrinkStrength())
        {
          if (isNotPotion(mc.player.inventory.getStack(slot)))
          {
            slot = StrengthpotionSlot()
            if (slot == -1)
            {
              stopDrinking()
              ChatUtils.moduleInfo(this, "Out of Pots")
              return
            }
            else
            changeSlot(slot)
          }
          drink()
        }
        else
        {
          stopDrinking()
        }
      }
      if (splashing)
      {
        if (ShouldDrinkStrength())
        {
          if (isNotSplashPotion(mc.player.inventory.getStack(slot)))
          {
            slot = StrengthSplashpotionSlot()
            if (slot == -1)
            {
              ChatUtils.moduleInfo(this, "Ran out of Pots while splashing")
              stopSplashing()
              return
            }
            else
            changeSlot(slot)
          }
          splash()
        }
        else
        {
          stopSplashing()
        }
      }
    }
  }
  @EventHandler
  private fun onItemUseCrosshairTarget(event:ItemUseCrosshairTargetEvent) {
    if (drinking) event.target = null
  }
  private fun setPressed(pressed:Boolean) {
    mc.options.keyUse.setPressed(pressed)
  }
  private fun startDrinking() {
    prevSlot = mc.player.inventory.selectedSlot
    drink()
    // Pause auras
    wasAura.clear()
    if (pauseAuras.get())
    {
      for (klass in AURAS)
      {
        val module = Modules.get().get(klass)
        if (module.isActive())
        {
          wasAura.add(klass)
          module.toggle()
        }
      }
    }
    // Pause baritone
    wasBaritone = false
    if (pauseBaritone.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
    {
      wasBaritone = true
      BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause")
    }
  }
  private fun startSplashing() {
    prevSlot = mc.player.inventory.selectedSlot
    if (lookDown.get())
    {
      Rotations.rotate(mc.player.yaw, 90)
      splash()
    }
    splash()
    // Pause auras
    wasAura.clear()
    if (pauseAuras.get())
    {
      for (klass in AURAS)
      {
        val module = Modules.get().get(klass)
        if (module.isActive())
        {
          wasAura.add(klass)
          module.toggle()
        }
      }
    }
    // Pause baritone
    wasBaritone = false
    if (pauseBaritone.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
    {
      wasBaritone = true
      BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause")
    }
  }
  private fun drink() {
    changeSlot(slot)
    setPressed(true)
    if (!mc.player.isUsingItem()) Utils.rightClick()
    drinking = true
  }
  private fun splash() {
    changeSlot(slot)
    setPressed(true)
    splashing = true
  }
  private fun stopDrinking() {
    changeSlot(prevSlot)
    setPressed(false)
    drinking = false
    // Resume auras
    if (pauseAuras.get())
    {
      for (klass in AURAS)
      {
        val module = Modules.get().get(klass)
        if (wasAura.contains(klass) && !module.isActive())
        {
          module.toggle()
        }
      }
    }
    // Resume baritone
    if (pauseBaritone.get() && wasBaritone)
    {
      BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume")
    }
  }
  private fun stopSplashing() {
    changeSlot(prevSlot)
    setPressed(false)
    splashing = false
    // Resume auras
    if (pauseAuras.get())
    {
      for (klass in AURAS)
      {
        val module = Modules.get().get(klass)
        if (wasAura.contains(klass) && !module.isActive())
        {
          module.toggle()
        }
      }
    }
    // Resume baritone
    if (pauseBaritone.get() && wasBaritone)
    {
      BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume")
    }
  }
  private fun truehealth():Double {
    assert(mc.player != null)
    return mc.player.getHealth()
  }
  private fun changeSlot(slot:Int) {
    mc.player.inventory.selectedSlot = slot
    this.slot = slot
  }
  //Sunk 7 hours into these checks, if i die blame checks
  //Heal pot checks
  private fun HealingpotionSlot():Int {
    val slot = -1
    for (i in 0..8)
    {
      // Skip if item stack is empty
      val stack = mc.player.inventory.getStack(i)
      if (stack.isEmpty()) continue
      if (stack.getItem() !== Items.POTION) continue
      if (stack.getItem() === Items.POTION)
      {
        val effects = PotionUtil.getPotion(mc.player.inventory.getStack(i)).getEffects()
        if (effects.size > 0)
        {
          val effect = effects.get(0)
          if (effect.getTranslationKey().equals("effect.minecraft.instant_health"))
          {
            slot = i
            break
          }
        }
      }
    }
    return slot
  }
  private fun HealingSplashpotionSlot():Int {
    val slot = -1
    for (i in 0..8)
    {
      // Skip if item stack is empty
      val stack = mc.player.inventory.getStack(i)
      if (stack.isEmpty()) continue
      if (stack.getItem() !== Items.SPLASH_POTION) continue
      if (stack.getItem() === Items.SPLASH_POTION)
      {
        val effects = PotionUtil.getPotion(mc.player.inventory.getStack(i)).getEffects()
        if (effects.size > 0)
        {
          val effect = effects.get(0)
          if (effect.getTranslationKey().equals("effect.minecraft.instant_health"))
          {
            slot = i
            break
          }
        }
      }
    }
    return slot
  }
  //Strength Pot Checks
  private fun StrengthSplashpotionSlot():Int {
    val slot = -1
    for (i in 0..8)
    {
      // Skip if item stack is empty
      val stack = mc.player.inventory.getStack(i)
      if (stack.isEmpty()) continue
      if (stack.getItem() !== Items.SPLASH_POTION) continue
      if (stack.getItem() === Items.SPLASH_POTION)
      {
        val effects = PotionUtil.getPotion(mc.player.inventory.getStack(i)).getEffects()
        if (effects.size > 0)
        {
          val effect = effects.get(0)
          if (effect.getTranslationKey().equals("effect.minecraft.strength"))
          {
            slot = i
            break
          }
        }
      }
    }
    return slot
  }
  private fun StrengthpotionSlot():Int {
    val slot = -1
    for (i in 0..8)
    {
      // Skip if item stack is empty
      val stack = mc.player.inventory.getStack(i)
      if (stack.isEmpty()) continue
      if (stack.getItem() !== Items.POTION) continue
      if (stack.getItem() === Items.POTION)
      {
        val effects = PotionUtil.getPotion(mc.player.inventory.getStack(i)).getEffects()
        if (effects.size > 0)
        {
          val effect = effects.get(0)
          if (effect.getTranslationKey().equals("effect.minecraft.strength"))
          {
            slot = i
            break
          }
        }
      }
    }
    return slot
  }
  private fun isNotPotion(stack:ItemStack):Boolean {
    val item = stack.getItem()
    return item !== Items.POTION
  }
  private fun isNotSplashPotion(stack:ItemStack):Boolean {
    val item = stack.getItem()
    return item !== Items.SPLASH_POTION
  }
  private fun ShouldDrinkHealth():Boolean {
    if (truehealth() < health.get()) return true
    return false
  }
  private fun ShouldNotDrinkHealth():Boolean {
    if (truehealth() >= health.get()) return true
    return false
  }
  private fun ShouldDrinkStrength():Boolean {
    val effects = mc.player.getActiveStatusEffects()
    return !effects.containsKey(StatusEffects.STRENGTH)
  }
  companion object {
    private val AURAS = arrayOf<Class<*>>(KillAura::class.java, CrystalAura::class.java, AnchorAura::class.java, BedAura::class.java)
  }
}
