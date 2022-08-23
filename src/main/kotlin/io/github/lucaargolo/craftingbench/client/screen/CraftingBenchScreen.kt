package io.github.lucaargolo.craftingbench.client.screen

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.craftingbench.client.CraftingBenchClient
import io.github.lucaargolo.craftingbench.common.screenhandler.CraftingBenchScreenHandler
import io.github.lucaargolo.craftingbench.utils.ModIdentifier
import it.unimi.dsi.fastutil.ints.IntList
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.item.TooltipContext
import net.minecraft.client.render.*
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeMatcher
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.random.Random
import net.minecraft.util.registry.Registry
import org.lwjgl.glfw.GLFW

class CraftingBenchScreen(handler: CraftingBenchScreenHandler, inventory: PlayerInventory, title: Text) : HandledScreen<CraftingBenchScreenHandler>(handler, inventory, title) {

    private val random = Random.create()

    private val buttonRenderFramebuffer = SimpleFramebuffer(1, 1, true, MinecraftClient.IS_SYSTEM_MAC)
    private val heightBtnReference = linkedMapOf<ButtonWidget, Int>()

    private var scrollableOffset = 0.0
    private var scrollable = false
    private var excessHeight = 0.0

    private var draggingScroll = false

    private var searchBar: TextFieldWidget? = null

    private var selectedCrafting: CraftingButtonWidget? = null

    private var internalTick = 0

    private var crafting = false
    private var craftingProgress = 0
    private var craftingPartProgress = 0

    private var recipeFinder = RecipeMatcher()

    init {
        backgroundWidth = 421
        titleX += 105
        playerInventoryTitleX += 105
    }

    override fun init() {
        super.init()

        searchBar = TextFieldWidget(textRenderer, x + 19, y + 7, 81, 10, Text.literal(""))
        searchBar?.setDrawsBackground(false)
        searchBar?.setChangedListener(::updateChildren)

        updateChildren("")
    }

    override fun resize(client: MinecraftClient?, width: Int, height: Int) {
        val string = searchBar?.text
        this.init(client, width, height)
        searchBar?.text = string
        if (searchBar?.text?.isNotEmpty() == true) {
            updateChildren(searchBar?.text ?: "")
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return when {
            searchBar?.keyPressed(keyCode, scanCode, modifiers) == true -> true
            searchBar?.isFocused == true && searchBar?.isVisible == true && keyCode != GLFW.GLFW_KEY_ESCAPE -> true
            else -> super.keyPressed(keyCode, scanCode, modifiers)
        }
    }

    private fun updateChildren(searchString: String) {
        clearChildren()
        addDrawableChild(searchBar)
        heightBtnReference.clear()

        scrollableOffset = 0.0
        excessHeight = 0.0
        scrollable = false

        handler.craftableRecipes.entries.filter { (recipe, _) ->
            val filter = searchString.lowercase()
            val itemId = Registry.ITEM.getId(recipe.output.item)
            when {
                filter == "" -> true
                itemId.path.replace("_", " ").contains(filter) -> true
                recipe.output.item.name.string.lowercase().contains(filter) -> true
                recipe.output.getTooltip(null, TooltipContext.Default.NORMAL).filter { it.string.lowercase().contains(filter) }.isNotEmpty() -> true
                filter.startsWith("@") && itemId.namespace.contains(filter.substring(1)) -> true
                else -> false
            }
        }.forEachIndexed { index, entry ->
            val btn = CraftingButtonWidget(handler, x+5, y+19+(index*20), if(scrollable) 88 else 95, 20, entry.key, entry.value.first, entry.value.second) { button ->
                val craftingButton = button as? CraftingButtonWidget ?: return@CraftingButtonWidget
                craftingButton.selected = true
                craftingButton.active = false
                selectedCrafting?.selected = false
                selectedCrafting?.active = true
                selectedCrafting = craftingButton
                craftingProgress = 0
            }
            this.addSelectableChild(btn)
            if(btn.y + btn.height > y+158) {
                if(!scrollable) {
                    scrollable = true
                    children().forEach {
                        (it as? ButtonWidget)?.width = 88
                    }
                    excessHeight += ((btn.y-y) + btn.height) - 158
                }else{
                    excessHeight += btn.height
                }
            }
            heightBtnReference[btn] = btn.y
        }
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return if(draggingScroll) {
            draggingScroll = false
            true
        }else if (crafting){
            crafting = false
            craftingPartProgress = 0
            true
        }else{
            super.mouseReleased(mouseX, mouseY, button)
        }
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return if(draggingScroll) {
            if(scrollable) {
                scrollableOffset += deltaY * (excessHeight/131)
                scrollableOffset = MathHelper.clamp(scrollableOffset, 0.0, excessHeight)
                updateButtonsHeight()
                true
            }else{
                draggingScroll = false
                false
            }
        }else{
            super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(scrollable && mouseX in (x+94.0..x+100.0) && mouseY in (y+19.0..y+158.0) && button == 0) {
            val offset = MathHelper.lerp(scrollableOffset / excessHeight, 19.0, 131.0)
            if(mouseX in (x+94.0)..(x+100.0) && mouseY in (y+offset)..(y+offset+27.0)) {
                client?.soundManager?.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F))
                draggingScroll = true
            }else{
                scrollableOffset = MathHelper.lerp((mouseY-19-y)/139, 0.0, excessHeight)
                updateButtonsHeight()
            }
            return true
        }else if(selectedCrafting != null && mouseX in (x+166.0)..(x+220.0) && mouseY in (y+38.0)..(y+65.0)) {
            client?.soundManager?.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F))
            crafting = true
            if(craftingProgress == (selectedCrafting?.recipeHistory?.size ?: 0)*2) {
                craftingProgress = 0
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if(scrollable) {
            scrollableOffset -= amount*4
            scrollableOffset = MathHelper.clamp(scrollableOffset, 0.0, excessHeight)
            updateButtonsHeight()
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun drawBackground(matrices: MatrixStack, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShaderTexture(0, TEXTURE)
        drawTexture(matrices, x, y, 0f, 0f, backgroundWidth, backgroundHeight, 512, 256)
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        textRenderer.draw(matrices, title, titleX.toFloat(), titleY.toFloat(), 0xFFFFFF)
        textRenderer.draw(matrices, playerInventoryTitle, playerInventoryTitleX.toFloat(), playerInventoryTitleY.toFloat(), 0xFFFFFF)
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        if(!scrollable) {
            DrawableHelper.fill(matrices, x+5, y+19, x+100, y+158, 0xFF8B8B8B.toInt())
            DrawableHelper.fill(matrices, x+93, y+158, x+94, y+159, 0xFFFFFFFF.toInt())
        }
        if(buttonRenderFramebuffer.textureWidth != client?.framebuffer?.textureWidth || buttonRenderFramebuffer.textureHeight != client?.framebuffer?.textureHeight) {
            buttonRenderFramebuffer.resize(client?.framebuffer?.textureWidth ?: 1, client?.framebuffer?.textureHeight ?: 1, MinecraftClient.IS_SYSTEM_MAC)
        }
        buttonRenderFramebuffer.beginWrite(true)
        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 0.0f)
        RenderSystem.clear(16384, MinecraftClient.IS_SYSTEM_MAC)
        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC)
        zOffset = 100
        itemRenderer.zOffset = 100.0f
        children().forEach {
            (it as? CraftingButtonWidget)?.let { btn ->
                if((y..y+158).contains(btn.y)) {
                    if(!btn.selected && !btn.notEnoughItems) {
                        btn.active = true
                    }
                    btn.render(matrices, mouseX, mouseY, delta)
                }else{
                    btn.active = false
                }
            }
        }
        itemRenderer.zOffset = 0f
        zOffset = 0
        buttonRenderFramebuffer.endWrite()
        client?.framebuffer?.beginWrite(true)
        buttonRenderFramebuffer.beginRead()
        RenderSystem.setShaderTexture(0, buttonRenderFramebuffer.colorAttachment)
        val sf = client?.window?.scaleFactor?.toFloat() ?: 1f
        matrices.push()
        matrices.scale(1/sf, 1/sf, 1/sf)

        val matrix = matrices.peek().positionMatrix
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        val bufferBuilder = Tessellator.getInstance().buffer

        val x0 = (x+5f)*sf
        val x1 = ((x+5f)*sf)+(95*sf)
        val y0 = (y+19f)*sf
        val y1 = ((y+19f)*sf)+(139*sf)

        val u0 = ((x+5f)*sf) / buttonRenderFramebuffer.textureWidth
        val u1 = (((x+5f)*sf)+(95*sf)) / buttonRenderFramebuffer.textureWidth
        val v0 = ((y+19f-10.5f)*sf) / buttonRenderFramebuffer.textureHeight
        val v1 = (((y+19f-10.5f)*sf)+(139*sf)) / buttonRenderFramebuffer.textureHeight

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE)
        bufferBuilder.vertex(matrix, x0, y1, 0f).texture(u0, v0).next()
        bufferBuilder.vertex(matrix, x1, y1, 0f).texture(u1, v0).next()
        bufferBuilder.vertex(matrix, x1, y0, 0f).texture(u1, v1).next()
        bufferBuilder.vertex(matrix, x0, y0, 0f).texture(u0, v1).next()
        BufferRenderer.drawWithShader(bufferBuilder.end())

        matrices.pop()

        buttonRenderFramebuffer.endRead()
        RenderSystem.setShaderTexture(0, TEXTURE)
        if(scrollable) {
            val offset = MathHelper.lerp(scrollableOffset / excessHeight, 19.0, 131.0)
            drawTexture(matrices, x + 94, y + offset.toInt(), 0f, 166f, 6, 27, 512, 256)
            if(mouseX in (x+94)..(x+100) && mouseY in (y+offset.toInt())..(y+offset.toInt()+27)) {
                DrawableHelper.fill(matrices, x + 94, y + offset.toInt(), x + 100, y + offset.toInt() + 27, -2130706433)
            }
        }
        if(selectedCrafting != null) {
            if(mouseX in (x+166)..(x+220) && mouseY in (y+38)..(y+65)) {
                drawTexture(matrices, x + 166, y + 38, 421f, 54f, 54, 27, 512, 256)
            }else{
                drawTexture(matrices, x + 166, y + 38, 421f, 27f, 54, 27, 512, 256)
            }
        }else{
            drawTexture(matrices, x + 166, y + 38, 421f, 0f, 54, 27, 512, 256)
        }
        val partProgress = MathHelper.lerp(craftingPartProgress/10f, 0f, 52f)
        drawTexture(matrices, x + 167, y + 26, 106f, 184f, MathHelper.floor(partProgress), 5, 512, 256)
        val progress = MathHelper.lerp(craftingProgress/((selectedCrafting?.recipeHistory?.size ?: 0)*2f), 0f, 124f)
        drawTexture(matrices, x + 131, y + 17, 106f, 176f, MathHelper.floor(progress), 7, 512, 256)
    }

    override fun handledScreenTick() {
        super.handledScreenTick()
        searchBar?.tick()
        children().forEach {
            (it as? CraftingButtonWidget)?.let { btn ->
                if((y..y+158).contains(btn.y)) {
                    btn.tick()
                }
            }
        }
        if(selectedCrafting?.notEnoughItems == true && !crafting && craftingProgress == 0) {
            selectedCrafting?.selected = false
            selectedCrafting = null
        }
        if(crafting) {
            val selectedCrafting = selectedCrafting ?: return
            val recipeHistory = selectedCrafting.recipeHistory
            if(craftingPartProgress++ == 10) {
                craftingPartProgress = 0
            }
            if(craftingPartProgress == 0) {
                if(handler.slots[0].hasStack() && !handler.slots[0].stack.isEmpty) {
                    client?.interactionManager?.clickSlot(handler.syncId, 0, 0, SlotActionType.QUICK_MOVE, client?.player)
                    client?.soundManager?.play(PositionedSoundInstance.master(SoundEvents.BLOCK_SMITHING_TABLE_USE, random.nextFloat() * 0.1f + 0.9f))
                }else{
                    val nextRecipe = recipeHistory[craftingProgress/2]
                    client?.networkHandler?.sendPacket(CraftRequestC2SPacket(handler.syncId, nextRecipe, false))
                    client?.soundManager?.play(PositionedSoundInstance.master(SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, 1.0f))
                }
                if(++craftingProgress == recipeHistory.size*2) {
                    crafting = false
                    client?.player?.let(handler::populateRecipes)
                    if(selectedCrafting.notEnoughItems) {
                        selectedCrafting.selected = false
                        this.selectedCrafting = null
                    }
                }
            }
        }
        internalTick++
    }

    private fun updateButtonsHeight() {
        children().forEach {
            (it as? ButtonWidget)?.y = (heightBtnReference[it] ?: 0) - scrollableOffset.toInt()
        }
    }

    inner class CraftingButtonWidget(private val handler: CraftingBenchScreenHandler, x: Int, y: Int, width: Int, height: Int, val recipe: Recipe<*>, val recipeHistory: List<Recipe<*>>, val requiredItems: List<IntList>, onPressAction: PressAction) : ButtonWidget(x, y, width, height, Text.literal(""), onPressAction) {

        private val client = MinecraftClient.getInstance()

        private val requiredItemsForRender = mutableListOf<ItemStack>()

        var selected = false
        var notEnoughItems = false

        fun tick() {
            requiredItemsForRender.clear()
            val fakeInventory = SimpleInventory(handler.combinedInventory.size())
            var hasRequiredItems = true
            requiredItems.forEach { ingredient ->
                var foundItem = ingredient.isEmpty()
                ingredient.forEach testIngredient@{ itemId ->
                    val item = Registry.ITEM.get(itemId)
                    if(handler.combinedInventory.fakeRemoveItem(item, 1).count == 1) {
                        fakeInventory.addStack(ItemStack(item, 1))
                        foundItem = true
                        return@testIngredient
                    }
                }
                if(!foundItem) {
                    hasRequiredItems = false
                }
            }
            requiredItemsForRender.addAll(fakeInventory.clearToList())
            if(!hasRequiredItems) {
                notEnoughItems = true
                active = false
            }else{
                notEnoughItems = false
                if(!selected) {
                    active = true
                }
            }
        }

        override fun getYImage(hovered: Boolean): Int {
            return if(selected) 2 else super.getYImage(hovered)
        }

        override fun renderButton(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
            super.renderButton(matrices, mouseX, mouseY, delta)
            val excessOffset = if(requiredItemsForRender.size / 3 > 0) {
                val excess = MathHelper.floor((requiredItemsForRender.size / 3.0) - 0.01) + 1
                (internalTick % (excess*30))/30
            } else 0
            requiredItemsForRender.subList(excessOffset*3, ((excessOffset+1)*3).coerceAtMost(requiredItemsForRender.size)).forEachIndexed { index, stack ->
                client.itemRenderer.renderInGuiWithOverrides(client.player, stack, x+2+(index*18), y+2, 0)
                client.itemRenderer.renderGuiItemOverlay(client.textRenderer, stack, x+2+(index*18), y+2, if(stack.count != 1) stack.count.toString() else "")
            }
            RenderSystem.setShaderTexture(0, TEXTURE)
            drawTexture(matrices, x+(width-18-4-16)+1, y+4, 6f, 166f, 16, 12, 512, 256)
            client.itemRenderer.renderInGuiWithOverrides(client.player, recipe.output, x+(width-17-2), y+2, 0)
            client.itemRenderer.renderGuiItemOverlay(client.textRenderer, recipe.output, x+(width-17-2), y+2, if(recipe.output.count != 1) recipe.output.count.toString() else "")
        }
        private fun Inventory.fakeRemoveItem(item: Item, count: Int): ItemStack {
            val itemStack = ItemStack(item, 0)
            for (i in this.size() - 1 downTo 0) {
                val itemStack2: ItemStack = this.getStack(i).copy()
                if (itemStack2.item == item) {
                    val j = count - itemStack.count
                    val itemStack3 = itemStack2.split(j)
                    itemStack.increment(itemStack3.count)
                    if (itemStack.count == count) {
                        break
                    }
                }
            }
            if (!itemStack.isEmpty) {
                this.markDirty()
            }
            return itemStack
        }

    }


    companion object {
        private val TEXTURE = ModIdentifier("textures/gui/crafting_bench.png")
    }

}