package com.tick_ins;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;

public class TickPredictor implements ModInitializer {
    public static final String MOD_ID = "tick-predictor";

    // 此记录器用于将文本写入控制台和日志文件。
    // 使用您的 mod id 作为记录器的名称被认为是最佳实践。
    // 这样，就可以清楚地知道哪个模组编写了信息、警告和错误。
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static BufferedWriter finalWriter;
    @Override
    public void onInitialize() {
        // 一旦 Minecraft 处于模组加载就绪状态，此代码就会运行。
        // 但是，某些东西（如资源）可能仍然未初始化。
        // 谨慎行事。
//        ClientCommandRegistrationCallback.
//                EVENT.
//                register((dispatcher, registryAccess) ->
//                        dispatcher.register(ClientCommandManager.literal("ping").executes(context -> {
//                            if (TickThread.isStart()) {
//                                CText.onGameMessage("关闭");
//                                TickThread.shotDown();
//                            } else {
//                                CText.onGameMessage("开启");
//                                TickThread.start();
//                            }
//
//                            return 1;
//                        }))
//                );
//        ClientCommandRegistrationCallback.
//                EVENT.
//                register((dispatcher, registryAccess) ->
//                                dispatcher.register(ClientCommandManager.literal("test").executes(context -> {
//                                    final String FILE_PATH = "output2.txt";
//// 在外部初始化 writer，确保生命周期覆盖所有任务
//                                    BufferedWriter writer = null;
//                                    try {
//                                        writer = new BufferedWriter(new FileWriter(FILE_PATH, true), 32768);
//                                    } catch (IOException e) {
//                                        throw new RuntimeException(e);
//                                    }
//                                     finalWriter = writer;
//                                    for (int i = 0; i < 10; i++) {
//                                        TickThread.addTask(new RunnableWithFlag(() -> {
//                                                    try {
//                                                        Ping2Server.send(111111);
//                                                        finalWriter.write("这是flag方法%d\n".formatted(System.currentTimeMillis()));
//                                                        finalWriter.flush();  // 每次写入后强制刷新到磁盘
//
//                                                    } catch (IOException e) {
//                                                        throw new RuntimeException(e);
//                                                    }
//                                                }, false),
//                                                new RunnableWithFlag(() -> {
//                                                    try {
//                                                        Ping2Server.send(111111);
//                                                        finalWriter.write("这是flag方法%d\n".formatted(System.currentTimeMillis()));
//                                                        finalWriter.flush();  // 每次写入后强制刷新到磁盘
//
//                                                    } catch (IOException e) {
//                                                        throw new RuntimeException(e);
//                                                    }
//                                                }, false),
//                                                new RunnableWithFlag(() -> {
//                                                    try {
//                                                        Ping2Server.send(111111);
//                                                        finalWriter.write("这是flag方法%d\n".formatted(System.currentTimeMillis()));
//                                                        finalWriter.flush();  // 每次写入后强制刷新到磁盘
//
//                                                    } catch (IOException e) {
//                                                        throw new RuntimeException(e);
//                                                    }
//                                                }, false),
//                                                new RunnableWithFlag(() -> {
//                                                    try {
//                                                        Ping2Server.send(111111);
//                                                        finalWriter.write("这是flag方法%d\n".formatted(System.currentTimeMillis()));
//                                                        finalWriter.flush();  // 每次写入后强制刷新到磁盘
//                                                    } catch (IOException e) {
//                                                        throw new RuntimeException(e);
//                                                    }
//                                                }, false)
//                                        );
//                                    }
//
//                                    return 1;
//                                }))
//                );
        LOGGER.info("tick-predictor load start");
    }
}