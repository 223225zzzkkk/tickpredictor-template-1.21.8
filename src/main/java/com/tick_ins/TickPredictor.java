package com.tick_ins;

import com.tick_ins.packet.PlayerAction2Server;
import com.tick_ins.tick.RunnableWithFlag;
import com.tick_ins.tick.TickThread;
import com.tick_ins.util.CText;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

public class TickPredictor implements ModInitializer {
    public static final String MOD_ID = "tick-predictor";

    // 此记录器用于将文本写入控制台和日志文件。
    // 使用您的 mod id 作为记录器的名称被认为是最佳实践。
    // 这样，就可以清楚地知道哪个模组编写了信息、警告和错误。
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 一旦 Minecraft 处于模组加载就绪状态，此代码就会运行。
        // 但是，某些东西（如资源）可能仍然未初始化。
        // 谨慎行事。
        ClientCommandRegistrationCallback.
                EVENT.
                register((dispatcher, registryAccess) ->
                        dispatcher.register(ClientCommandManager.literal("ping").executes(context -> {
                            if (TickThread.isStart()) {
                                CText.onGameMessage("关闭");
                                TickThread.shotDown();
                            } else {
                                CText.onGameMessage("开启");
                                TickThread.start();
                            }

                            return 1;
                        }))
                );
        ClientCommandRegistrationCallback.
                EVENT.
                register((dispatcher, registryAccess) ->
                        dispatcher.register(ClientCommandManager.literal("test").executes(context -> {
                            TickThread.addTask(new RunnableWithFlag(() -> {
                                        CText.onGameMessage("这是flag方法%d".formatted(System.currentTimeMillis()));
                                    }, true),
                                    new RunnableWithFlag(() -> {
                                        CText.onGameMessage("这是flag方法%d".formatted(System.currentTimeMillis()));
                                        }, true),
                                    new RunnableWithFlag(() -> {
                                        CText.onGameMessage("这是无flag方法%d".formatted(System.currentTimeMillis()));
                                    }, false),
                                    new RunnableWithFlag(() -> {
                                        CText.onGameMessage("这是flag方法%d".formatted(System.currentTimeMillis()));
                                        }, true)
                            );


                            return 1;
                        }))
                );
        LOGGER.info("tick-predictor load start");
    }
}