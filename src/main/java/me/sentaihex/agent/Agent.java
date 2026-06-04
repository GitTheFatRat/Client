package me.sentaihex.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;
import java.util.jar.JarFile;

public class Agent {

    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[SentaiHex] Agent injected!");

        // 1. Ép trạng thái headless thành false thông qua Properties Override toàn cục công hiệu nhất
        System.setProperty("java.awt.headless", "false");
        try {
            Properties props = System.getProperties();
            Properties customProps = new Properties(props) {
                @Override
                public String getProperty(String key) {
                    if ("java.awt.headless".equals(key)) return "false";
                    return super.getProperty(key);
                }
                @Override
                public Object get(Object key) {
                    if ("java.awt.headless".equals(key)) return "false";
                    return super.get(key);
                }
            };
            customProps.putAll(props);
            customProps.setProperty("java.awt.headless", "false");
            System.setProperties(customProps);
            System.out.println("[SentaiHex] Da cuong che bien Headless sang FALSE qua Properties!");
        } catch (Exception e) {
            System.out.println("[SentaiHex] Khong the ghi de Properties: " + e.getMessage());
        }

        // 2. Tìm vị trí file JAR hiện tại của Agent để chuẩn bị ép vào ClassLoader
        File agentJarFile = null;
        try {
            String path = Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            agentJarFile = new File(path);
            System.out.println("[SentaiHex] Vi tri file Agent JAR: " + agentJarFile.getAbsolutePath());

            // Đưa JAR hiện tại vào System ClassLoader Search của JVM
            inst.appendToSystemClassLoaderSearch(new JarFile(agentJarFile));
            System.out.println("[SentaiHex] Da append JAR vao System ClassLoader Search!");
        } catch (Exception e) {
            System.out.println("[SentaiHex] Khong the lay vi tri code source hoac append System Search: " + e.getMessage());
        }

        final File finalJarFile = agentJarFile;

        Thread thread = new Thread(() -> {
            try {
                // Chờ game ổn định luồng hiển thị GLFW
                Thread.sleep(3000);

                // Tìm ClassLoader của Minecraft (Hỗ trợ KnotClassLoader của Fabric trên Modrinth)
                ClassLoader mcClassLoader = findMinecraftClassLoader(inst);
                if (mcClassLoader == null) {
                    System.out.println("[SentaiHex] Khong tim thay ClassLoader cua Minecraft!");
                    return;
                }

                System.out.println("[SentaiHex] Da tim thay Minecraft classloader: " + mcClassLoader.getClass().getName());

                // 3. TUYỆT CHIÊU QUAN TRỌNG NHẤT: Ép Fabric Loader chấp nhận nạp Class từ file JAR của bạn
                if (finalJarFile != null && finalJarFile.exists()) {
                    try {
                        // Thử gọi phương thức addURL hoặc addDelegateUrl của Fabric KnotClassLoader bằng Reflection
                        // Điều này phá tan rào cản Sandbox cô lập class của Fabric
                        Method addUrlMethod = null;
                        try {
                            addUrlMethod = mcClassLoader.getClass().getMethod("addURL", URL.class);
                        } catch (NoSuchMethodException e) {
                            try {
                                addUrlMethod = mcClassLoader.getClass().getDeclaredMethod("addUrl", URL.class);
                            } catch (NoSuchMethodException ex) {
                                // Thử tìm trong class cha hoặc các delegate nếu có
                                for (Method m : mcClassLoader.getClass().getDeclaredMethods()) {
                                    if (m.getName().toLowerCase().contains("addurl") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == URL.class) {
                                        addUrlMethod = m;
                                        break;
                                    }
                                }
                            }
                        }

                        if (addUrlMethod != null) {
                            addUrlMethod.setAccessible(true);
                            addUrlMethod.invoke(mcClassLoader, finalJarFile.toURI().toURL());
                            System.out.println("[SentaiHex] Da inject thanh cong URL cua file JAR vao Fabric ClassLoader!");
                        } else {
                            System.out.println("[SentaiHex] Khong tim thay method addURL trong ClassLoader, thuc hien fallback...");
                        }
                    } catch (Exception e) {
                        System.out.println("[SentaiHex] Canh bao khi phơi bày JAR vao Fabric: " + e.getMessage());
                    }
                }

                Thread.currentThread().setContextClassLoader(mcClassLoader);

                // 4. Khởi chạy core Client của bạn
                System.out.println("[SentaiHex] Dang nap lop core Client...");
                Class<?> clientClass = Class.forName("me.sentaihex.client.SentaiHex", true, mcClassLoader);
                Field instanceField = clientClass.getDeclaredField("INSTANCE");
                Object clientInstance = clientClass.getDeclaredConstructor().newInstance();
                instanceField.set(null, clientInstance);

                Method startMethod = clientClass.getMethod("start");
                startMethod.invoke(clientInstance);

            } catch (Exception e) {
                System.out.println("[SentaiHex] Loi khoi chay Client: " + e.getMessage());
                e.printStackTrace();
            }
        });

        thread.setName("SentaiHex-Initializer");
        thread.setDaemon(true);
        thread.start();
    }

    private static ClassLoader findMinecraftClassLoader(Instrumentation inst) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("Render thread".equals(t.getName())) {
                ClassLoader cl = t.getContextClassLoader();
                if (cl != null) return cl;
            }
        }

        String[] mcClasses = {
                "net.minecraft.client.MinecraftClient",
                "net.minecraft.client.Minecraft",
                "net.minecraft.class_310"
        };

        Class<?>[] allLoaded = inst.getAllLoadedClasses();
        for (Class<?> cls : allLoaded) {
            for (String target : mcClasses) {
                if (cls.getName().equals(target)) {
                    ClassLoader cl = cls.getClassLoader();
                    if (cl != null) return cl;
                }
            }
        }

        for (Class<?> cls : allLoaded) {
            if (cls.getName().startsWith("net.minecraft.")) {
                ClassLoader cl = cls.getClassLoader();
                if (cl != null) return cl;
            }
        }

        return null;
    }
}