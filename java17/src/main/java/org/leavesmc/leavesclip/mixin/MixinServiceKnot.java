package org.leavesmc.leavesclip.mixin;

import fun.bm.morninggloryclip.Hyacinthusclip;
import org.jetbrains.annotations.NotNull;
import org.leavesmc.leavesclip.logger.SimpleLogger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.util.ReEntranceLock;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

public class MixinServiceKnot implements IMixinService, IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {
    public static ClassLoader classLoader;
    public static IMixinTransformer transformer;
    private final ReEntranceLock lock;

    public MixinServiceKnot() {
        lock = new ReEntranceLock(1);
    }

    public byte[] getClassBytes(@NotNull String name, boolean ignored) throws ClassNotFoundException, IOException {
        String resource = name.replace('.', '/') + ".class";
        try (InputStream is = classLoader.getResourceAsStream(resource)) {
            if (is == null) {
                throw new ClassNotFoundException(name);
            }
            return is.readAllBytes();
        }
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return getClassNode(name, runTransformers, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException, IOException {
        ClassReader reader = new ClassReader(getClassBytes(name, runTransformers));
        ClassNode node = new ClassNode();
        reader.accept(node, readerFlags);
        return node;
    }

    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return classLoader.loadClass(name);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, classLoader);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, Hyacinthusclip.class.getClassLoader());
    }

    @Override
    public String getName() {
        return "MorninggloryClip";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void prepare() {
    }

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return MixinEnvironment.Phase.PREINIT;
    }

    @Override
    public void offer(IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory) {
            transformer = ((IMixinTransformerFactory) internal).createTransformer();
        }
    }

    @Override
    public void init() {
    }

    @Override
    public void beginPhase() {
    }

    @Override
    public void checkEnv(Object bootSource) {
    }

    @Override
    public ReEntranceLock getReEntranceLock() {
        return lock;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return this;
    }

    @Override
    public IClassTracker getClassTracker() {
        return this;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public IFeatureValidator getFeatureValidator() {
        return null;
    }

    @Override
    public IAdviceProvider getAdviceProvider() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Collections.singletonList("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        if (UrlUtil.LOADER_CODE_SOURCE != null) {
            return new ContainerHandleURI(UrlUtil.LOADER_CODE_SOURCE.toUri());
        } else {
            throw new IllegalStateException("CodeSource is null");
        }
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return Collections.emptyList();
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return classLoader.getResourceAsStream(name);
    }

    @Override
    public void registerInvalidClass(String className) {
    }

    @Override
    public boolean isClassLoaded(String className) {
        if (classLoader instanceof ClassLoader) {
            try {
                java.lang.reflect.Method findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
                findLoadedClass.setAccessible(true);
                Object result = findLoadedClass.invoke(classLoader, className);
                return result != null;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public String getClassRestrictions(String className) {
        return "";
    }

    @Override
    public Collection<ITransformer> getTransformers() {
        return Collections.emptyList();
    }

    @Override
    public Collection<? extends ITransformer> getDelegatedTransformers() {
        return Collections.emptyList();
    }

    @Override
    public void addTransformerExclusion(String name) {
    }

    @Override
    public String getSideName() {
        return "SERVER";
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_8;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_25;
    }

    @Override
    public ILogger getLogger(String name) {
        return new SimpleLogger("Mixin");
    }
}
